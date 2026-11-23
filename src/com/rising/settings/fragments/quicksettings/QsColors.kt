/*
 * Copyright (C) 2026 The RisingOS Android Project
 * Licensed under the Apache License, Version 2.0.
 */
package com.rising.settings.fragments.quicksettings

import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
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
    }

    override fun getMetricsCategory(): Int = MetricsProto.MetricsEvent.VIEW_UNKNOWN
}
