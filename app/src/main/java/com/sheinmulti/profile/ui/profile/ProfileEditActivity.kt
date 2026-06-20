package com.sheinmulti.profile.ui.profile

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sheinmulti.profile.R
import com.sheinmulti.profile.data.model.BrowserProfile
import com.sheinmulti.profile.databinding.ActivityProfileEditBinding
import com.sheinmulti.profile.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

class ProfileEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileEditBinding
    private lateinit var viewModel: ProfileViewModel
    private var editingProfileId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupToolbar()
        setupSpinner()
        setupListeners()

        editingProfileId = intent.getLongExtra("PROFILE_ID", -1)
        if (editingProfileId != -1L) {
            loadProfileData()
        } else {
            binding.etUserAgent.setText(getString(R.string.default_user_agent))
            binding.etStartUrl.setText("https://www.google.com")
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this, ProfileViewModel.Factory(application))[ProfileViewModel::class.java]
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (editingProfileId != -1L) "Editar Perfil" else "Nuevo Perfil"
    }

    private fun setupSpinner() {
        val proxyTypes = arrayOf("NONE", "HTTP", "SOCKS4", "SOCKS5")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, proxyTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerProxyType.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener { saveProfile() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun loadProfileData() {
        lifecycleScope.launch {
            val profile = viewModel.allProfiles.value?.find { it.id == editingProfileId }
            profile?.let {
                binding.etProfileName.setText(it.name)
                binding.etStartUrl.setText(it.startUrl)
                binding.etUserAgent.setText(it.userAgent)
                binding.etProxyHost.setText(it.proxyHost ?: "")
                binding.etProxyPort.setText(it.proxyPort?.toString() ?: "")
                binding.etProxyUsername.setText(it.proxyUsername ?: "")
                binding.etProxyPassword.setText(it.proxyPassword ?: "")
                val proxyTypes = arrayOf("NONE", "HTTP", "SOCKS4", "SOCKS5")
                val index = proxyTypes.indexOf(it.proxyType)
                if (index >= 0) binding.spinnerProxyType.setSelection(index)
            }
        }
    }

    private fun saveProfile() {
        val name = binding.etProfileName.text.toString().trim()
        val startUrl = binding.etStartUrl.text.toString().trim()
        val userAgent = binding.etUserAgent.text.toString().trim()
        val proxyHost = binding.etProxyHost.text.toString().trim().takeIf { it.isNotBlank() }
        val proxyPort = binding.etProxyPort.text.toString().trim().toIntOrNull()
        val proxyUsername = binding.etProxyUsername.text.toString().trim().takeIf { it.isNotBlank() }
        val proxyPassword = binding.etProxyPassword.text.toString().trim().takeIf { it.isNotBlank() }
        val proxyType = binding.spinnerProxyType.selectedItem.toString()

        if (name.isBlank()) {
            binding.etProfileName.error = "Nombre requerido"
            return
        }

        if (editingProfileId != -1L) {
            lifecycleScope.launch {
                val existing = viewModel.allProfiles.value?.find { it.id == editingProfileId }
                existing?.let {
                    val updated = it.copy(
                        name = name,
                        startUrl = startUrl.ifBlank { "https://www.google.com" },
                        userAgent = userAgent,
                        proxyHost = proxyHost,
                        proxyPort = proxyPort,
                        proxyUsername = proxyUsername,
                        proxyPassword = proxyPassword,
                        proxyType = proxyType
                    )
                    viewModel.updateProfile(updated)
                    Toast.makeText(this@ProfileEditActivity, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            viewModel.addProfile(
                name = name,
                startUrl = startUrl,
                userAgent = userAgent,
                proxyHost = proxyHost,
                proxyPort = proxyPort,
                proxyUsername = proxyUsername,
                proxyPassword = proxyPassword,
                proxyType = proxyType
            )
            Toast.makeText(this, "Perfil creado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
