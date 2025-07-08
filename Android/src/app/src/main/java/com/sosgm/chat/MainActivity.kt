package com.sosgm.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.sosgm.chat.llama.LlamaCppWrapper
import com.sosgm.chat.ui.theme.SOSGMTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val llamaCppWrapper = LlamaCppWrapper()
    private var llamaContext: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SOSGMTheme {
                ChatScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ChatScreen() {
        var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
        var inputText by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var modelStatus by remember { mutableStateOf("No model loaded") }

        LaunchedEffect(Unit) {
            checkAndLoadModel { status ->
                modelStatus = status
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = "GGUF Chat",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = modelStatus,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Messages
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageCard(message)
                }
            }

            // Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Type a message...") },
                    enabled = !isLoading && llamaContext != 0L
                )

                Button(
                    onClick = {
                        if (inputText.isNotBlank() && llamaContext != 0L) {
                            val userMessage = ChatMessage(inputText, true)
                            messages = messages + userMessage

                            isLoading = true
                            lifecycleScope.launch {
                                val response = generateResponse(inputText)
                                val aiMessage = ChatMessage(response, false)
                                messages = messages + aiMessage
                                isLoading = false
                            }
                            inputText = ""
                        }
                    },
                    enabled = !isLoading && llamaContext != 0L && inputText.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send")
                    }
                }
            }
        }
    }

    @Composable
    fun MessageCard(message: ChatMessage) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = if (message.isUser) "You" else "AI",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (message.isUser) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    color = if (message.isUser) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }

    private suspend fun generateResponse(prompt: String): String {
        return withContext(Dispatchers.IO) {
            llamaCppWrapper.generateText(llamaContext, prompt, 512, 0.7f)
        }
    }

    private fun checkAndLoadModel(onStatus: (String) -> Unit) {
        val modelFile = File(filesDir, "gemma-3n-E2B-it-Q4_K_M.gguf")

        if (modelFile.exists()) {
            loadModel(modelFile.absolutePath, onStatus)
        } else {
            onStatus("Downloading model...")
            downloadModel(onStatus)
        }
    }

    private fun downloadModel(onStatus: (String) -> Unit) {
        lifecycleScope.launch {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://huggingface.co/unsloth/gemma-3n-E2B-it-GGUF/resolve/main/gemma-3n-E2B-it-Q4_K_M.gguf")
                    .build()

                withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val modelFile = File(filesDir, "gemma-3n-E2B-it-Q4_K_M.gguf")
                        val outputStream = FileOutputStream(modelFile)
                        response.body?.byteStream()?.use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                        onStatus("Model downloaded successfully")
                        loadModel(modelFile.absolutePath, onStatus)
                    } else {
                        onStatus("Download failed: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                onStatus("Download error: ${e.message}")
            }
        }
    }

    private fun loadModel(modelPath: String, onStatus: (String) -> Unit) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    llamaContext = llamaCppWrapper.initModel(modelPath)
                    if (llamaContext != 0L) {
                        onStatus("Model loaded successfully")
                    } else {
                        onStatus("Failed to load model")
                    }
                }
            } catch (e: Exception) {
                onStatus("Model loading error: ${e.message}")
            }
        }
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)