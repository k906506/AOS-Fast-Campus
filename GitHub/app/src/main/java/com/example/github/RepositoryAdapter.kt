package com.example.github

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.github.RepositoryAdapter.RepositoryViewHolder
import com.example.github.data.entity.GithubRepository
import com.example.github.databinding.ItemRepositoryBinding

class RepositoryAdapter(val itemClickListener: (GithubRepository) -> Unit) :
    ListAdapter<GithubRepository, RepositoryViewHolder>(diffUtil) {
    inner class RepositoryViewHolder(private val binding: ItemRepositoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindData(repo: GithubRepository) = with(binding) {
            Glide.with(ownerProfileImageView.context)
                .load(repo.owner.avatarUrl)
                .into(ownerProfileImageView)

            ownerNameTextView.text = repo.owner.login
            nameTextView.text = repo.fullName
            subtextTextView.text = repo.description
            stargazersCountText.text = repo.stargazersCount.toString()
            repo.language?.let {
                languageText.isGone = false
                languageText.text = it
            } ?: kotlin.run {
                languageText.isGone = true
                languageText.text = ""
            }

        }

        fun bindViews(repo: GithubRepository) {
            binding.root.setOnClickListener {
                itemClickListener(repo)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepositoryViewHolder {
        return RepositoryViewHolder(
            ItemRepositoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RepositoryViewHolder, position: Int) {
        holder.bindData(currentList[position])
        holder.bindViews(currentList[position])
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<GithubRepository>() {
            override fun areItemsTheSame(
                oldItem: GithubRepository,
                newItem: GithubRepository
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: GithubRepository,
                newItem: GithubRepository
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}