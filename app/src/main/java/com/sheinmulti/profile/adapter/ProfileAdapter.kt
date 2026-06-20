package com.sheinmulti.profile.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sheinmulti.profile.data.model.BrowserProfile
import com.sheinmulti.profile.databinding.ItemProfileBinding

class ProfileAdapter(
    private val onProfileClick: (BrowserProfile) -> Unit,
    private val onProfileEdit: (BrowserProfile) -> Unit,
    private val onProfileDelete: (BrowserProfile) -> Unit
) : ListAdapter<BrowserProfile, ProfileAdapter.ProfileViewHolder>(ProfileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val binding = ItemProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProfileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProfileViewHolder(private val binding: ItemProfileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(profile: BrowserProfile) {
            binding.apply {
                tvProfileName.text = profile.name
                tvStartUrl.text = profile.startUrl
                tvUserAgent.text = profile.userAgent.take(50) + "..."
                tvProxyInfo.text = when {
                    profile.proxyHost != null && profile.proxyUsername != null ->
                        "Proxy: ${profile.proxyHost}:${profile.proxyPort} (${profile.proxyType}) [Auth]"
                    profile.proxyHost != null ->
                        "Proxy: ${profile.proxyHost}:${profile.proxyPort} (${profile.proxyType})"
                    else -> "Sin proxy"
                }
                tvStatus.text = if (profile.isActive) "● Activo" else "○ Inactivo"
                tvStatus.setTextColor(
                    if (profile.isActive) android.graphics.Color.parseColor("#4CAF50")
                    else android.graphics.Color.parseColor("#888888")
                )
                root.setOnClickListener { onProfileClick(profile) }
                btnEdit.setOnClickListener { onProfileEdit(profile) }
                btnDelete.setOnClickListener { onProfileDelete(profile) }
            }
        }
    }

    class ProfileDiffCallback : DiffUtil.ItemCallback<BrowserProfile>() {
        override fun areItemsTheSame(oldItem: BrowserProfile, newItem: BrowserProfile): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: BrowserProfile, newItem: BrowserProfile): Boolean {
            return oldItem == newItem
        }
    }
}
