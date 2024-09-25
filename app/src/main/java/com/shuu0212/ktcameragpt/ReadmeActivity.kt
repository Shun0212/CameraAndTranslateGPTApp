package com.shuu0212.ktcameragpt

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import androidx.appcompat.app.AppCompatActivity
import com.shuu0212.ktcameragpt.databinding.ActivityReadmeBinding

class ReadmeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReadmeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadmeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent から README の全文を取得
        val readmeContent = intent.getStringExtra("README_CONTENT") ?: "READMEの内容がありません"

        // TextViewに全文を表示
        binding.readmeTextView.text = readmeContent

        // LinkifyでURLを自動的にリンク化
        Linkify.addLinks(binding.readmeTextView, Linkify.WEB_URLS)

        // TextView内のリンクをクリック可能に設定
        binding.readmeTextView.movementMethod = LinkMovementMethod.getInstance()
    }
}
