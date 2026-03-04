package com.timetogo.app.ui.map

import android.app.Activity
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.timetogo.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

class MapSelectionActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var selectButton: ExtendedFloatingActionButton
    private lateinit var addressTextView: TextView
    private lateinit var geocoder: Geocoder
    
    // Default location (e.g. Lisbon)
    private val defaultLocation = LatLng(38.7223, -9.1393)
    private var currentLatLng = defaultLocation
    private var currentAddressName: String? = null
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge to edge support for nicer maps look
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContentView(R.layout.activity_map_selection)

        selectButton = findViewById(R.id.select_location_button)
        addressTextView = findViewById(R.id.address_text)
        geocoder = Geocoder(this, Locale.getDefault())

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        selectButton.setOnClickListener {
            val intent = Intent().apply {
                putExtra("EXTRA_ADDRESS_NAME", currentAddressName ?: "Selected Location")
                putExtra("EXTRA_LATITUDE", currentLatLng.latitude)
                putExtra("EXTRA_LONGITUDE", currentLatLng.longitude)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
        
        selectButton.isEnabled = false
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        
        // Move camera to default location initially
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))

        map.setOnCameraIdleListener {
            val position: CameraPosition = map.cameraPosition
            currentLatLng = position.target
            updateAddressForLocation(currentLatLng)
        }
        
        map.setOnCameraMoveStartedListener {
            addressTextView.text = "Searching..."
            selectButton.isEnabled = false
        }
    }

    private fun updateAddressForLocation(latLng: LatLng) {
        scope.launch {
            val address = geocodeLocation(latLng)
            withContext(Dispatchers.Main) {
                if (address != null) {
                    val addressText = address.getAddressLine(0)
                    currentAddressName = addressText
                    addressTextView.text = addressText
                    selectButton.isEnabled = true
                } else {
                    currentAddressName = "${latLng.latitude}, ${latLng.longitude}"
                    addressTextView.text = "Unknown location\n${latLng.latitude}, ${latLng.longitude}"
                    selectButton.isEnabled = true
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun geocodeLocation(latLng: LatLng): Address? = withContext(Dispatchers.IO) {
        try {
            // Android 13+ has a new Geocoder API, but for compatibility we use the blocking one in a background thread
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                return@withContext addresses[0]
            }
        } catch (e: IOException) {
            Log.e("MapSelection", "Geocoding error", e)
        }
        return@withContext null
    }
}
