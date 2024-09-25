package com.shuu0212.ktcameragpt

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.shuu0212.ktcameragpt.databinding.ActivitySettingBinding

class SettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewBindingを使用
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 既存のAPIキーを取得して表示
        sharedPreferences = getSharedPreferences("api_keys", MODE_PRIVATE)
        val githubApiKey = sharedPreferences.getString("GITHUB_API_KEY", "")
        val openAiApiKey = sharedPreferences.getString("OPENAI_API_KEY", "")
        binding.githubApiKeyInput.setText(githubApiKey)
        binding.openAiApiKeyInput.setText(openAiApiKey)

        // 保存ボタンの処理
        binding.saveButton.setOnClickListener {
            val newGithubApiKey = binding.githubApiKeyInput.text.toString()
            val newOpenAiApiKey = binding.openAiApiKeyInput.text.toString()

            val editor = sharedPreferences.edit()
            editor.putString("GITHUB_API_KEY", newGithubApiKey)
            editor.putString("OPENAI_API_KEY", newOpenAiApiKey)
            editor.apply()


            binding.statusTextView.text = "APIキーが保存されました"
        }
    }
}
