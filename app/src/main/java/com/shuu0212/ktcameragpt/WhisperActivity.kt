package com.shuu0212.ktcameragpt

import android.media.MediaPlayer
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.shuu0212.ktcameragpt.databinding.ActivityWhisperBinding
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import android.text.Editable.Factory.getInstance

class WhisperActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityWhisperBinding
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private var transcribedText: String? = null
    private val RECORD_AUDIO_REQUEST_CODE = 101

    private var recordingStartTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var recordingTimeRunnable: Runnable

    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWhisperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this, this)

        // 録音開始ボタンのクリック処理
        binding.recordButton.setOnClickListener {
            if (checkPermission()) {
                startRecording()
            } else {
                requestPermission()
            }
        }

        // 録音停止ボタンのクリック処理
        binding.stopButton.setOnClickListener {
            stopRecording()
        }

        // Whisper APIに送信してテキスト化するためのボタン処理
        binding.displayButton.setOnClickListener {
            audioFilePath?.let { path ->
                sendAudioToWhisperAPI(path)
            } ?: Toast.makeText(this, "No audio recorded", Toast.LENGTH_SHORT).show()
        }

        // gpt-4o-miniを使って翻訳するボタンのクリック処理
        binding.translateButton.setOnClickListener {
            transcribedText?.let { text ->
                translateTextUsingGpt(text)
            } ?: Toast.makeText(this, "No text to translate", Toast.LENGTH_SHORT).show()
        }

        // 発話ボタンのクリック処理
        binding.speakButton.setOnClickListener {
            val textToSpeak = binding.resultText.text.toString()
            if (textToSpeak.isNotEmpty()) {
                generateSpeechUsingOpenAI(textToSpeak)
            } else {
                Toast.makeText(this, "No text to speak", Toast.LENGTH_SHORT).show()
            }
        }


        // タイマー処理のRunnable
        recordingTimeRunnable = object : Runnable {
            override fun run() {
                val elapsedTime = System.currentTimeMillis() - recordingStartTime
                val seconds = (elapsedTime / 1000).toInt()
                binding.recordingTimeText.text = String.format("%02d:%02d", seconds / 60, seconds % 60)
                handler.postDelayed(this, 1000) // 1秒ごとにタイマーを更新
            }
        }
    }

    // TextToSpeech初期化完了時のコールバック
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "This language is not supported", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playAudio(filePath: String) {
        val mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            start()
        }
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
    }

    // テキストを発話する
    private fun generateSpeechUsingOpenAI(text: String) {
        val apiKey = BuildConfig.OPENAI_API_KEY // Replace with your actual API key securely
        val url = "https://api.openai.com/v1/audio/speech"

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        val json = JSONObject().apply {
            put("model", "tts-1")
            put("voice", "alloy")
            put("input", text)
        }

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@WhisperActivity, "Failed to connect to the server: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@WhisperActivity, "API request failed: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                val audioData = response.body?.bytes()
                if (audioData != null) {
                    val audioFilePath = "${externalCacheDir?.absolutePath}/speech.mp3"
                    val audioFile = File(audioFilePath)
                    audioFile.writeBytes(audioData)

                    runOnUiThread {
                        playAudio(audioFilePath)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@WhisperActivity, "Failed to receive audio data", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }


    // 録音を開始する
    private fun startRecording() {
        audioFilePath = "${externalCacheDir?.absolutePath}/audio_record.m4a"
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(1) // モノラル録音
            setAudioSamplingRate(22050) // サンプリングレートを下げる
            setAudioEncodingBitRate(32000) // ビットレートを下げる
            setOutputFile(audioFilePath)
            try {
                prepare()
                start()
                recordingStartTime = System.currentTimeMillis()
                handler.post(recordingTimeRunnable)
                Toast.makeText(this@WhisperActivity, "Recording started", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(this@WhisperActivity, "Recording failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 録音を停止し、ファイルサイズを表示する
    private fun stopRecording() {
        mediaRecorder?.apply {
            try {
                stop()
            } catch (e: IllegalStateException) {
                Toast.makeText(this@WhisperActivity, "No active recording to stop", Toast.LENGTH_SHORT).show()
            } finally {
                release()
            }
        }
        mediaRecorder = null
        handler.removeCallbacks(recordingTimeRunnable)
        binding.recordingTimeText.text = "00:00"
        Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()

        // 録音ファイルの大きさを表示
        audioFilePath?.let { path ->
            val audioFile = File(path)
            val fileSizeInKB = audioFile.length() / 1024 // KBに変換
            binding.recordingTimeText.text = "File size: $fileSizeInKB KB"
        }
    }

    // Whisper APIに音声を送信してテキスト化する
    private fun sendAudioToWhisperAPI(audioFilePath: String) {
        val apiKey = BuildConfig.OPENAI_API_KEY
        val url = "https://api.openai.com/v1/audio/transcriptions"

        val audioFile = File(audioFilePath)
        if (!audioFile.exists()) {
            Toast.makeText(this, "Audio file not found", Toast.LENGTH_SHORT).show()
            return
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", audioFile.name,
                audioFile.asRequestBody("audio/m4a".toMediaTypeOrNull())
            )
            .addFormDataPart("model", "whisper-1")
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@WhisperActivity, "Failed to connect to the server: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody == null) {
                    runOnUiThread {
                        Toast.makeText(this@WhisperActivity, "API request failed: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                try {
                    val jsonResponse = JSONObject(responseBody)
                    transcribedText = jsonResponse.getString("text") // 変換されたテキストを保存
                    runOnUiThread {
                        binding.resultText.text = getInstance().newEditable(transcribedText) // 画面に表示
                    }
                } catch (e: JSONException) {
                    runOnUiThread {
                        Toast.makeText(this@WhisperActivity, "Failed to parse response: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // GPT-4を使ってテキストを翻訳する
    private fun translateTextUsingGpt(text: String) {
        val apiKey = BuildConfig.OPENAI_API_KEY
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
                    put("content", "これが英語の場合は日本語に、日本語の場合は英語に翻訳してください: $text")
                })
            })
        }

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@WhisperActivity, "Failed to connect to the server: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody == null) {
                    runOnUiThread {
                        Toast.makeText(this@WhisperActivity, "API request failed: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                try {
                    val jsonResponse = JSONObject(responseBody)
                    val translatedText = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    runOnUiThread {
                        binding.resultText.text = getInstance().newEditable(translatedText) // 翻訳されたテキストを表示
                    }
                } catch (e: JSONException) {
                    runOnUiThread {
                        Toast.makeText(this@WhisperActivity, "Failed to parse response: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // 録音のパーミッションを確認
    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    // パーミッションのリクエスト
    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}
