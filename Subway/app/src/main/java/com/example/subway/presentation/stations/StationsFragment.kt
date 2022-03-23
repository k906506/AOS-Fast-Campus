package com.example.subway.presentation.stations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.subway.databinding.FragmentStationsBinding
import com.example.subway.domain.Station
import com.example.subway.extension.toGone
import com.example.subway.extension.toVisible
import org.koin.android.scope.ScopeFragment

class StationsFragment : ScopeFragment(), StationsContract.View {

    override val presenter: StationsContract.Presenter by inject()

    private var binding: FragmentStationsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentStationsBinding.inflate(inflater, container, false)
        .also { binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        bindViews()
        presenter.onViewCreated()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    override fun showLoadingIndicator() {
        binding?.progressBar?.toVisible()
    }

    override fun hideLoadingIndicator() {
        binding?.progressBar?.toGone()
    }

    override fun showStations(stations: List<Station>) {
        (binding?.recyclerView?.adapter as? StationsAdapter)?.run {
            this.data = stations
            notifyDataSetChanged()
        }
    }

    private fun initViews() {
        binding?.recyclerView?.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = StationsAdapter()
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(DividerItemDecoration(context, RecyclerView.VERTICAL))
        }
    }

    private fun bindViews() {
        binding?.searchEditText?.addTextChangedListener { editable ->
            presenter.filterStations(editable.toString())
        }

        (binding?.recyclerView?.adapter as? StationsAdapter)?.apply {
            onItemClickListener = { station -> }
            onFavoriteClickListener = { station -> }
        }
    }
}