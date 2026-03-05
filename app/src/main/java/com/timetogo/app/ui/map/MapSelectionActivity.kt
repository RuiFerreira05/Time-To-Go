package com.timetogo.app.ui.map

import android.app.Activity
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.view.inputmethod.InputMethodManager
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.timetogo.app.BuildConfig
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
    
    // Search properties
    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlacePredictionAdapter
    private lateinit var placesClient: PlacesClient
    private var sessionToken: AutocompleteSessionToken? = null
    private var isProgrammaticTextChange = false
    
    // Insets handling to not consume them more than once dynamically
    private var isSearchCardInsetApplied = false
    private var isSelectButtonInsetApplied = false

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge to edge support for nicer maps look
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContentView(R.layout.activity_map_selection)

        selectButton = findViewById(R.id.select_location_button)
        searchEditText = findViewById(R.id.search_edit_text)
        val searchCard: View = findViewById(R.id.search_card)
        recyclerView = findViewById(R.id.recycler_view_predictions)
        geocoder = Geocoder(this, Locale.getDefault())
        
        // Handle window insets so UI doesn't overlap with system bars
        ViewCompat.setOnApplyWindowInsetsListener(searchCard) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            if (!isSearchCardInsetApplied) {
                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin += insets.top
                }
                isSearchCardInsetApplied = true
            }
            windowInsets
        }

        ViewCompat.setOnApplyWindowInsetsListener(selectButton) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            if (!isSelectButtonInsetApplied) {
                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin += insets.bottom
                }
                isSelectButtonInsetApplied = true
            }
            windowInsets
        }
        
        // Initialize Places logic
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(this, BuildConfig.MAPS_API_KEY)
        }
        placesClient = Places.createClient(this)
        sessionToken = AutocompleteSessionToken.newInstance()
        
        adapter = PlacePredictionAdapter { prediction ->
            handlePredictionSelection(prediction)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        setupSearchEditText()

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
        
        map.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                isProgrammaticTextChange = true
                searchEditText.setText("Searching...")
                isProgrammaticTextChange = false
                selectButton.isEnabled = false
                
                // Hide search list if user interacts with map
                if (recyclerView.visibility == View.VISIBLE) {
                    recyclerView.visibility = View.GONE
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
                    searchEditText.clearFocus()
                }
            }
        }
    }

    private fun setupSearchEditText() {
        searchEditText.setOnClickListener {
            searchEditText.selectAll()
        }
        
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isProgrammaticTextChange) return
                
                val query = s?.toString() ?: ""
                if (query.length > 2) {
                    performSearch(query)
                } else {
                    recyclerView.visibility = View.GONE
                    adapter.submitList(emptyList())
                }
            }
        })
    }

    private fun performSearch(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(sessionToken)
            .setQuery(query)
            .build()
            
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val predictions = response.autocompletePredictions
                if (predictions.isNotEmpty()) {
                    adapter.submitList(predictions)
                    recyclerView.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                Log.e("MapSelectionActivity", "Error finding predictions", it)
            }
    }

    private fun handlePredictionSelection(prediction: com.google.android.libraries.places.api.model.AutocompletePrediction) {
        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
        
        recyclerView.visibility = View.GONE
        
        // Set text without triggering search
        isProgrammaticTextChange = true
        searchEditText.setText(prediction.getPrimaryText(null))
        searchEditText.setSelection(searchEditText.text.length)
        isProgrammaticTextChange = false
        
        // Fetch LatLng
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LOCATION)
        val request = FetchPlaceRequest.newInstance(prediction.placeId, placeFields)
        
        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                place.latLng?.let { latLng ->
                    currentAddressName = place.name ?: prediction.getPrimaryText(null).toString()
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    // A new search session starts after a selection
                    sessionToken = AutocompleteSessionToken.newInstance()
                }
            }
            .addOnFailureListener {
                Log.e("MapSelectionActivity", "Error fetching place details", it)
            }
    }

    private fun updateAddressForLocation(latLng: LatLng) {
        scope.launch {
            val address = geocodeLocation(latLng)
            withContext(Dispatchers.Main) {
                if (address != null) {
                    val addressText = address.getAddressLine(0)
                    currentAddressName = addressText
                    isProgrammaticTextChange = true
                    searchEditText.setText(addressText)
                    isProgrammaticTextChange = false
                    selectButton.isEnabled = true
                } else {
                    currentAddressName = "${latLng.latitude}, ${latLng.longitude}"
                    isProgrammaticTextChange = true
                    searchEditText.setText("${latLng.latitude}, ${latLng.longitude}")
                    isProgrammaticTextChange = false
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
