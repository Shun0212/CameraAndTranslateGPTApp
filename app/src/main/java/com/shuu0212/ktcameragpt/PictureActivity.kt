package com.shuu0212.ktcameragpt

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shuu0212.ktcameragpt.databinding.ActivityPictureBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class PictureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPictureBinding
    private var imagePath: String? = null
    private lateinit var promptTemplates: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPictureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Initialize prompt templates
        promptTemplates = arrayOf(
            "テンプレートを使う場合はここから選んでください",
            "この写真に何が写っているか説明してください",
            "この写真に写っているプログラムについて説明してください",
            "この写真の数式をLatexで書き直してください",
            "この写真の文章を翻訳してください",
            "この写真の内容を要約してください"
        )

        // Set up the spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, promptTemplates)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.promptSpinner.adapter = adapter

        // Handle spinner item selection
        binding.promptSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedPrompt = promptTemplates[position]
                if (selectedPrompt != "Custom prompt") {
                    binding.editText.setText(selectedPrompt)
                } else {
                    binding.editText.text.clear()
                    binding.editText.hint = "Enter your custom prompt"
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        // 画像パスを取得
        imagePath = intent.getStringExtra("imagePath")

        // 画像を表示
        if (imagePath != null) {
            val imgFile = File(imagePath!!)
            if (imgFile.exists()) {
                val bitmap = getRotatedBitmap(imgFile.absolutePath)
                binding.imageView.setImageBitmap(bitmap)
            } else {
                Toast.makeText(this, "Image not found", Toast.LENGTH_SHORT).show()
            }
        }

        // キャンセルボタン
        binding.cancelButton.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
            finish()
        }

        // GPTに画像とテキストを送信
        binding.sendButton.setOnClickListener {
            if (imagePath != null) {
                val userInputText = binding.editText.text.toString()  // EditTextから入力されたテキストを取得
                if (userInputText.isNotBlank()) {
                    sendImageToGpt(imagePath!!, userInputText)  // テキストと画像を一緒に送信
                } else {
                    Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No image to send", Toast.LENGTH_SHORT).show()
            }
        }


        // 「PCへ送る」ボタンの処理
        binding.sendactButton.setOnClickListener {
            if (imagePath != null && binding.editText.text.isNotBlank()) {
                // SendPictureActivityへ画像パスとテキストを渡して起動
                val intent = Intent(this, SendPictureActivity::class.java)
                intent.putExtra("imagePath", imagePath)  // 画像パスを渡す
                intent.putExtra("resultText", binding.editText.text.toString())  // テキストを渡す
                startActivity(intent)
            } else {
                Toast.makeText(this, "画像とテキストを入力してください", Toast.LENGTH_SHORT).show()
            }
        }


    }

    private fun getRotatedBitmap(imagePath: String): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
        }
        var bitmap = BitmapFactory.decodeFile(imagePath, options)

        val exif = ExifInterface(imagePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        return bitmap
    }

    private fun sendImageToGpt(imagePath: String, userText: String) {
        val apiKey = BuildConfig.OPENAI_API_KEY
        val url = "https://api.openai.com/v1/chat/completions"

        val imageFile = File(imagePath)
        if (!imageFile.exists()) {
            runOnUiThread {
                Toast.makeText(this@PictureActivity, "Image file not found", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val base64Image = encodeImageToBase64(imageFile)

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        // JSONオブジェクトを作成してテキストと画像を一緒に送信
        val json = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", userText)  // ユーザーが入力したテキストを送信
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$base64Image")  // Base64エンコードされた画像を送信
                            })
                        })
                    })
                })
            })
            put("max_tokens", 10000)
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
                    Toast.makeText(this@PictureActivity, "Failed to connect to the server", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@PictureActivity, "API request failed: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                val responseBody = response.body?.string()
                responseBody?.let {
                    try {
                        val jsonResponse = JSONObject(it)
                        if (jsonResponse.has("choices")) {
                            val gptResponse = jsonResponse.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")

                            runOnUiThread {
                                binding.resultText.text = gptResponse

                                // 「PCへ送る」ボタンの処理でGPTの結果を渡す
                                binding.sendactButton.setOnClickListener {
                                    if (imagePath != null) {
                                        // GPTの結果をSendPictureActivityに渡す
                                        val intent = Intent(this@PictureActivity, SendPictureActivity::class.java)
                                        intent.putExtra("imagePath", imagePath)  // 画像パスを渡す
                                        intent.putExtra("resultText", gptResponse)  // GPTの結果を渡す
                                        startActivity(intent)
                                    } else {
                                        Toast.makeText(this@PictureActivity, "画像がありません", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@PictureActivity, "No choices in response", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: JSONException) {
                        runOnUiThread {
                            Toast.makeText(this@PictureActivity, "Failed to parse response: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

        })
    }

    private fun encodeImageToBase64(imageFile: File): String {
        val bytes = imageFile.readBytes()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }
}
