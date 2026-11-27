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
import com.rising.settings.fragments.bindUnifiedStyle
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
        val resolver = requireContext().contentResolver
        val oldStyle = Settings.System.getIntForUser(resolver, "qs_compose_tile_style", 0, UserHandle.USER_CURRENT)
        listOf("active", "inactive").forEach { kind ->
            val key = "qs_${kind}_background_effect"
            var effect = Settings.System.getIntForUser(resolver, key, -1, UserHandle.USER_CURRENT)
            if (effect == -1) {
                val enabled = Settings.System.getIntForUser(resolver, "qs_${kind}_background_color_enabled", 0, UserHandle.USER_CURRENT) != 0
                val gradient = Settings.System.getIntForUser(resolver, "qs_${kind}_gradient_enabled", 0, UserHandle.USER_CURRENT) != 0
                effect = if (!enabled) 0 else if (gradient) 2 else 1
            }
            if (effect == 0 && oldStyle in 1..7) effect = oldStyle + 8
            Settings.System.putIntForUser(resolver, key, effect, UserHandle.USER_CURRENT)
        }
        if (oldStyle != 0) Settings.System.putIntForUser(resolver,
            "qs_compose_tile_style", 0, UserHandle.USER_CURRENT)
        val brightnessEffect = Settings.System.getIntForUser(resolver, "qs_brightness_color_effect", 0, UserHandle.USER_CURRENT)
        val brightnessGradient = Settings.System.getIntForUser(resolver, "qs_brightness_gradient_enabled", 0, UserHandle.USER_CURRENT) != 0
        val brightnessColor = Settings.System.getIntForUser(resolver, "qs_brightness_color_enabled", 0, UserHandle.USER_CURRENT) != 0
        if (brightnessEffect == 0 && brightnessGradient && brightnessColor) {
            Settings.System.putIntForUser(resolver, "qs_brightness_color_effect", 3, UserHandle.USER_CURRENT)
        }
        if (brightnessGradient) Settings.System.putIntForUser(resolver, "qs_brightness_gradient_enabled", 0, UserHandle.USER_CURRENT)
        addPreferencesFromResource(R.xml.rising_settings_qs_colors)
        listOf("active", "inactive").forEach { kind ->
            findPreference<Preference>("qs_${kind}_background_effect")?.setOnPreferenceChangeListener { _, value ->
                updateTileVisibility(kind, value.toString().toIntOrNull() ?: 0)
                true
            }
        }
        bindUnifiedStyle("qs_brightness_color_effect", "qs_brightness_style") { effect ->
            findPreference<Preference>("qs_brightness_glow_intensity")?.isVisible = effect in 1..2
            findPreference<Preference>("qs_brightness_neon_transparency")?.isVisible = effect in 1..2
            findPreference<Preference>("qs_brightness_gradient_end_color")?.isVisible = effect == 2 || effect == 3
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
        findPreference<Preference>("qs_brightness_neon_transparency")?.isVisible = brightnessEffect in 1..2
        findPreference<Preference>("qs_brightness_gradient_end_color")?.isVisible = brightnessEffect == 2 || brightnessEffect == 3
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
            findPreference<Preference>("qs_${kind}_${glass}_glass_transparency")?.isVisible = effect == value
        }
        findPreference<Preference>("qs_${kind}_background_color")?.isVisible = effect in 1..8
        findPreference<Preference>("qs_${kind}_gradient_end_color")?.isVisible = effect in listOf(2, 4, 6)
    }

    override fun getMetricsCategory(): Int = MetricsProto.MetricsEvent.VIEW_UNKNOWN
}
