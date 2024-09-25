package com.shuu0212.ktcameragpt

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class WebViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        // Intent からHTMLファイルのパスを取得
        val htmlFilePath = intent.getStringExtra("HTML_FILE_PATH")

        if (htmlFilePath != null) {
            val file = File(htmlFilePath)

            // WebViewを設定してHTMLファイルを読み込む
            val webView = findViewById<WebView>(R.id.webview)
            webView.webViewClient = WebViewClient()
            webView.settings.javaScriptEnabled = true

            // ローカルのHTMLファイルを表示
            webView.loadUrl("file:///$htmlFilePath")
        }
    }
}
