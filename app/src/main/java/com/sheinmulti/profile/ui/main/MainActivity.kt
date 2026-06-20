package com.sheinmulti.profile.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.sheinmulti.profile.adapter.ProfileAdapter
import com.sheinmulti.profile.data.model.BrowserProfile
import com.sheinmulti.profile.databinding.ActivityMainBinding
import com.sheinmulti.profile.ui.profile.ProfileEditActivity
import com.sheinmulti.profile.ui.webview.WebViewActivity
import com.sheinmulti.profile.viewmodel.ProfileViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ProfileViewModel
    private lateinit var adapter: ProfileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this, ProfileViewModel.Factory(application))[ProfileViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = ProfileAdapter(
            onProfileClick = { profile -> openProfile(profile) },
            onProfileEdit = { profile -> editProfile(profile) },
            onProfileDelete = { profile -> confirmDelete(profile) }
        )
        binding.recyclerViewProfiles.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewProfiles.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.allProfiles.observe(this) { profiles ->
            adapter.submitList(profiles)
            binding.tvEmpty.visibility = if (profiles.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
        viewModel.operationResult.observe(this) { message ->
            message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun setupListeners() {
        binding.fabAddProfile.setOnClickListener {
            startActivity(Intent(this, ProfileEditActivity::class.java))
        }
    }

    private fun openProfile(profile: BrowserProfile) {
        viewModel.activateProfile(profile.id)
        val intent = Intent(this, WebViewActivity::class.java).apply {
            putExtra("PROFILE_ID", profile.id)
            putExtra("PROFILE_NAME", profile.name)
            putExtra("PROFILE_SUFFIX", profile.dataDirectorySuffix)
            putExtra("START_URL", profile.startUrl)
            putExtra("USER_AGENT", profile.userAgent)
            putExtra("PROXY_HOST", profile.proxyHost)
            putExtra("PROXY_PORT", profile.proxyPort ?: -1)
            putExtra("PROXY_USERNAME", profile.proxyUsername)
            putExtra("PROXY_PASSWORD", profile.proxyPassword)
            putExtra("PROXY_TYPE", profile.proxyType)
        }
        startActivity(intent)
    }

    private fun editProfile(profile: BrowserProfile) {
        startActivity(Intent(this, ProfileEditActivity::class.java).apply {
            putExtra("PROFILE_ID", profile.id)
        })
    }

    private fun confirmDelete(profile: BrowserProfile) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Perfil")
            .setMessage("¿Eliminar ${profile.name}? Se perderan todas las cookies y sesiones.")
            .setPositiveButton("Eliminar") { _, _ -> viewModel.deleteProfile(profile) }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
