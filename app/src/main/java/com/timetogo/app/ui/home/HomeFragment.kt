package com.timetogo.app.ui.home

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.timetogo.app.BuildConfig
import com.timetogo.app.R
import com.timetogo.app.databinding.FragmentHomeBinding
import com.timetogo.app.util.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.timetogo.app.ui.map.MapSelectionActivity

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Snackbar.make(
                binding.root,
                "Location and notification permissions are needed for route notifications.",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    // Places Autocomplete launcher
    // BILLABLE API CALL: Place Autocomplete sessions are billed per session.
    private val autocompleteResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.let { data ->
                    val place = Autocomplete.getPlaceFromIntent(data)
                    val address = place.formattedAddress ?: place.name ?: ""
                    val latLng = place.latLng
                    if (latLng != null) {
                        viewModel.setHomeAddress(address, latLng.latitude, latLng.longitude)
                    }
                }
            }
            AutocompleteActivity.RESULT_ERROR -> {
                result.data?.let { data ->
                    val status = Autocomplete.getStatusFromIntent(data)
                    Snackbar.make(
                        binding.root,
                        "Address search error: ${status.statusMessage}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Map Selection launcher
    private val mapSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val address = data.getStringExtra("EXTRA_ADDRESS_NAME") ?: ""
                val lat = data.getDoubleExtra("EXTRA_LATITUDE", 0.0)
                val lng = data.getDoubleExtra("EXTRA_LONGITUDE", 0.0)
                if (lat != 0.0 && lng != 0.0) {
                    viewModel.setHomeAddress(address, lat, lng)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Places SDK if not already initialized
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(
                requireContext(),
                BuildConfig.MAPS_API_KEY
            )
        }

        setupToolbar()
        setupListeners()
        requestPermissions()
        observeState()
    }

    private fun setupToolbar() {
        binding.toolbar.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.add(Menu.NONE, R.id.settingsFragment, Menu.NONE, R.string.settings)
                    .setIcon(R.drawable.ic_settings)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.settingsFragment -> {
                        findNavController().navigate(R.id.action_home_to_settings)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner)
    }

    private fun setupListeners() {
        // Address input — launch Places Autocomplete
        binding.addressEditText.setOnClickListener {
            launchPlacesAutocomplete()
        }

        // Map selection button
        binding.mapSelectionButton.setOnClickListener {
            val intent = android.content.Intent(requireContext(), MapSelectionActivity::class.java)
            mapSelectionLauncher.launch(intent)
        }

        // Clear address
        binding.clearAddressButton.setOnClickListener {
            viewModel.clearHomeAddress()
        }

        // Change time
        binding.changeTimeButton.setOnClickListener {
            showTimePicker()
        }

        // Alarm switch
        binding.alarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            val state = viewModel.uiState.value
            if (isChecked && !state.hasAddress) {
                binding.alarmSwitch.isChecked = false
                Snackbar.make(binding.root, R.string.set_address_first, Snackbar.LENGTH_LONG).show()
                return@setOnCheckedChangeListener
            }

            if (isChecked && !viewModel.canScheduleExactAlarms()) {
                binding.alarmSwitch.isChecked = false
                showExactAlarmPermissionDialog()
                return@setOnCheckedChangeListener
            }

            if (isChecked && !PermissionHelper.hasBackgroundLocationPermission(requireContext())) {
                binding.alarmSwitch.isChecked = false
                showBackgroundLocationDialog()
                return@setOnCheckedChangeListener
            }

            viewModel.setAlarmEnabled(isChecked)
        }

        // Alarm mode radio group
        binding.alarmModeGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.setRecurring(checkedId == R.id.radio_recurring)
        }

        // Test notification
        binding.testNotificationButton.setOnClickListener {
            viewModel.triggerNotificationNow()
        }
    }

    private fun requestPermissions() {
        val missingPermissions = PermissionHelper.REQUIRED_PERMISSIONS.filter { permission ->
            !PermissionHelper.hasNotificationPermission(requireContext()) ||
            !PermissionHelper.hasAnyLocationPermission(requireContext())
        }
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(PermissionHelper.REQUIRED_PERMISSIONS)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.greetingText.text = getString(R.string.greeting, state.userName.ifEmpty { "there" })
                    binding.addressEditText.setText(state.homeAddress.ifEmpty { "" })
                    binding.clearAddressButton.isVisible = state.hasAddress
                    binding.alarmTimeText.text = String.format("%02d:%02d", state.alarmHour, state.alarmMinute)

                    // Update switch without triggering listener
                    binding.alarmSwitch.setOnCheckedChangeListener(null)
                    binding.alarmSwitch.isChecked = state.alarmEnabled
                    binding.alarmSwitch.setOnCheckedChangeListener { _, isChecked ->
                        val currentState = viewModel.uiState.value
                        if (isChecked && !currentState.hasAddress) {
                            binding.alarmSwitch.isChecked = false
                            Snackbar.make(binding.root, R.string.set_address_first, Snackbar.LENGTH_LONG).show()
                            return@setOnCheckedChangeListener
                        }
                        if (isChecked && !viewModel.canScheduleExactAlarms()) {
                            binding.alarmSwitch.isChecked = false
                            showExactAlarmPermissionDialog()
                            return@setOnCheckedChangeListener
                        }
                        if (isChecked && !PermissionHelper.hasBackgroundLocationPermission(requireContext())) {
                            binding.alarmSwitch.isChecked = false
                            showBackgroundLocationDialog()
                            return@setOnCheckedChangeListener
                        }
                        viewModel.setAlarmEnabled(isChecked)
                    }

                    // Alarm mode — detach listener to avoid feedback loop
                    binding.alarmModeGroup.setOnCheckedChangeListener(null)
                    if (state.isRecurring) {
                        binding.radioRecurring.isChecked = true
                    } else {
                        binding.radioOneShot.isChecked = true
                    }
                    binding.alarmModeGroup.setOnCheckedChangeListener { _, checkedId ->
                        viewModel.setRecurring(checkedId == R.id.radio_recurring)
                    }

                    // Status
                    binding.statusText.text = state.statusText
                    if (state.alarmEnabled) {
                        binding.statusCard.setCardBackgroundColor(
                            com.google.android.material.color.MaterialColors.getColor(
                                binding.statusCard, R.attr.statusActiveColor
                            )
                        )
                    } else {
                        binding.statusCard.setCardBackgroundColor(
                            com.google.android.material.color.MaterialColors.getColor(
                                binding.statusCard, R.attr.statusInactiveColor
                            )
                        )
                    }

                    // Error banner
                    binding.errorBanner.isVisible = state.lastFetchFailed
                    if (state.lastFetchFailed) {
                        binding.errorBannerText.text = getString(R.string.last_fetch_failed)
                    }

                    // Test notification
                    binding.testNotificationButton.isEnabled = !state.isTriggering
                    binding.testNotificationProgress.isVisible = state.isTriggering
                    if (state.triggerStatusMessage.isNotEmpty()) {
                        binding.testNotificationStatusText.isVisible = true
                        binding.testNotificationStatusText.text = state.triggerStatusMessage
                    } else {
                        binding.testNotificationStatusText.isVisible = false
                    }
                }
            }
        }
    }

    private fun launchPlacesAutocomplete() {
        val fields = listOf(
            Place.Field.ID,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.NAME,
            Place.Field.LOCATION
        )

        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
            .build(requireContext())

        autocompleteResultLauncher.launch(intent)
    }

    private fun showTimePicker() {
        val state = viewModel.uiState.value
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(state.alarmHour)
            .setMinute(state.alarmMinute)
            .setTitleText("Set notification time")
            .build()

        picker.addOnPositiveButtonClickListener {
            viewModel.setAlarmTime(picker.hour, picker.minute)
        }

        picker.show(childFragmentManager, "time_picker")
    }

    private fun showBackgroundLocationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Background Location Required")
            .setMessage("Time to Go needs \"Allow all the time\" location access to determine your position when the alarm fires while the app is closed.\n\nPlease select \"Allow all the time\" in the next screen.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(PermissionHelper.getBackgroundLocationSettingsIntent(requireContext()))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showExactAlarmPermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Exact Alarm Permission")
            .setMessage("Time to Go needs permission to schedule exact alarms to notify you at the right time every day. Please grant this permission in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(PermissionHelper.getExactAlarmSettingsIntent(requireContext()))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
