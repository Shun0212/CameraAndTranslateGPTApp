package com.shuu0212.ktcameragpt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val chatMessages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.chatMessageText)
        val messageLayout: ConstraintLayout = itemView.findViewById(R.id.messageLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chatMessage = chatMessages[position]

        holder.messageText.text = chatMessage.message

        // メッセージの寄せ方を制御
        val params = holder.messageText.layoutParams as ConstraintLayout.LayoutParams
        if (chatMessage.isUser) {
            // 右寄せ（ユーザーメッセージ）
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            params.startToStart = ConstraintLayout.LayoutParams.UNSET
            holder.messageText.setBackgroundResource(R.drawable.user_message_background)
        } else {
            // 左寄せ（GPTメッセージ）
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET
            holder.messageText.setBackgroundResource(R.drawable.gpt_message_background)
        }
        holder.messageText.layoutParams = params
    }

    override fun getItemCount(): Int = chatMessages.size
}
