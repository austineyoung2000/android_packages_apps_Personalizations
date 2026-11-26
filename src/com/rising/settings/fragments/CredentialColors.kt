/* Copyright (C) 2026 The RisingOS Android Project
 * Licensed under the Apache License, Version 2.0.
 */
package com.rising.settings.fragments

import android.app.ActivityManager
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import androidx.preference.Preference
import com.android.internal.logging.nano.MetricsProto
import com.android.internal.widget.LockPatternUtils
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

class CredentialColors : SettingsPreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.rising_credential_colors)
        findPreference<Preference>("rising_pin_effect")?.setOnPreferenceChangeListener { _, value ->
            updateEffect(value.toString().toIntOrNull() ?: 0)
            true
        }
        findPreference<Preference>("rising_knock_effect")?.setOnPreferenceChangeListener { _, value ->
            updateKnockEffect(value.toString().toIntOrNull() ?: 0)
            true
        }
        findPreference<Preference>("rising_password_effect")?.setOnPreferenceChangeListener { _, value ->
            updatePasswordEffect(value.toString().toIntOrNull() ?: 0)
            true
        }
        findPreference<Preference>("rising_pattern_effect")?.setOnPreferenceChangeListener { _, value ->
            updatePatternEffect(value.toString().toIntOrNull() ?: 0)
            true
        }
    }
    override fun onResume() {
        super.onResume()
        val type = try { LockPatternUtils(requireContext()).getCredentialTypeForUser(ActivityManager.getCurrentUser()) }
            catch (_: RuntimeException) { LockPatternUtils.CREDENTIAL_TYPE_UNKNOWN }
        val isKnock = type == LockPatternUtils.CREDENTIAL_TYPE_PIN && try {
            LockPatternUtils(requireContext()).isKnockCodeEnabled(ActivityManager.getCurrentUser())
        } catch (_: RuntimeException) { false }
        val isPin = type == LockPatternUtils.CREDENTIAL_TYPE_PIN && !isKnock
        val isPattern = type == LockPatternUtils.CREDENTIAL_TYPE_PATTERN
        val isPassword = type == LockPatternUtils.CREDENTIAL_TYPE_PASSWORD
        val title = if (isKnock) R.string.credential_current_knock else when (type) {
            LockPatternUtils.CREDENTIAL_TYPE_PIN -> R.string.credential_current_pin
            LockPatternUtils.CREDENTIAL_TYPE_PATTERN -> R.string.credential_current_pattern
            LockPatternUtils.CREDENTIAL_TYPE_PASSWORD -> R.string.credential_current_password
            LockPatternUtils.CREDENTIAL_TYPE_NONE -> R.string.credential_current_none
            else -> R.string.credential_current_unknown
        }
        findPreference<Preference>("credential_status")?.apply {
            setTitle(title)
            summary = if (isPin || isPattern || isKnock || isPassword) null else getString(R.string.credential_pattern_unavailable)
        }
        findPreference<Preference>("credential_pin_options")?.isVisible = isPin
        findPreference<Preference>("credential_pattern_options")?.isVisible = isPattern
        findPreference<Preference>("credential_knock_options")?.isVisible = isKnock
        findPreference<Preference>("credential_password_options")?.isVisible = isPassword
        val effect = Settings.System.getIntForUser(requireContext().contentResolver,
            "rising_pin_effect", 0, UserHandle.USER_CURRENT)
        updateEffect(effect)
        val patternEffect = Settings.System.getIntForUser(requireContext().contentResolver,
            "rising_pattern_effect", 0, UserHandle.USER_CURRENT)
        updatePatternEffect(patternEffect)
        updateKnockEffect(Settings.System.getIntForUser(requireContext().contentResolver,
            "rising_knock_effect", 0, UserHandle.USER_CURRENT))
        updatePasswordEffect(Settings.System.getIntForUser(requireContext().contentResolver,
            "rising_password_effect", 0, UserHandle.USER_CURRENT))
    }
    private fun updatePasswordEffect(effect: Int) {
        findPreference<Preference>("rising_password_first_color")?.isVisible = effect != 0
        findPreference<Preference>("rising_password_second_color")?.isVisible = effect == 2 || effect == 4
        findPreference<Preference>("rising_password_glow_intensity")?.isVisible = effect == 3 || effect == 4
    }
    private fun updateKnockEffect(effect: Int) {
        findPreference<Preference>("rising_knock_first_color")?.isVisible = effect != 0
        findPreference<Preference>("rising_knock_second_color")?.isVisible = effect == 2 || effect == 4
        findPreference<Preference>("rising_knock_glow_intensity")?.isVisible = effect == 3 || effect == 4
    }
    private fun updatePatternEffect(effect: Int) {
        findPreference<Preference>("rising_pattern_first_color")?.isVisible = effect != 0
        findPreference<Preference>("rising_pattern_second_color")?.isVisible = effect == 2 || effect == 4
        findPreference<Preference>("rising_pattern_glow_intensity")?.isVisible = effect == 3 || effect == 4
    }
    private fun updateEffect(effect: Int) {
        listOf("rising_pin_shape", "rising_pin_first_color", "rising_pin_digit_color").forEach {
            findPreference<Preference>(it)?.isVisible = effect != 0
        }
        findPreference<Preference>("rising_pin_second_color")?.isVisible = effect == 2 || effect == 4
        findPreference<Preference>("rising_pin_glow_intensity")?.isVisible = effect == 3 || effect == 4
    }
    override fun getMetricsCategory(): Int = MetricsProto.MetricsEvent.VIEW_UNKNOWN
}
