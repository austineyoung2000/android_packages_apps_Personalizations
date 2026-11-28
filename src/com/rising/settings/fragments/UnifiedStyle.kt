/*
 * Copyright (C) 2026 The RisingOS Android Project
 * Licensed under the Apache License, Version 2.0.
 */
package com.rising.settings.fragments

import android.os.UserHandle
import android.provider.Settings
import androidx.preference.ListPreference
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

/** Presents existing style and effect settings as a single mutually exclusive choice. */
internal fun SettingsPreferenceFragment.bindUnifiedStyle(
    key: String,
    styleKey: String,
    defaultEffect: Int = 0,
    styleCount: Int = 7,
    defaultStyle: Int = 0,
    dualColorKey: String? = null,
    outlineKey: String? = null,
    onEffectChanged: (Int) -> Unit = {},
) {
    val pref = findPreference<ListPreference>(key) ?: return
    val resolver = requireContext().contentResolver
    if (!pref.extras.getBoolean("unified_style_entries")) {
        val names = resources.getStringArray(R.array.qs_compose_tile_style_entries)
        val oldEntries = pref.entries?.toList() ?: emptyList()
        val oldValues = pref.entryValues?.toList() ?: emptyList()
        pref.entries = (oldEntries + (1..styleCount).map { names[it] }).toTypedArray()
        pref.entryValues = (oldValues + (1..styleCount).map { (100 + it).toString() }).toTypedArray()
        pref.extras.putBoolean("unified_style_entries", true)
    }
    val effect = Settings.System.getIntForUser(resolver, key, defaultEffect, UserHandle.USER_CURRENT)
    var style = Settings.System.getIntForUser(resolver, styleKey, defaultStyle, UserHandle.USER_CURRENT)
    val dual = dualColorKey != null && Settings.System.getIntForUser(
        resolver, dualColorKey, 0, UserHandle.USER_CURRENT) != 0
    val outline = outlineKey != null && Settings.System.getIntForUser(
        resolver, outlineKey, 0, UserHandle.USER_CURRENT) != 0
    if (effect != defaultEffect && style != 0) {
        Settings.System.putIntForUser(resolver, styleKey, 0, UserHandle.USER_CURRENT)
        style = 0
    } else if (effect == defaultEffect && style == 0 && outline) {
        style = 1
        Settings.System.putIntForUser(resolver, styleKey, style, UserHandle.USER_CURRENT)
    }
    if (outline && outlineKey != null)
        Settings.System.putIntForUser(resolver, outlineKey, 0, UserHandle.USER_CURRENT)
    val visibleEffect = if (dualColorKey != null && effect == 2 && dual) 7 else effect
    pref.value = (if (effect == defaultEffect && style in 1..styleCount) 100 + style else visibleEffect).toString()
    pref.setOnPreferenceChangeListener { _, value ->
        val selection = value.toString().toIntOrNull() ?: defaultEffect
        val chosenStyle = if (selection in 101..(100 + styleCount)) selection - 100 else 0
        val chosenEffect = if (chosenStyle != 0) defaultEffect else if (dualColorKey != null && selection == 7) 2 else selection
        val chosenDual = dualColorKey != null && selection == 7
        // Clear the previous layer before enabling the newly selected one.
        val first = if (chosenStyle != 0)
            Settings.System.putIntForUser(resolver, key, defaultEffect, UserHandle.USER_CURRENT)
        else Settings.System.putIntForUser(resolver, styleKey, 0, UserHandle.USER_CURRENT)
        val second = if (!first) false else if (chosenStyle != 0)
            Settings.System.putIntForUser(resolver, styleKey, chosenStyle, UserHandle.USER_CURRENT)
        else Settings.System.putIntForUser(resolver, key, chosenEffect, UserHandle.USER_CURRENT)
        val third = if (first && second && dualColorKey != null)
            Settings.System.putIntForUser(resolver, dualColorKey, if (chosenDual) 1 else 0, UserHandle.USER_CURRENT)
        else first && second
        if (third && outlineKey != null)
            Settings.System.putIntForUser(resolver, outlineKey, 0, UserHandle.USER_CURRENT)
        if (third) onEffectChanged(if (chosenDual) 7 else chosenEffect)
        third
    }
}
