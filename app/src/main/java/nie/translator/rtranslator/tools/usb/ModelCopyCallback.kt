package com.translate.download

interface ModelCopyCallback {
    fun onStart(totalFiles: Int)
    fun onProgress(current: Int, total: Int, filename: String, speedMbps: Double)
    fun onSuccess()
    fun onFailure(error: String)
    fun onCancelled()
}
