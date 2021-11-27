package com.android.chatapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.android.chatapp.databinding.ActivityLatestMessagesBinding
import com.android.chatapp.databinding.ActivityProfileBinding

class LatestMessagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLatestMessagesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLatestMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}