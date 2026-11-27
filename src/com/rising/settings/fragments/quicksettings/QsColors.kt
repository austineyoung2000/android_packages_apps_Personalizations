/*
 * Copyright (C) 2026 The RisingOS Android Project
 * Licensed under the Apache License, Version 2.0.
 */
package com.rising.settings.fragments.quicksettings

import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import androidx.preference.Preference
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class QsColors : SettingsPreferenceFragment() {
    companion object {
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.rising_settings_qs_colors)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listOf("active", "inactive").forEach { kind ->
            val resolver = requireContext().contentResolver
            val key = "qs_${kind}_background_effect"
            if (Settings.System.getIntForUser(resolver, key, -1, UserHandle.USER_CURRENT) == -1) {
                val enabled = Settings.System.getIntForUser(resolver, "qs_${kind}_background_color_enabled", 0, UserHandle.USER_CURRENT) != 0
                val gradient = Settings.System.getIntForUser(resolver, "qs_${kind}_gradient_enabled", 0, UserHandle.USER_CURRENT) != 0
                Settings.System.putIntForUser(resolver, key, if (!enabled) 0 else if (gradient) 2 else 1, UserHandle.USER_CURRENT)
            }
        }
        addPreferencesFromResource(R.xml.rising_settings_qs_colors)
        listOf("active", "inactive").forEach { kind ->
            findPreference<Preference>("qs_${kind}_background_effect")?.setOnPreferenceChangeListener { _, value ->
                updateTileVisibility(kind, value.toString().toIntOrNull() ?: 0)
                true
            }
        }
        findPreference<Preference>("qs_brightness_color_effect")?.setOnPreferenceChangeListener { _, value ->
            findPreference<Preference>("qs_brightness_glow_intensity")?.isVisible =
                (value.toString().toIntOrNull() ?: 0) in 1..2
            true
        }
        updateGlowVisibility()
    }

    override fun onResume() {
        super.onResume()
        updateGlowVisibility()
    }

    private fun updateGlowVisibility() {
        val brightnessEffect = Settings.System.getIntForUser(requireContext().contentResolver,
            "qs_brightness_color_effect", 0, UserHandle.USER_CURRENT)
        findPreference<Preference>("qs_brightness_glow_intensity")?.isVisible = brightnessEffect in 1..2
        listOf("active", "inactive").forEach { kind ->
            val effect = Settings.System.getIntForUser(requireContext().contentResolver,
                "qs_${kind}_background_effect", 0, UserHandle.USER_CURRENT)
            updateTileVisibility(kind, effect)
        }
    }

    private fun updateTileVisibility(kind: String, effect: Int) {
        findPreference<Preference>("qs_${kind}_glow_intensity")?.isVisible = effect in 5..6
        findPreference<Preference>("qs_${kind}_neon_transparency")?.isVisible = effect in 5..6
        listOf("frosted" to 7, "liquid" to 8).forEach { (glass, value) ->
            findPreference<Preference>("qs_${kind}_${glass}_glass_tint")?.isVisible = effect == value
            findPreference<Preference>("qs_${kind}_${glass}_glass_softness")?.isVisible = effect == value
        }
        findPreference<Preference>("qs_${kind}_background_color")?.isVisible = effect != 0
        findPreference<Preference>("qs_${kind}_gradient_end_color")?.isVisible = effect in listOf(2, 4, 6)
    }

    override fun getMetricsCategory(): Int = MetricsProto.MetricsEvent.VIEW_UNKNOWN
}
