/* Copyright (C) 2026 The RisingOS Android Project
 * Licensed under the Apache License, Version 2.0.
 */
package com.rising.settings.fragments.statusbar

import android.os.UserHandle
import android.provider.Settings
import androidx.preference.Preference
import com.android.settings.SettingsPreferenceFragment
import com.rising.settings.fragments.bindUnifiedStyle

internal class DynamicPopupPreferences(
    private val fragment: SettingsPreferenceFragment,
    private val prefix: String,
) {
    private fun pref(suffix: String): Preference? = fragment.findPreference(prefix + suffix)

    fun attach() {
        fragment.bindUnifiedStyle(prefix + "_popup_color_mode", prefix + "_popup_style",
            dualColorKey = prefix + "_popup_dual_color", outlineKey = prefix + "_popup_outline") { mode ->
            update(mode, readDual())
        }
        refresh()
    }

    private fun readMode() = Settings.System.getIntForUser(fragment.requireContext().contentResolver,
        prefix + "_popup_color_mode", 0, UserHandle.USER_CURRENT)
    private fun readDual() = Settings.System.getIntForUser(fragment.requireContext().contentResolver,
        prefix + "_popup_dual_color", 0, UserHandle.USER_CURRENT) != 0

    fun refresh() = update(readMode(), readDual())

    private fun update(mode: Int, dual: Boolean) {
        pref("_popup_first_color")?.isVisible = mode in 2..7
        pref("_popup_dual_color")?.isVisible = false
        pref("_popup_second_color")?.isVisible = mode == 4 || mode == 6 || mode == 7 || mode == 2 && dual
        pref("_popup_glow_intensity")?.isVisible = mode == 3 || mode == 4
        pref("_popup_neon_transparency")?.isVisible = mode == 3 || mode == 4
    }
}
