package com.shuu0212.ktcameragpt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.shuu0212.ktcameragpt.databinding.ActivityFirstBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class FirstActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFirstBinding
    private lateinit var newsAdapter: NewsAdapter

    // デフォルトのプロンプト（ここは使用されない）
    private var defaultPrompt = "Android 最新"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirstBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        binding.navigateButton.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        binding.navigateToGptButton.setOnClickListener {
            startActivity(Intent(this, GptActivity::class.java))
        }

        binding.navigateToWhisperButton.setOnClickListener {
            startActivity(Intent(this, WhisperActivity::class.java))
        }

        // プロンプト入力ボタン
        binding.searchButton.setOnClickListener {
            val prompt = binding.searchEditText.text.toString()
            fetchRepositories(prompt)  // GitHubリポジトリを検索
        }
        // 設定画面への遷移ボタン
        binding.navigateToSettingButton.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter { news ->
            val intent = Intent(this, NewsActivity::class.java).apply {
                putExtra("NEWS_TITLE", news.title)
                putExtra("NEWS_DESCRIPTION", news.description)
                putExtra("NEWS_URL", news.url)
            }
            startActivity(intent)
        }
        binding.newsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@FirstActivity)
            adapter = newsAdapter
        }
    }

    private fun fetchRepositories(query: String) {
        binding.progressBar.visibility = View.VISIBLE

        // GitHub APIのURL（リポジトリ検索）
        val githubSearchUrl = "https://api.github.com/search/repositories?q=$query"

        // OkHttpクライアント
        val client = OkHttpClient()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                // GitHubリポジトリ検索リクエストの作成
                val request = Request.Builder()
                    .url(githubSearchUrl)
                    .build()

                // リクエストを実行
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (responseBody != null) {
                    // レスポンスの解析
                    val jsonResponse = JSONObject(responseBody)
                    val items = jsonResponse.getJSONArray("items")
                    val repoList = mutableListOf<News>()

                    for (i in 0 until items.length()) {
                        val repo = items.getJSONObject(i)
                        val name = repo.getString("full_name")
                        val description = repo.optString("description", "説明なし")
                        val url = repo.getString("html_url")

                        // リポジトリデータをリストに追加
                        val news = News(name, description, url, "")
                        repoList.add(news)
                    }

                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        if (repoList.isNotEmpty()) {
                            newsAdapter.submitList(repoList)
                            binding.newsRecyclerView.visibility = View.VISIBLE
                            binding.noNewsTextView.visibility = View.GONE
                        } else {
                            binding.noNewsTextView.visibility = View.VISIBLE
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("FirstActivity", "Error fetching repositories: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.errorTextView.visibility = View.VISIBLE
                }
            }
        }
    }
}
