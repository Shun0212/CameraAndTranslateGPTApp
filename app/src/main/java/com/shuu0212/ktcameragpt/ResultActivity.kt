package com.shuu0212.ktcameragpt

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shuu0212.ktcameragpt.databinding.ActivityResultBinding
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)


        photoUri = intent.getStringExtra("PHOTO_URI")?.let { Uri.parse(it) }

        // Send the image to GPT on activity start
        photoUri?.let {
            sendImageToGpt(it)
        } ?: Toast.makeText(this, "No photo found", Toast.LENGTH_SHORT).show()
    }

    private fun sendImageToGpt(uri: Uri) {
        val sharedPreferences = getSharedPreferences("api_keys", MODE_PRIVATE)
        val openAiApiKey = sharedPreferences.getString("OPENAI_API_KEY", "") ?: ""
        val url = "https://api.openai.com/v1/chat/completions"

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        val json = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", JSONObject().apply {
                put("role", "user")
                put("content", "Analyze this image")
            })
        }

        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), json.toString())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $openAiApiKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ResultActivity, "Failed to connect", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                responseBody?.let {
                    val jsonResponse = JSONObject(it)
                    val gptResponse = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    runOnUiThread {
                        binding.responseText.text = gptResponse
                    }
                }
            }
        })
    }
}
