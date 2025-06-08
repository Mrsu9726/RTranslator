package nie.translator.rtranslator.tools.usb

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader

object RootFileUtils {

    fun copyFileWithRoot(sourcePath: String, destinationPath: String, callback: FileCopyCallback) {
        var process: Process? = null
        var os: DataOutputStream? = null
        var reader: BufferedReader? = null
        try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)
            // 执行 cp 命令复制文件
            os.writeBytes("cp $sourcePath $destinationPath\n")
            os.writeBytes("exit\n")
            os.flush()

            // 读取命令执行结果
            reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // 这里简单示例，实际需要根据 cp 输出解析进度
                // 目前仅假设 cp 命令不输出进度，可按需扩展
            }

            // 等待命令执行完成
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                callback.onSuccess()
            } else {
                callback.onFailure("复制文件失败，退出码: $exitCode")
            }
        } catch (e: IOException) {
            callback.onFailure("IO 异常: ${e.message}")
        } catch (e: InterruptedException) {
            callback.onFailure("线程中断异常: ${e.message}")
        } finally {
            try {
                os?.close()
                reader?.close()
                process?.destroy()
            } catch (e: IOException) {
                callback.onFailure("关闭资源异常: ${e.message}")
            }
        }
    }
}
