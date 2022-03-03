package com.example.googlemap

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.googlemap.MapActivity.Companion.SEARCH_RESULT_EXTRA_KEY
import com.example.googlemap.databinding.ActivityMainBinding
import com.example.googlemap.model.LocationLatLngEntity
import com.example.googlemap.model.SearchResultEntity
import com.example.googlemap.response.search.Poi
import com.example.googlemap.response.search.Pois
import com.example.googlemap.utility.RetrofitUtil
import kotlinx.coroutines.*
import java.lang.Exception
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var adapter: SearchRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initJob()
        initAdapter()

        initData()
        searchKeyword()
    }

    private fun initJob() {
        job = Job()
    }

    private fun initAdapter() {
        adapter = SearchRecyclerAdapter()

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(applicationContext)
    }

    private fun initData() {
        adapter.notifyDataSetChanged()
    }

    private fun setData(pois: Pois) {
        val dataList = pois.poi.map {
            SearchResultEntity(
                name = it.name ?: "이름이 존재하지 않습니다.",
                fullAddress = makeMainAddress(it),
                locationLatLng = LocationLatLngEntity(
                    it.noorLat,
                    it.noorLon
                )
            )
        }

        adapter.setSearchResultList(dataList, itemClickListener = {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra(SEARCH_RESULT_EXTRA_KEY, it)
            startActivity(intent)
        })

    }

    private fun makeMainAddress(poi: Poi): String =
        if (poi.secondNo?.trim().isNullOrEmpty()) {
            (poi.upperAddrName?.trim() ?: "") + " " +
                    (poi.middleAddrName?.trim() ?: "") + " " +
                    (poi.lowerAddrName?.trim() ?: "") + " " +
                    (poi.detailAddrName?.trim() ?: "") + " " +
                    poi.firstNo?.trim()
        } else {
            (poi.upperAddrName?.trim() ?: "") + " " +
                    (poi.middleAddrName?.trim() ?: "") + " " +
                    (poi.lowerAddrName?.trim() ?: "") + " " +
                    (poi.detailAddrName?.trim() ?: "") + " " +
                    (poi.firstNo?.trim() ?: "") + " " +
                    poi.secondNo?.trim()
        }


    private fun searchKeyword() {
        binding.searchButton.setOnClickListener {
            val keyword = binding.searchEditText.text.toString()

            launch(coroutineContext) {
                try {
                    withContext(Dispatchers.IO) {
                        val response = RetrofitUtil.apiService.getSearchLocation(
                            keyword = keyword
                        )
                        if (response.isSuccessful) {
                            val body = response.body()
                            withContext(Dispatchers.Main) {
                                body?.let { searchResponse ->
                                    setData(searchResponse.searchPoiInfo.pois)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {

                }
            }
        }
    }

}