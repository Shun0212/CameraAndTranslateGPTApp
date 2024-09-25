package com.shuu0212.ktcameragpt

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class SendPictureActivity : AppCompatActivity() {

    private var imagePath: String? = null
    private var userInputText: String? = null
    private var serverUrl: String? = null  // QRコードで取得するサーバーのURL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_picture)

        // PictureActivityから画像パスとテキストを受け取る
        imagePath = intent.getStringExtra("imagePath")
        userInputText = intent.getStringExtra("resultText")

        // QRコードをスキャンしてサーバーURLを取得
        val qrIntegrator = IntentIntegrator(this)
        qrIntegrator.setPrompt("QRコードをスキャンしてください")
        qrIntegrator.setOrientationLocked(false)
        qrIntegrator.initiateScan()
    }

    // QRコードのスキャン結果を受け取る
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                serverUrl = result.contents  // スキャンしたURLを保存
                Toast.makeText(this, "サーバーURLを取得しました: $serverUrl", Toast.LENGTH_SHORT).show()

                // QRコードでサーバーURLを取得後、サーバーに画像とテキストを送信
                if (imagePath != null && userInputText != null && serverUrl != null) {
                    sendImageToServer(imagePath!!, userInputText!!, serverUrl!!)
                } else {
                    Toast.makeText(this, "データが不足しています", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "QRコードのスキャンに失敗しました", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    // 画像とテキストをサーバーに送信する関数
    private fun sendImageToServer(imagePath: String, userText: String, serverUrl: String) {
        val imageFile = File(imagePath)
        if (!imageFile.exists()) {
            Toast.makeText(this, "画像ファイルが見つかりません", Toast.LENGTH_SHORT).show()
            return
        }

        // 画像をBase64でエンコード
        val base64Image = encodeImageToBase64(imageFile)

        // OkHttpクライアント設定
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        // JSONオブジェクトを作成してテキストと画像を送信
        val json = JSONObject().apply {
             put("image", base64Image)  // Base64エンコードされた画像データ
            put("gpt_result", userText)  // GPTの結果として送信するテキスト
        }

        // デバッグ用のログ出力
        println("Sending JSON data: $json")

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(serverUrl)
            .post(requestBody)
            .build()

        // サーバーへリクエストを送信
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@SendPictureActivity, "サーバー接続に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@SendPictureActivity, "送信に失敗しました: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                    return
                }
                runOnUiThread {
                    Toast.makeText(this@SendPictureActivity, "データが送信されました", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }


    // 画像をBase64にエンコードする関数
    private fun encodeImageToBase64(imageFile: File): String {
        val bytes = imageFile.readBytes()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }
}
