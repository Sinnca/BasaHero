package com.basahero.elearning.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vosk.Model
import java.io.File

object VoskManager {
    var model: Model? = null
        private set

    private var isUnpacking = false

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    fun initModel(context: Context) {
        if (model != null || isUnpacking) return
        isUnpacking = true

        Log.d("VoskManager", "Unpacking Vosk model from assets manually...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val destDir = File(context.filesDir, "vosk_model")
                val markerFile = File(destDir, ".copied")

                if (!markerFile.exists()) {
                    Log.d("VoskManager", "Model not found on disk, copying from assets...")
                    destDir.deleteRecursively()
                    destDir.mkdirs()
                    copyAssets(context, "model", destDir)
                    markerFile.createNewFile()
                } else {
                    Log.d("VoskManager", "Model already copied to disk. Loading instantly...")
                }

                val loadedModel = Model(destDir.absolutePath)
                
                withContext(Dispatchers.Main) {
                    model = loadedModel
                    isUnpacking = false
                    _isReady.value = true
                    Log.d("VoskManager", "✓ Vosk model loaded successfully")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isUnpacking = false
                    _isError.value = true
                    Log.e("VoskManager", "✗ Failed to load Vosk model", e)
                }
            }
        }
    }

    private fun copyAssets(context: Context, path: String, outPath: File) {
        val assets = context.assets
        val files = assets.list(path) ?: emptyArray()
        if (files.isEmpty()) {
            context.assets.open(path).use { inStream ->
                outPath.outputStream().use { outStream ->
                    inStream.copyTo(outStream)
                }
            }
        } else {
            outPath.mkdirs()
            for (file in files) {
                val childOut = File(outPath, file)
                copyAssets(context, "$path/$file", childOut)
            }
        }
    }
}
