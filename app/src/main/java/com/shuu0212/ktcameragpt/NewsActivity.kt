package com.shuu0212.ktcameragpt

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shuu0212.ktcameragpt.databinding.ActivityNewsBinding
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import android.text.util.Linkify

class NewsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent からのデータ取得
        val repoName = intent.getStringExtra("NEWS_TITLE") ?: ""
        val repoDescription = intent.getStringExtra("NEWS_DESCRIPTION") ?: ""
        val repoUrl = intent.getStringExtra("NEWS_URL") ?: ""

        // TextView にリポジトリ情報を表示
        binding.repoNameTextView.text = repoName
        binding.repoDescriptionTextView.text = repoDescription
        binding.repoUrlTextView.text = repoUrl

        Linkify.addLinks(binding.repoUrlTextView, Linkify.WEB_URLS)


        // 「要約を取得」ボタンのクリックリスナー
        binding.fetchReadmeButton.setOnClickListener {
            fetchReadme(repoName)  // READMEを取得し、要約を取得する処理を呼び出す
        }

        // 「全文表示」ボタンのクリックリスナー
        binding.viewFullReadmeButton.setOnClickListener {
            fetchFullReadmeAndShow(repoName)  // README全文を取得して次の画面に渡す処理
        }

        binding.qrCodeButton.setOnClickListener {
            val repoUrl = binding.repoUrlTextView.text.toString()
            if (repoUrl.isNotEmpty()) {
                val intent = Intent(this, QRActivity::class.java)
                intent.putExtra("NEWS_URL", repoUrl)  // キーを"NEWS_URL"に統一
                startActivity(intent)
            } else {
                Toast.makeText(this, "URLが空です。", Toast.LENGTH_SHORT).show()
            }
        }



    }
    private fun fetchFullReadmeAndShow(repoName: String) {
        val githubReadmeUrl = "https://api.github.com/repos/$repoName/readme"
        val githubToken = BuildConfig.GITHUB_API_KEY // GitHubのトークンを設定

        val client = OkHttpClient()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(githubReadmeUrl)
                    .addHeader("Authorization", "token $githubToken")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@NewsActivity, "Failed to fetch README. Status code: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val responseBody = response.body?.string()

                if (responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.has("content")) {
                        val contentBase64 = jsonResponse.getString("content")

                        // Base64デコードしてREADMEの全文を取得
                        val readmeContent = android.util.Base64.decode(contentBase64, android.util.Base64.DEFAULT).toString(Charsets.UTF_8)

                        // README全文を次のアクティビティに渡す
                        withContext(Dispatchers.Main) {
                            val intent = Intent(this@NewsActivity, ReadmeActivity::class.java)
                            intent.putExtra("README_CONTENT", readmeContent)
                            startActivity(intent)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@NewsActivity, "No README found in the repository", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NewsActivity, "Error fetching README: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchReadme(repoName: String) {
        val githubReadmeUrl = "https://api.github.com/repos/$repoName/readme"
        val githubToken = BuildConfig.GITHUB_API_KEY
        val client = OkHttpClient()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(githubReadmeUrl)
                    .addHeader("Authorization", "token $githubToken")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@NewsActivity, "Failed to fetch README. Status code: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val responseBody = response.body?.string()

                if (responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.has("content")) {
                        val contentBase64 = jsonResponse.getString("content")

                        // Base64デコードしてREADMEの内容を取得
                        val readmeContent = android.util.Base64.decode(contentBase64, android.util.Base64.DEFAULT).toString(Charsets.UTF_8)

                        // 最初の8行を抽出してChatGPTに送信
                        val first8Lines = readmeContent.lines().take(20).joinToString("\n")
                        sendRequestToGpt(first8Lines)

                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@NewsActivity, "No README found in the repository", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NewsActivity, "Error fetching README: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendRequestToGpt(readmeContent: String) {
        val apiKey = BuildConfig.OPENAI_API_KEY
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "API key is missing", Toast.LENGTH_SHORT).show()
            return
        }

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
                    put("content", "以下のREADMEファイルの最初の20行を読み、どのようなプログラムのリポジトリであるか教えてください、もし、画像表示のURLなどが多く内容がわからない場合は[全文を表示し内容を確認してください]と送ってください:\n$readmeContent")
                })
            })
        }

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@NewsActivity, "Failed to connect: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                if (responseBody != null) {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        if (jsonResponse.has("error")) {
                            val errorMessage = jsonResponse.getJSONObject("error").getString("message")
                            runOnUiThread {
                                Toast.makeText(this@NewsActivity, "API Error: $errorMessage", Toast.LENGTH_LONG).show()
                            }
                        } else if (jsonResponse.has("choices")) {
                            val gptResponse = jsonResponse.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")

                            runOnUiThread {
                                binding.analysisTextView.text = gptResponse
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@NewsActivity, "Unexpected API response format", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@NewsActivity, "Failed to parse response: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }
}