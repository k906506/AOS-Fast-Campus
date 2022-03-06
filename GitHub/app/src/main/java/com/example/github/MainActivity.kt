package com.example.github

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import com.example.github.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    val job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initViews()
    }

    private fun initViews() = with(binding) {
        loginButton.setOnClickListener {
            loginGithub()
        }
    }

    private fun loginGithub() {
        val loginUrl = Uri.Builder().scheme("https").authority("github.com")
            .appendPath("login")
            .appendPath("oauth")
            .appendPath("authorize")
            .appendQueryParameter("client_id", Key.GITHUB_CLIENT_ID)
            .build()

        CustomTabsIntent.Builder().build().also {
            it.launchUrl(this, loginUrl)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // todo getAccessToken
        intent?.data?.getQueryParameter("code")?.let {
            launch(coroutineContext) {
                getAccessToken(it)
            }
        }
    }

    private suspend fun getAccessToken(code: String) = withContext(Dispatchers.IO) {
    }
}