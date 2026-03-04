package com.timetogo.app.ui.settings

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.timetogo.app.BuildConfig
import com.timetogo.app.R
import com.timetogo.app.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        // Apply saved state before attaching listeners so the UI is correct from
        // the first frame and the listener doesn't fire on the initial update.
        applyInitialState()
        setupListeners()
        observeState()

        // Set version text
        binding.versionText.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)

        // Set attribution text
        binding.attributionText.text = Html.fromHtml(
            getString(R.string.google_maps_attribution),
            Html.FROM_HTML_MODE_COMPACT
        )
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    /** Set radio / preview from the current ViewModel snapshot so the
     *  correct option is visible before the first emission arrives. */
    private fun applyInitialState() {
        val state = viewModel.uiState.value
        if (state.isDetailedNotification) {
            binding.radioDetailed.isChecked = true
            binding.previewText.text = getString(R.string.detailed_preview)
        } else {
            binding.radioBrief.isChecked = true
            binding.previewText.text = getString(R.string.brief_preview)
        }
    }

    private val notificationModeListener =
        android.widget.RadioGroup.OnCheckedChangeListener { _, checkedId ->
            val detailed = checkedId == R.id.radio_detailed
            viewModel.setDetailedNotification(detailed)
            binding.previewText.text = if (detailed) {
                getString(R.string.detailed_preview)
            } else {
                getString(R.string.brief_preview)
            }
        }

    private fun setupListeners() {
        // Notification mode
        binding.notificationModeGroup.setOnCheckedChangeListener(notificationModeListener)

        // Sign out
        binding.signOutButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sign Out")
                .setMessage(R.string.sign_out_confirm)
                .setPositiveButton(R.string.yes) { _, _ ->
                    viewModel.signOut()
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }

        // Debug menu
        binding.debugButton.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_debug)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect { state ->
                    // Detach listener to avoid feedback loop / flicker
                    binding.notificationModeGroup.setOnCheckedChangeListener(null)

                    // Notification mode
                    if (state.isDetailedNotification) {
                        binding.radioDetailed.isChecked = true
                        binding.previewText.text = getString(R.string.detailed_preview)
                    } else {
                        binding.radioBrief.isChecked = true
                        binding.previewText.text = getString(R.string.brief_preview)
                    }
                    // Skip any pending check-mark animation so the toggle appears instant
                    binding.radioDetailed.jumpDrawablesToCurrentState()
                    binding.radioBrief.jumpDrawablesToCurrentState()

                    // Re-attach listener
                    binding.notificationModeGroup.setOnCheckedChangeListener(notificationModeListener)

                    // Account info
                    binding.accountName.text = state.userName
                    binding.accountEmail.text = state.userEmail

                    // Sign out navigation
                    if (state.isSignedOut) {
                        findNavController().navigate(R.id.action_settings_to_signIn)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
