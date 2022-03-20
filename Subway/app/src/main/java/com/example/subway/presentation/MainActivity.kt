package com.example.subway.presentation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.subway.R
import com.example.subway.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}