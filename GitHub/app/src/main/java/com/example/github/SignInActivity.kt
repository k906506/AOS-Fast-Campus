package com.example.github

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.isGone
import com.example.github.databinding.ActivitySignInBinding
import com.example.github.utility.AuthTokenProvider
import com.example.github.utility.Key
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class SignInActivity : AppCompatActivity(), CoroutineScope {
    private val binding by lazy {
        ActivitySignInBinding.inflate(layoutInflater)
    }

    private val authTokenProvider by lazy {
        AuthTokenProvider(this)
    }

    val job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if (checkAuthCodeExist()) {
            launchMainActivity()
        } else {
            initViews()
        }
    }

    private fun initViews() = with(binding) {
        loginButton.setOnClickListener {
            loginGithub()
        }
    }

    private fun checkAuthCodeExist(): Boolean = !authTokenProvider.getToken.isNullOrEmpty()

    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        startActivity(intent)
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

        intent?.data?.getQueryParameter("code")?.let {
            launch(coroutineContext) {
                showProgress()
                val progressJob = getAccessToken(it)
                progressJob.join()
                dismissProgress()
            }
        }
    }

    private suspend fun getAccessToken(code: String) = launch(coroutineContext) {
        try {
            withContext(Dispatchers.IO) {
                val response = RetrofitUtil.authApiService.getAccessToken(
                    clientId = Key.GITHUB_CLIENT_ID,
                    clientSecret = Key.GITHUB_CLIENT_SECRET,
                    code = code
                )

                if (response.isSuccessful) {
                    val accessToken = response.body()?.accessToken ?: "login"
                    if (accessToken.isNotEmpty()) {
                        authTokenProvider.setToken(accessToken)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()

        }
    }

    private suspend fun showProgress() = withContext(coroutineContext) {
        with(binding) {
            loginButton.isGone = true
            progressBar.isGone = false
            loadingTextView.isGone = false
        }
    }

    private suspend fun dismissProgress() = withContext(coroutineContext) {
        with(binding) {
            loginButton.isGone = false
            loginButton.isEnabled = false
            progressBar.isGone = true
            loadingTextView.isGone = true

            Toast.makeText(this@SignInActivity, "???????????? ??????????????????.", Toast.LENGTH_SHORT).show()
        }
    }
}