package com.timetogo.app.ui.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.timetogo.app.databinding.FragmentDebugBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DebugFragment : Fragment() {

    private var _binding: FragmentDebugBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DebugViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDebugBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupListeners()
        observeState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupListeners() {
        binding.triggerNotificationButton.setOnClickListener {
            viewModel.triggerNotificationNow()
        }

        binding.clearLogsButton.setOnClickListener {
            viewModel.clearLogs()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.triggerNotificationButton.isEnabled = !state.isTriggering
                    binding.notificationProgress.isVisible = state.isTriggering

                    if (state.statusMessage.isNotEmpty()) {
                        binding.notificationStatusText.isVisible = true
                        binding.notificationStatusText.text = state.statusMessage
                    } else {
                        binding.notificationStatusText.isVisible = false
                    }

                    binding.logText.text = state.logs.ifEmpty { "No logs yet. Trigger a notification to see logs." }

                    binding.appStateText.text = state.appState

                    // Auto-scroll log to bottom
                    if (state.logs.isNotEmpty()) {
                        binding.logScrollView.post {
                            binding.logScrollView.fullScroll(View.FOCUS_DOWN)
                        }
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
