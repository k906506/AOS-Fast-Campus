package com.example.github

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import com.example.github.data.database.SimpleGithubDatabase
import com.example.github.data.entity.GithubRepository
import com.example.github.databinding.ActivityRepositoryBinding
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class RepositoryActivity : AppCompatActivity(), CoroutineScope {
    private val binding by lazy {
        ActivityRepositoryBinding.inflate(layoutInflater)
    }

    private val db by lazy {
        SimpleGithubDatabase.getInstance(applicationContext)
    }

    lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        job = Job()

        val repositoryOwner = intent.getStringExtra(REPOSITORY_OWNER_KEY) ?: "소유자가 없습니다."
        val repositoryName = intent.getStringExtra(REPOSITORY_NAME_KEY) ?: "이름이 없습니다."

        launch {
            loadRepository(repositoryOwner, repositoryName)?.let {
                setData(it)
            } ?: run {
                Toast.makeText(this@RepositoryActivity, "Repository 정보가 없습니다.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }

        showProgress()
    }

    private suspend fun loadRepository(
        repositoryOwner: String,
        repositoryName: String
    ): GithubRepository? = withContext(coroutineContext) {
        var repository: GithubRepository? = null

        withContext(Dispatchers.IO) {
            val response = RetrofitUtil.githubApiSearchService.getRepository(
                ownerLogin = repositoryOwner,
                repoName = repositoryName
            )
            if (response.isSuccessful) {
                val body = response.body()
                withContext(Dispatchers.Main) {
                    body?.let {
                        repository = it
                    }
                }
            }

        }
        repository
    }

    private fun setData(githubRepository: GithubRepository) = with(binding) {
        dismissProgress()
        ownerNameAndRepoNameTextView.text =
            "${githubRepository.owner.login}/${githubRepository.name}"
        stargazersCountText.text = githubRepository.stargazersCount.toString()
        githubRepository.language?.let { language ->
            languageText.isGone = false
            languageText.text = language
        } ?: kotlin.run {
            languageText.isGone = true
            languageText.text = ""
        }
        descriptionTextView.text = githubRepository.description
        updateTimeTextView.text = githubRepository.updateAt

        setLikeState(githubRepository)
    }

    private fun setLikeState(githubRepository: GithubRepository) = launch {
        withContext(Dispatchers.IO) {
            val repository = db?.githubDao()?.getRepository(githubRepository.name)
            val isLiked = repository != null
            withContext(Dispatchers.Main) {
                setLikeImage(isLiked)
                binding.likeButton.setOnClickListener {
                    addLikeGithubRepository(githubRepository, isLiked)
                }
            }
        }
    }

    private fun setLikeImage(isLiked: Boolean) = with(binding) {
        likeButton.setImageDrawable(
            ContextCompat.getDrawable(
                this@RepositoryActivity,
                if (isLiked) {
                    R.drawable.ic_baseline_mood_bad_24
                } else {
                    R.drawable.ic_baseline_mood_24
                }
            )
        )
    }

    private fun addLikeGithubRepository(githubRepository: GithubRepository, isLike: Boolean) =
        launch {
            withContext(Dispatchers.IO) {
                if (isLike) {
                    db?.githubDao()?.deleteRepository(githubRepository.name)
                } else {
                    db?.githubDao()?.insert(githubRepository)
                }
                withContext(Dispatchers.Main) {
                    setLikeImage(isLike.not())
                }
            }
        }

    private fun showProgress() {
        with(binding) {
            progressBar.isGone = false
        }
    }

    private  fun dismissProgress() {
        with(binding) {
            progressBar.isGone = true
        }
    }

    companion object {
        const val REPOSITORY_OWNER_KEY = "repositoryOwner"
        const val REPOSITORY_NAME_KEY = "repositoryName"
    }
}