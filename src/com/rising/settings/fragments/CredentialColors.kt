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
    }
    override fun onResume() {
        super.onResume()
        val type = try { LockPatternUtils(requireContext()).getCredentialTypeForUser(ActivityManager.getCurrentUser()) }
            catch (_: RuntimeException) { LockPatternUtils.CREDENTIAL_TYPE_UNKNOWN }
        val isPin = type == LockPatternUtils.CREDENTIAL_TYPE_PIN
        val title = when (type) {
            LockPatternUtils.CREDENTIAL_TYPE_PIN -> R.string.credential_current_pin
            LockPatternUtils.CREDENTIAL_TYPE_PATTERN -> R.string.credential_current_pattern
            LockPatternUtils.CREDENTIAL_TYPE_PASSWORD -> R.string.credential_current_password
            LockPatternUtils.CREDENTIAL_TYPE_NONE -> R.string.credential_current_none
            else -> R.string.credential_current_unknown
        }
        findPreference<Preference>("credential_status")?.apply {
            setTitle(title)
            summary = if (isPin) null else getString(R.string.credential_pin_unavailable)
        }
        findPreference<Preference>("credential_pin_options")?.isVisible = isPin
        val effect = Settings.System.getIntForUser(requireContext().contentResolver,
            "rising_pin_effect", 0, UserHandle.USER_CURRENT)
        updateEffect(effect)
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
