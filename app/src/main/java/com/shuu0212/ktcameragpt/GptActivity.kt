package com.shuu0212.ktcameragpt

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.shuu0212.ktcameragpt.databinding.ActivityGptBinding
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class GptActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGptBinding
    private lateinit var chatAdapter: ChatAdapter
    private val chatMessages = mutableListOf<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupChatRecyclerView()
        setupSendButton()

        // Display initial message
        addMessageToChat("Hello! I'm GPT. How can I assist you today?", false)
    }

    private fun setupUI() {
        // Set up the toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Chat with GPT"

        // Customize the input field
        binding.inputText.setHint("Type a message...")
    }

    private fun setupChatRecyclerView() {
        chatAdapter = ChatAdapter(chatMessages)
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@GptActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupSendButton() {
        binding.sendButton.setOnClickListener {
            val userInput = binding.inputText.text.toString().trim()
            if (userInput.isNotEmpty()) {
                addMessageToChat(userInput, true)
                sendRequestToGpt(userInput)
                binding.inputText.text.clear()
            } else {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addMessageToChat(message: String, isUser: Boolean) {
        chatMessages.add(ChatMessage(message, isUser))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1)
    }

    private fun sendRequestToGpt(userInput: String) {
        val OpenAiApiKey = BuildConfig.OPENAI_API_KEY
        if (OpenAiApiKey.isEmpty()) {
            Toast.makeText(this, "API key is missing", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        val url = "https://api.openai.com/v1/chat/completions"

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        val json = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userInput)
                })
            })
        }

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $OpenAiApiKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this@GptActivity, "Failed to connect: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread { showLoading(false) }

                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@GptActivity, "Error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                val responseBody = response.body?.string()

                if (responseBody != null) {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val gptResponse = jsonResponse.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")

                        runOnUiThread {
                            addMessageToChat(gptResponse, false)
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@GptActivity, "Failed to parse response: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@GptActivity, "Response body is empty", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.sendButton.isEnabled = !isLoading
        binding.inputText.isEnabled = !isLoading
    }
}