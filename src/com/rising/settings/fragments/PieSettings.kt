/*
 * SPDX-FileCopyrightText: The Resurrected Remix Project
 * SPDX-FileCopyrightText: The RisingOS Android Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.rising.settings.fragments

import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider

/** Pie slice order and configuration, adapted from Resurrected Remix PieSettings. */
class PieSettings : SettingsPreferenceFragment() {
    companion object {
        private const val ITEMS = "pie_items"
        private const val DEFAULT_ITEMS = "back,home,recents,notifications,flashlight,screenshot,power"
        private const val NONE = "none"
        private const val SLOT_COUNT = 9
        @JvmField val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.rising_pie_settings)
    }

    private val slots = mutableListOf<ListPreference>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.rising_pie_settings)
        val category = findPreference<PreferenceCategory>("pie_slices_category") ?: return
        val entries = resources.getStringArray(R.array.rising_pie_slice_entries)
        val values = resources.getStringArray(R.array.rising_pie_slice_values)
        repeat(SLOT_COUNT) { index ->
            val pref = ListPreference(requireContext()).apply {
                key = "pie_slice_slot_$index"
                title = getString(R.string.rising_pie_slice_slot, index + 1)
                this.entries = entries
                entryValues = values
                isPersistent = false
            }
            category.addPreference(pref)
            pref.dependency = "pie_controls"
            slots.add(pref)
            pref.setOnPreferenceChangeListener { _, newValue ->
                val action = newValue.toString()
                val ordered = readItems().toMutableList()
                while (ordered.size < SLOT_COUNT) ordered.add(NONE)
                val previous = ordered[index]
                val existing = ordered.indexOf(action)
                ordered[index] = action
                if (action != NONE && existing >= 0 && existing != index) ordered[existing] = previous
                val result = ordered.filter { it != NONE }
                if (result.isEmpty()) return@setOnPreferenceChangeListener false
                val saved = Settings.System.putStringForUser(requireContext().contentResolver,
                    ITEMS, result.joinToString(","), UserHandle.USER_CURRENT)
                if (saved) updateSlots(result)
                false // Settings.System is the source of truth, not Preference storage.
            }
        }
        updateSlots(readItems())
    }

    override fun onResume() {
        super.onResume()
        updateSlots(readItems())
    }

    private fun readItems(): List<String> {
        val stored = Settings.System.getStringForUser(requireContext().contentResolver,
            ITEMS, UserHandle.USER_CURRENT) ?: DEFAULT_ITEMS
        val allowed = resources.getStringArray(R.array.rising_pie_slice_values).toSet() - NONE
        return stored.split(',').map { it.trim() }.filter { it in allowed }.distinct().take(SLOT_COUNT)
    }

    private fun updateSlots(items: List<String>) {
        slots.forEachIndexed { index, pref ->
            pref.value = items.getOrNull(index) ?: NONE
            pref.summary = pref.entry
        }
    }

    override fun getMetricsCategory(): Int = MetricsProto.MetricsEvent.VIEW_UNKNOWN
}
