package nie.translator.rtranslator.tools.usb

interface FileCopyCallback {
    fun onSuccess()
    fun onFailure(errorMessage: String)
    fun onProgress(progress: Int)
}