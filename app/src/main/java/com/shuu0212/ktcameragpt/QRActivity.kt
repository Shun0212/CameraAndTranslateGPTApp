package com.shuu0212.ktcameragpt

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException
import android.content.Intent
import java.net.MalformedURLException

class QRActivity : AppCompatActivity() {

    private lateinit var serverUrl: String  // QRコードから取得するサーバURL
    private lateinit var urlTextView: TextView
    private lateinit var githubUrl: String  // NewsActivityから受け取るGitHubのURL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr)

        // UI要素を取得
        urlTextView = findViewById(R.id.urlTextView)

        // NewsActivityから受け取ったGitHubのURLを取得
        githubUrl = intent.getStringExtra("NEWS_URL") ?: ""

        // GitHub URLが空でないか確認
        if (githubUrl.isEmpty()) {
            Toast.makeText(this, "GitHub URLが無効です", Toast.LENGTH_SHORT).show()
            finish()  // URLが空の場合はアクティビティを終了
            return
        }

        Toast.makeText(this, "GitHub URL: $githubUrl", Toast.LENGTH_SHORT).show()

        // QRコードをスキャンするための初期化
        val integrator = IntentIntegrator(this)
        integrator.setPrompt("QRコードをスキャンしてください")
        integrator.setOrientationLocked(true)
        integrator.setBeepEnabled(true)
        integrator.initiateScan()  // QRコードスキャンを開始
    }

    // QRコードスキャンが終了した時の結果を受け取る
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                // QRコードから読み取ったサーバURLを取得
                serverUrl = result.contents
                Toast.makeText(this, "サーバのURL: $serverUrl", Toast.LENGTH_SHORT).show()

                // URLが正しいかを事前にチェック
                if (!isValidUrl(serverUrl)) {
                    Toast.makeText(this, "無効なサーバURLです", Toast.LENGTH_SHORT).show()
                    return
                }

                // 現在のサーバURLをTextViewに表示
                urlTextView.text = "サーバURL: $serverUrl"

                // GitHubのURLをHTTPサーバにPOSTで送信
                sendUrlToHttpServer(githubUrl)
            } else {
                Toast.makeText(this, "QRコードのスキャンに失敗しました", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    // URLが有効かどうかをチェックする関数
    private fun isValidUrl(urlString: String): Boolean {
        return try {
            URL(urlString)
            true
        } catch (e: MalformedURLException) {
            false
        }
    }

    // GitHubのURLをHTTPサーバにPOSTで送信する関数（JSON形式）
    private fun sendUrlToHttpServer(githubUrl: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // サーバーURLの確認
                val url = URL(serverUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 10000  // 接続タイムアウト (10秒)
                connection.readTimeout = 10000  // 読み込みタイムアウト (10秒)
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

                // JSONデータを作成
                val jsonParam = JSONObject().apply {
                    put("github_url", githubUrl)
                }

                // JSONデータをPOSTとして送信
                connection.outputStream.use { outputStream ->
                    val writer = OutputStreamWriter(outputStream, "UTF-8")
                    writer.write(jsonParam.toString())
                    writer.flush()
                    writer.close()
                }

                // レスポンスコードを確認
                val responseCode = connection.responseCode
                withContext(Dispatchers.Main) {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        Toast.makeText(this@QRActivity, "URLを送信しました", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@QRActivity, "レスポンスコード: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@QRActivity, "サーバ接続に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
