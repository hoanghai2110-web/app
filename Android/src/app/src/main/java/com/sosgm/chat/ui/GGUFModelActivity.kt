
package com.sosgm.chat.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.sosgm.chat.llama.LlamaCppWrapper
import com.sosgm.chat.ui.theme.SOSGMTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class GGUFModelActivity : ComponentActivity() {
    
    data class GGUFModel(
        val name: String,
        val url: String,
        val fileName: String,
        val description: String
    )
    
    private val availableModels = listOf(
        GGUFModel(
            name = "Gemma 3n E2B IT",
            url = "https://huggingface.co/unsloth/gemma-3n-E2B-it-GGUF/resolve/main/gemma-3n-e2b-it-Q4_K_M.gguf",
            fileName = "gemma-3n-e2b-it-Q4_K_M.gguf",
            description = "Gemma 3n E2B IT model optimized for instruction following"
        )
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            SOSGMTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GGUFModelScreen()
                }
            }
        }
    }
    
    @Composable
    fun GGUFModelScreen() {
        val context = LocalContext.current
        val llamaWrapper = remember { LlamaCppWrapper(context) }
        var downloadProgress by remember { mutableStateOf(0f) }
        var isDownloading by remember { mutableStateOf(false) }
        var downloadStatus by remember { mutableStateOf("") }
        var isModelLoaded by remember { mutableStateOf(false) }
        var generatedText by remember { mutableStateOf("") }
        var inputPrompt by remember { mutableStateOf("Hello! How are you?") }
        
        LaunchedEffect(Unit) {
            isModelLoaded = llamaWrapper.isModelLoaded()
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "GGUF Models",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(availableModels) { model ->
                    ModelCard(
                        model = model,
                        downloadProgress = downloadProgress,
                        isDownloading = isDownloading,
                        downloadStatus = downloadStatus,
                        isModelLoaded = isModelLoaded,
                        onDownload = { selectedModel ->
                            lifecycleScope.launch {
                                downloadModel(
                                    model = selectedModel,
                                    llamaWrapper = llamaWrapper,
                                    onProgress = { progress ->
                                        downloadProgress = progress
                                    },
                                    onComplete = { success ->
                                        isDownloading = false
                                        isModelLoaded = success
                                        downloadStatus = if (success) "Model loaded successfully!" else "Failed to load model"
                                    },
                                    onStart = {
                                        isDownloading = true
                                        downloadStatus = "Downloading..."
                                    }
                                )
                            }
                        }
                    )
                }
                
                item {
                    if (isModelLoaded) {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Test the Model",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                OutlinedTextField(
                                    value = inputPrompt,
                                    onValueChange = { inputPrompt = it },
                                    label = { Text("Enter your prompt") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Button(
                                    onClick = {
                                        lifecycleScope.launch {
                                            generatedText = "Generating..."
                                            val result = llamaWrapper.generate(inputPrompt)
                                            generatedText = result
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Generate Text")
                                }
                                
                                if (generatedText.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = generatedText,
                                            modifier = Modifier.padding(16.dp),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    fun ModelCard(
        model: GGUFModel,
        downloadProgress: Float,
        isDownloading: Boolean,
        downloadStatus: String,
        isModelLoaded: Boolean,
        onDownload: (GGUFModel) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = model.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = model.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                if (isDownloading) {
                    LinearProgressIndicator(
                        progress = downloadProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    Text(
                        text = "Downloading... ${(downloadProgress * 100).toInt()}%",
                        fontSize = 12.sp
                    )
                } else {
                    Button(
                        onClick = { onDownload(model) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        enabled = !isModelLoaded
                    ) {
                        Text(if (isModelLoaded) "Model Loaded" else "Download & Load")
                    }
                }
                
                if (downloadStatus.isNotEmpty() && !isDownloading) {
                    Text(
                        text = downloadStatus,
                        fontSize = 12.sp,
                        color = if (isModelLoaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
    
    private suspend fun downloadModel(
        model: GGUFModel,
        llamaWrapper: LlamaCppWrapper,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean) -> Unit,
        onStart: () -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            onStart()
            
            val modelDir = File(getExternalFilesDir(null), "models")
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }
            
            val modelFile = File(modelDir, model.fileName)
            
            // Skip download if file already exists
            if (modelFile.exists()) {
                onProgress(1f)
                val success = llamaWrapper.loadModel(modelFile.absolutePath)
                onComplete(success)
                return@withContext
            }
            
            // Download using OkHttp
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(model.url)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    onComplete(false)
                    return@withContext
                }
                
                val totalBytes = response.body?.contentLength() ?: -1L
                var downloadedBytes = 0L
                
                response.body?.byteStream()?.use { inputStream ->
                    FileOutputStream(modelFile).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            
                            if (totalBytes > 0) {
                                val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                                withContext(Dispatchers.Main) {
                                    onProgress(progress)
                                }
                            }
                        }
                    }
                }
            }
            
            // Load the downloaded model
            val success = llamaWrapper.loadModel(modelFile.absolutePath)
            withContext(Dispatchers.Main) {
                onComplete(success)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onComplete(false)
            }
        }
    }
}
