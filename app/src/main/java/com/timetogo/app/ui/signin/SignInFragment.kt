package com.timetogo.app.ui.signin

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.timetogo.app.R
import com.timetogo.app.databinding.FragmentSignInBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignInViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if already signed in
        viewModel.checkExistingSignIn()

        binding.signInButton.setOnClickListener {
            viewModel.signIn(requireActivity())
        }

        binding.skipSignInButton.setOnClickListener {
            viewModel.skipSignIn()
        }

        // Observe UI state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.signInButton.isEnabled = !state.isLoading
                    binding.skipSignInButton.isEnabled = !state.isLoading
                    binding.loadingIndicator.isVisible = state.isLoading

                    if (state.isSignedIn) {
                        findNavController().navigate(R.id.action_signIn_to_home)
                    }

                    if (state.noAccountFound) {
                        showAddAccountDialog()
                        viewModel.clearError()
                    } else {
                        state.errorMessage?.let { error ->
                            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun showAddAccountDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Google Account Required")
            .setMessage("No Google account was found on this device. Would you like to add one now?")
            .setPositiveButton("Add Account") { _, _ ->
                val intent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
                    putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
