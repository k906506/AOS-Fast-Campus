package com.example.googlemap

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.googlemap.databinding.ItemSearchBinding
import com.example.googlemap.model.SearchResultEntity

class SearchRecyclerAdapter() :
    RecyclerView.Adapter<SearchRecyclerAdapter.SearchRecyclerViewHolder>() {
    private var searchResultList: List<SearchResultEntity> = listOf()
    private lateinit var itemClickListener: (SearchResultEntity) -> Unit

    inner class SearchRecyclerViewHolder(private val binding: ItemSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SearchResultEntity) {
            binding.titleTextView.text = item.name
            binding.subtitleTextView.text = item.fullAddress

            binding.root.setOnClickListener {
                itemClickListener(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchRecyclerViewHolder {
        return SearchRecyclerViewHolder(
            ItemSearchBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: SearchRecyclerViewHolder, position: Int) {
        return holder.bind(searchResultList[position])
    }

    override fun getItemCount(): Int {
        return searchResultList.size
    }

    fun setSearchResultList(
        searchResultList: List<SearchResultEntity>,
        itemClickListener: (SearchResultEntity) -> Unit
    ) {
        this.searchResultList = searchResultList
        this.itemClickListener = itemClickListener
        notifyDataSetChanged()
    }
}