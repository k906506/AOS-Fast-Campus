package com.example.github

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.github.data.database.SimpleGithubDatabase
import com.example.github.data.entity.GithubOwner
import com.example.github.data.entity.GithubRepository
import com.example.github.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val db by lazy {
        SimpleGithubDatabase.getInstance(applicationContext)
    }

    val job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        launch {
            addMockData()
            val repository = loadGithubRepositories()
            withContext(coroutineContext) {
                Log.d("repository", repository.toString())
            }
        }

        initViews()
    }

    private fun initViews() = with(binding) {
        floatingActionButton.setOnClickListener {
            val intent = Intent(this@MainActivity, SearchActivity::class.java)
            startActivity(intent)
        }
    }

    private suspend fun addMockData() = withContext(Dispatchers.IO) {
        val mockData = (0..10).map {
            GithubRepository(
                name = "repo $it",
                fullName = "name $it",
                owner = GithubOwner(
                    "login",
                    "avatarUrl",
                ),
                description = null,
                language = null,
                updateAt = Date().toString(),
                stargazersCount = it
            )
        }

        db?.githubDao()?.insertAll(mockData)
    }

    private suspend fun loadGithubRepositories() = withContext(Dispatchers.IO) {
        val histories = db?.githubDao()?.getHistory()
        return@withContext histories
    }
}