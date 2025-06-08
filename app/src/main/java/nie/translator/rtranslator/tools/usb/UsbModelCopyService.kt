package com.translate.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Environment
import android.util.Log
import com.blankj.utilcode.util.LogUtils
import me.jahnen.libaums.core.UsbMassStorageDevice
import me.jahnen.libaums.core.fs.UsbFile
import me.jahnen.libaums.core.fs.UsbFileInputStream
import nie.translator.rtranslator.Global
import nie.translator.rtranslator.tools.usb.FileCopyCallback
import nie.translator.rtranslator.tools.usb.RootFileUtils
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object UsbModelCopyService {

    private const val TAG = "UsbModelCopyService"
    private const val MODEL_ROOT_FOLDER_NAME = "syntalk"
    private const val MODEL_FOLDER_NAME = "models"
    private const val APK_FOLDER_NAME = "apk"
    private var TARGET_DIR = "syntalk/models"//后面需要重写
    var TARGET_APK_DIR = "syntalk/apk"//后面需要重写
    var APK_NAME = "syntalk.apk"//后面需要重写
    private var context: WeakReference<Context>? = null
    private var callback: ModelCopyCallback? = null
    private var apkCallback: ApkCopyCallback? = null
    private val cancelFlag = AtomicBoolean(false)
    private var isUsbInserted = false
    private var global: Global? = null
    private var usbPath: String = ""

    // 定义线程池，线程数量根据 CPU 核心数动态调整
    private val executorService: ExecutorService =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2)

    fun registerUsbReceiver(context: Context, cb: ModelCopyCallback) {
        callback = cb
        this.context = WeakReference(context)
        global = context.applicationContext as Global
        TARGET_DIR = context.getFilesDir().absolutePath
        val filter = IntentFilter(Intent.ACTION_MEDIA_MOUNTED)
        filter.addDataScheme("file")
        context.registerReceiver(usbReceiver, filter)
        LogUtils.i("U盘插入监听已注册")
    }

    fun registerApkReceiver(context: Context, apkCal: ApkCopyCallback) {
        apkCallback = apkCal
        this.context = WeakReference(context)
        TARGET_DIR = context.getFilesDir().absolutePath
        val filter = IntentFilter(Intent.ACTION_MEDIA_MOUNTED)
        filter.addDataScheme("file")
        context.registerReceiver(usbReceiver, filter)
        LogUtils.i("U盘插入监听已注册")
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val path = intent.data?.path
            LogUtils.i("检测到U盘插入路径: $path")
            if (path != null) {
                usbPath = path
                isUsbInserted = true
                scanAndCopyFromUsb()
            } else {
                isUsbInserted = false
                usbPath = ""
            }
        }
    }

    fun cancel() {
        cancelFlag.set(true)
    }

    private fun scanAndCopyFromUsb() {
        val ctx = context?.get() ?: run {
            callback?.onFailure("Context 为空")
            return
        }

        val devices = UsbMassStorageDevice.getMassStorageDevices(ctx)
        for (device in devices) {
            try {
                device.init()
                for (partition in device.partitions) {
                    val root = partition.fileSystem.rootDirectory

                    val syntalkDir = root.listFiles()
                        .find { it.isDirectory && it.name == MODEL_ROOT_FOLDER_NAME }
                    val modelsDir = syntalkDir?.listFiles()
                        ?.find { it.isDirectory && it.name == MODEL_FOLDER_NAME }
                    val apkDir = syntalkDir?.listFiles()
                        ?.find { it.isDirectory && it.name == APK_FOLDER_NAME }
                    if (modelsDir != null) {
                        LogUtils.i("找到模型目录: ${modelsDir.name}")
                        Thread {
                            copyModelsFromUsb(modelsDir)
                        }.start()
                    }
                    if (apkDir != null) {
                        LogUtils.i("找到APK目录: ${apkDir.name}")
                        val listFiles = apkDir.listFiles()
                        var apkExit = false
                        listFiles.forEach { file ->
                            if (file.name.startsWith(APK_NAME) && file.name.endsWith(".apk")) {
                                LogUtils.i("找到APK文件: ${file.name}")
                                apkExit = true
                                return@forEach
                            }
                        }
                        if (apkExit) {
                            Thread {
                                copyApkFromUsb(apkDir)
                            }.start()
                        }
                    }
                }
            } catch (e: Exception) {
                LogUtils.e(e, "U盘初始化失败: ${device.usbDevice.deviceName}")
            }
        }

        callback?.onFailure("未检测到包含模型的 U 盘路径")
    }

    private fun copyModelsFromUsb(usbModelsDir: UsbFile) {
        try {
            val allFiles = collectUsbFilesRecursively(usbModelsDir)
            val total = allFiles.size
            callback?.onStart(total)

            var current = 0
            var lastReportTime = System.nanoTime()
            var bytesCopiedSinceLast = 0L
            val targetDir = File(Environment.getExternalStorageDirectory(), TARGET_DIR)

            val futures = allFiles.map { usbFile ->
                executorService.submit {
                    val relativePath = getRelativePath(usbModelsDir, usbFile)
                    val destFile = File(targetDir, relativePath)
                    destFile.parentFile?.mkdirs()

                    val bytesCopied = copyUsbFile(usbFile, destFile)
                    synchronized(this) {
                        current++
                        bytesCopiedSinceLast += bytesCopied
                        val now = System.nanoTime()
                        val durationSec = (now - lastReportTime) / 1e9
                        var speed = 0.0
                        if (durationSec >= 0.1) {
                            speed = bytesCopiedSinceLast / 1024.0 / 1024.0 / durationSec
                            bytesCopiedSinceLast = 0
                            lastReportTime = now
                        }
                        callback?.onProgress(current, total, usbFile.name, speed)
                    }
                }
            }

            futures.forEach { it.get() }

            if (cancelFlag.get()) return
            callback?.onSuccess()

        } catch (e: Exception) {
            Log.e(TAG, "模型复制失败: ${e.message}", e)
            callback?.onFailure("模型复制失败: ${e.message}")
        } finally {
//            executorService.shutdown()
        }
    }

    private fun copyApkFromUsb(apkDir: UsbFile) {
        try {
            val allFiles = collectUsbFilesRecursively(apkDir)
            val total = allFiles.size
            apkCallback?.onStart(total)

            var current = 0
            var lastReportTime = System.nanoTime()
            var bytesCopiedSinceLast = 0L
            val targetDir = File(Environment.getExternalStorageDirectory(), TARGET_APK_DIR)

            val futures = allFiles.map { usbFile ->
                executorService.submit {
                    val relativePath = getRelativePath(apkDir, usbFile)
                    val destFile = File(targetDir, relativePath)
                    destFile.parentFile?.mkdirs()

                    val bytesCopied = copyUsbFile(usbFile, destFile)
                    synchronized(this) {
                        current++
                        bytesCopiedSinceLast += bytesCopied
                        val now = System.nanoTime()
                        val durationSec = (now - lastReportTime) / 1e9
                        var speed = 0.0
                        if (durationSec >= 0.1) {
                            speed = bytesCopiedSinceLast / 1024.0 / 1024.0 / durationSec
                            bytesCopiedSinceLast = 0
                            lastReportTime = now
                        }
                        apkCallback?.onProgress(current, total, usbFile.name, speed)
                    }
                }
            }

            futures.forEach { it.get() }

            if (cancelFlag.get()) return
            apkCallback?.onSuccess()

        } catch (e: Exception) {
            Log.e(TAG, "模型复制失败: ${e.message}", e)
            apkCallback?.onFailure("模型复制失败: ${e.message}")
        } finally {
//            executorService.shutdown()
        }
    }

    private fun getRelativePath(root: UsbFile, file: UsbFile): String {
        var current: UsbFile? = file
        val pathSegments = mutableListOf<String>()
        while (current != null && current != root) {
            pathSegments.add(0, current.name)
            current = current.parent
        }
        return pathSegments.joinToString(File.separator)
    }

    private fun collectUsbFilesRecursively(dir: UsbFile): List<UsbFile> {
        val result = mutableListOf<UsbFile>()
        if (!dir.isDirectory) return listOf(dir)
        dir.listFiles()?.forEach {
            result.addAll(collectUsbFilesRecursively(it))
        }
        return result
    }

    private fun copyUsbFilesRecursively(
        source: UsbFile,
        destination: File,
        onFileCopied: (fileName: String, bytesCopied: Long) -> Unit
    ) {
        if (cancelFlag.get()) return

        if (source.isDirectory) {
            if (!destination.exists()) destination.mkdirs()
            source.listFiles()?.forEach { child ->
                copyUsbFilesRecursively(child, File(destination, child.name), onFileCopied)
            }
        } else {
            val bytesCopied = copyUsbFile(source, destination)
            onFileCopied(source.name, bytesCopied)
        }
    }

    // 修改copyUsbFile方法
    private fun copyUsbFile(usbFile: UsbFile, dest: File): Long {
        var totalBytes = 0L
        val bufferSize = 64 * 1024
        UsbFileInputStream(usbFile).use { input ->
            FileOutputStream(dest).channel.use { outputChannel ->
                val buffer = ByteBuffer.allocateDirect(bufferSize)
                while (input.read(buffer.array()) > 0) {
                    if (cancelFlag.get()) return totalBytes
                    buffer.flip()
                    totalBytes += outputChannel.write(buffer)
                    buffer.clear()
                }
            }
        }
        return totalBytes
    }

    fun isUsbConnected(): Boolean {
        return isUsbInserted
    }

    fun unregisterUsbReceiver(context: Context) {
        context.unregisterReceiver(usbReceiver)
        LogUtils.i("U盘插入监听已注销")
    }
}
