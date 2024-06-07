package com.example.parkingmadrid

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.parkingmadrid.Clases.ApiClient.retrofit
import com.example.parkingmadrid.Clases.MadridAPI
import com.example.parkingmadrid.Clases.ParkingInfo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var madridAPI: MadridAPI
    private var dataList: List<ParkingInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        madridAPI = retrofit.create(MadridAPI::class.java)

        fetchData()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation()
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
        }
    }

    private fun fetchData() {
        val call = madridAPI.getParkingInfo("ES")
        call.enqueue(object : Callback<List<ParkingInfo>> {
            override fun onResponse(call: Call<List<ParkingInfo>>, response: Response<List<ParkingInfo>>) {
                if (response.isSuccessful) {
                    dataList = response.body() ?: emptyList()
                    addParkingMarkers(dataList)
                } else {
                    Toast.makeText(this@MapsActivity, "Error al obtener los datos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<ParkingInfo>>, t: Throwable) {
                Toast.makeText(this@MapsActivity, "Error en la solicitud: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addParkingMarkers(dataList: List<ParkingInfo>) {
        dataList.forEach { parking ->
            try {
                val latitude = parking.latitude.toDouble()
                val longitude = parking.longitude.toDouble()
                val parkingLatLng = LatLng(latitude, longitude)
                mMap.addMarker(MarkerOptions()
                    .position(parkingLatLng)
                    .title(parking.name)
                    .snippet("Sitios libres: ${parking.occupations?.firstOrNull()?.free ?: "N/A"}"))
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Error en los datos de latitud o longitud", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
