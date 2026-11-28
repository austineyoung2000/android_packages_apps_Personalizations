/*
 * Copyright (C) 2023 crDroid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rising.settings.fragments.lockscreen.doze

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast

import com.android.internal.logging.nano.MetricsProto

import androidx.preference.ListPreference
import androidx.preference.Preference

import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment
import com.android.settings.utils.ImageUtils

class AODSettings : OptimizedSettingsFragment() {

    companion object {
        private const val CUSTOM_IMAGE_REQUEST_CODE_KEY = "lockscreen_custom_image"
        private const val CUSTOM_IMAGE_REQUEST_CODE = 1001
    }

    private lateinit var mCustomImagePreference: Preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.rising_aod_settings)
        mCustomImagePreference = findCachedPreference(CUSTOM_IMAGE_REQUEST_CODE_KEY)!!
        val context = getSafeContext()
        val clockStyle = context?.let { ctx ->
            Settings.Secure.getIntForUser(ctx.contentResolver, "lock_screen_custom_clock_style", 0, UserHandle.USER_CURRENT) 
        } ?: 0
        val imagePath = Settings.System.getString(context?.contentResolver, "custom_aod_image_uri")
        if (imagePath != null && clockStyle.compareTo(0) > 0) {
            mCustomImagePreference.summary = imagePath
            mCustomImagePreference.isEnabled = true
        } else if (clockStyle == 0) {
            mCustomImagePreference.summary = context?.getString(R.string.custom_aod_image_not_supported)
            mCustomImagePreference.isEnabled = false
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateAmbientPeekIconPacks()
    }

    private fun updateAmbientPeekIconPacks() {
        val ctx = context ?: return
        val pref = findPreference<ListPreference>("ambient_peek_icon_pack_picker") ?: return
        val pm = ctx.packageManager
        val packs = linkedMapOf<String, String>()
        val actions = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.anddoes.launcher.THEME",
            "com.gau.go.launcherex.theme"
        )
        actions.forEach { action ->
            pm.queryIntentActivities(Intent(action), 0).forEach results@ { result ->
                val info = result.activityInfo?.applicationInfo ?: return@results
                packs[info.packageName] = info.loadLabel(pm).toString()
            }
        }
        val options = packs.entries.sortedBy { it.value.lowercase() }
        val saved = Settings.System.getStringForUser(
            ctx.contentResolver, "ambient_peek_icon_pack", UserHandle.USER_CURRENT
        ).orEmpty()
        val names = mutableListOf(getString(R.string.ambient_peek_icon_pack_default))
        val values = mutableListOf("")
        options.forEach { names.add(it.value); values.add(it.key) }
        if (saved.isNotEmpty() && saved !in values) {
            names.add(saved)
            values.add(saved)
        }
        pref.entries = names.toTypedArray()
        pref.entryValues = values.toTypedArray()
        pref.value = saved
        pref.summary = names[values.indexOf(saved)]
        pref.setOnPreferenceChangeListener { _, newValue ->
            val selected = newValue as String
            val success = Settings.System.putStringForUser(
                ctx.contentResolver, "ambient_peek_icon_pack",
                selected.ifEmpty { null }, UserHandle.USER_CURRENT
            )
            if (success) pref.summary = names[values.indexOf(selected)]
            success
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference == mCustomImagePreference) {
            try {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.type = "image/*"
                startActivityForResult(intent, CUSTOM_IMAGE_REQUEST_CODE)
            } catch (e: Exception) {
                Toast.makeText(context, R.string.qs_header_needs_gallery, Toast.LENGTH_LONG).show()
            }
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        super.onActivityResult(requestCode, resultCode, result)
        if (requestCode == CUSTOM_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && result != null) {
            val imgUri = result.data
            if (imgUri != null) {
                val savedImagePath = context?.let { ctx ->
                    ImageUtils.saveImageToInternalStorage(ctx, imgUri, "lockscreen_aod_image", "LOCKSCREEN_CUSTOM_AOD_IMAGE")
                }
                if (savedImagePath != null) {
                    val resolver = context?.contentResolver
                    Settings.System.putStringForUser(resolver, "custom_aod_image_uri", savedImagePath, UserHandle.USER_CURRENT)
                    mCustomImagePreference.summary = savedImagePath
                }
            }
        }
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
