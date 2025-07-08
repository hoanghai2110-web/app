
package com.sosgm.chat.llama

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LlamaCppWrapper(private val context: Context) {
    
    companion object {
        init {
            try {
                System.loadLibrary("llama-android")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("LlamaCpp", "Failed to load native library: ${e.message}")
            }
        }
    }
    
    private external fun initModel(modelPath: String): Long
    private external fun generateText(
        contextPtr: Long, 
        prompt: String, 
        maxTokens: Int, 
        temperature: Float
    ): String
    private external fun freeModel(contextPtr: Long)
    
    private var modelContext: Long = 0
    
    suspend fun loadModel(modelPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("LlamaCpp", "Loading GGUF model from: $modelPath")
            if (!File(modelPath).exists()) {
                Log.e("LlamaCpp", "Model file does not exist: $modelPath")
                return@withContext false
            }
            modelContext = initModel(modelPath)
            Log.d("LlamaCpp", "Model loaded successfully, context: $modelContext")
            modelContext != 0L
        } catch (e: Exception) {
            Log.e("LlamaCpp", "Failed to load model: ${e.message}")
            false
        }
    }
    
    fun isModelLoaded(): Boolean = modelContext != 0L
    
    suspend fun loadGGUFModel(modelFile: String): Boolean = withContext(Dispatchers.IO) {
        val modelDir = File(context.getExternalFilesDir(null), "models")
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
        val modelPath = File(modelDir, modelFile).absolutePath
        return@withContext loadModel(modelPath)
    }
    
    suspend fun generate(
        prompt: String,
        maxTokens: Int = 512,
        temperature: Float = 0.7f,
        onToken: (String) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        try {
            generateText(modelContext, prompt, maxTokens, temperature)
        } catch (e: Exception) {
            Log.e("LlamaCpp", "Generation failed: ${e.message}")
            ""
        }
    }
    
    fun cleanup() {
        if (modelContext != 0L) {
            freeModel(modelContext)
            modelContext = 0L
        }
    }
}
