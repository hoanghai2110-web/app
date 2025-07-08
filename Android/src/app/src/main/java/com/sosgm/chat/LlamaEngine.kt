
package com.sosgm.chat

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LlamaEngine private constructor() {
    companion object {
        private const val TAG = "LlamaEngine"
        
        init {
            try {
                System.loadLibrary("llama-android")
                Log.d(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library", e)
            }
        }
        
        @Volatile
        private var INSTANCE: LlamaEngine? = null
        
        fun getInstance(): LlamaEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LlamaEngine().also { INSTANCE = it }
            }
        }
    }
    
    private var handle: Long = 0
    private var isInitialized = false
    
    // Native methods
    private external fun initLlama(modelPath: String): Long
    private external fun generateText(handle: Long, prompt: String): String
    private external fun freeLlama(handle: Long)
    private external fun getSystemInfo(): String
    
    fun initialize(context: Context, modelFileName: String): Boolean {
        return try {
            val modelFile = File(context.filesDir, modelFileName)
            if (!modelFile.exists()) {
                Log.e(TAG, "Model file not found: ${modelFile.absolutePath}")
                return false
            }
            
            handle = initLlama(modelFile.absolutePath)
            isInitialized = handle != 0L
            
            if (isInitialized) {
                Log.d(TAG, "Llama initialized successfully")
                Log.d(TAG, "System info: ${getSystemInfo()}")
            } else {
                Log.e(TAG, "Failed to initialize Llama")
            }
            
            isInitialized
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Llama", e)
            false
        }
    }
    
    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            Log.e(TAG, "Llama not initialized")
            return@withContext ""
        }
        
        try {
            val result = generateText(handle, prompt)
            Log.d(TAG, "Generated text: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error generating text", e)
            ""
        }
    }
    
    fun cleanup() {
        if (isInitialized && handle != 0L) {
            freeLlama(handle)
            handle = 0
            isInitialized = false
            Log.d(TAG, "Llama cleaned up")
        }
    }
}
