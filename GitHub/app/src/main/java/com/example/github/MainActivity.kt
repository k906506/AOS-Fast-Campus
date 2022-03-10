package com.example.github

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
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

    private val adapter by lazy {
        RepositoryAdapter(itemClickListener = {
            val intent = Intent(this@MainActivity, RepositoryActivity::class.java)
            intent.putExtra(RepositoryActivity.REPOSITORY_NAME_KEY, it.name)
            intent.putExtra(RepositoryActivity.REPOSITORY_OWNER_KEY, it.owner.login)
            startActivity(intent)
        })
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
        floatingActionButton.setOnClickListener {
            val intent = Intent(this@MainActivity, SearchActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        launch(coroutineContext) {
            loadLikedRepositoryList()
        }
    }

    private suspend fun loadLikedRepositoryList() = withContext(Dispatchers.IO) {
        val repoList = db?.githubDao()?.getHistory()
        withContext(Dispatchers.Main) {
            setData(repoList!!)
        }
    }

    private fun setData(githubRepositoryList: List<GithubRepository>) = with(binding) {
        if (githubRepositoryList.isEmpty()) {
            emptyResultTextView.isGone = false
            recyclerView.isGone = true
        } else {
            emptyResultTextView.isGone = true

            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(applicationContext)
            adapter.submitList(githubRepositoryList)

            recyclerView.isGone = false
        }
    }
}