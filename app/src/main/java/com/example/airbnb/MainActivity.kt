package com.example.airbnb

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.util.FusedLocationSource
import com.naver.maps.map.util.MarkerIcons
import com.naver.maps.map.widget.LocationButtonView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : AppCompatActivity(), OnMapReadyCallback, Overlay.OnClickListener {

    private val mapView: MapView by lazy {
        findViewById(R.id.mapView)
    }
    private lateinit var naverMap: NaverMap
    private lateinit var locationSource: FusedLocationSource
    private val viewPager: ViewPager2 by lazy {
        findViewById(R.id.houseViewPager)
    }
    private val viewPagerAdapter = HouseViewPagerAdapter(itemClicked = {
        val intent = Intent()
            .apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "[지금 이 가격에 예약하세요!!] ${it.title} ${it.price}")
                type = "text/plain"
            }

        startActivity(Intent.createChooser(intent, null))
    })
    private val recyclerView: RecyclerView by lazy {
        findViewById(R.id.recyclerView)
    }
    private val recyclerAdapter = HouseListAdapter()
    private val currentLocationButton: LocationButtonView by lazy {
        findViewById(R.id.currentLocationButton)
    }
    private val bottomSheetTitleTextView: TextView by lazy {
        findViewById(R.id.bottomSheetTitleTextView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync(this)

        viewPager.adapter = viewPagerAdapter
        recyclerView.adapter = recyclerAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val selectedHouse = viewPagerAdapter.currentList[position]
                val cameraUpdate = CameraUpdate.scrollTo(LatLng(selectedHouse.lat, selectedHouse.lng))
                    .animate(CameraAnimation.Easing)

                naverMap.moveCamera(cameraUpdate)

            }
        })



    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map

        naverMap.maxZoom = 18.0
        naverMap.minZoom = 10.0

        val cameraUpdate = CameraUpdate.scrollTo(LatLng(37.496326, 126.736325))
        naverMap.moveCamera(cameraUpdate)

        val uiSetting = naverMap.uiSettings
        uiSetting.isLocationButtonEnabled = false

        currentLocationButton.map = naverMap

        locationSource = FusedLocationSource(this@MainActivity, LOCATION_PERMISSION_REQUEST_CODE)
        naverMap.locationSource = locationSource

        getHouseListFromAPI()

    }

    private fun getHouseListFromAPI() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://run.mocky.io")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val houseService = retrofit.create(HouseService::class.java)

        houseService.getHouseList()
            .enqueue(object: Callback<HouseDto> {
                @SuppressLint("SetTextI18n")
                override fun onResponse(call: Call<HouseDto>, response: Response<HouseDto>) {
                    if (response.isSuccessful.not()) {
                        return
                    }
                    response.body()?.let { dto ->
                        updateMarker(dto.items)
                        viewPagerAdapter.submitList(dto.items)
                        recyclerAdapter.submitList(dto.items)
                        bottomSheetTitleTextView.text = "${dto.items.size}개의 숙소"
                    }
                }
                override fun onFailure(call: Call<HouseDto>, t: Throwable) {
                    Log.d("retrofit", t.toString())
                }
            })
    }

    private fun updateMarker(houses: List<HouseModel>) {
        houses.forEach { house ->
            val marker = Marker()
            marker.position = LatLng(house.lat, house.lng)
            marker.onClickListener = this
            marker.map = naverMap
            marker.tag = house.id
            marker.icon = MarkerIcons.BLACK
            marker.iconTintColor = Color.RED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return
        }

        if (locationSource.onRequestPermissionsResult(requestCode,permissions, grantResults)) {
            if (!locationSource.isActivated) {
                naverMap.locationTrackingMode = LocationTrackingMode.None
            }
            return
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }
    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }
    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    override fun onClick(overly: Overlay): Boolean {
        //overly.tag

        val selectedModel = viewPagerAdapter.currentList.firstOrNull {
            it.id == overly.tag
        }
        selectedModel?.let {
            val position = viewPagerAdapter.currentList.indexOf(it)
            viewPager.currentItem = position
        }
        return true
    }
}