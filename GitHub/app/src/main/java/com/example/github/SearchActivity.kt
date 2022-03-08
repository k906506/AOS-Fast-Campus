package com.example.github

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.github.databinding.ActivitySearchBinding
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class SearchActivity : AppCompatActivity(), CoroutineScope {
    private val binding by lazy {
        ActivitySearchBinding.inflate(layoutInflater)
    }

    private val adapter by lazy {
        RepositoryAdapter(itemClickListener = {
            val intent = Intent(this, RepositoryActivity::class.java)
            intent.putExtra(RepositoryActivity.REPOSITORY_NAME_KEY, it.name ?: "Repository 이름이 없습니다.")
            intent.putExtra(RepositoryActivity.REPOSITORY_OWNER_KEY, it.owner.login ?: "Repository 소유자가 없습니다.")
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
        bindViews()
    }

    private fun initViews() = with(binding) {
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(applicationContext)
    }

    private fun bindViews() = with(binding) {
        searchButton.setOnClickListener {
            searchKeyword(searchEditText.text.toString())
        }
    }

    private fun searchKeyword(keyword: String) = launch {
        withContext(Dispatchers.IO) {
            val response = RetrofitUtil.githubApiSearchService.searchRepositories(keyword)
            if (response.isSuccessful) {
                val body = response.body()
                withContext(Dispatchers.Main) {
                    adapter.submitList(body?.items)
                }
            }
        }
    }

}