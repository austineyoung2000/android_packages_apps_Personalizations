/*
 * SPDX-FileCopyrightText: The Resurrected Remix Project
 * SPDX-FileCopyrightText: The RisingOS Android Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.rising.settings.fragments

import android.content.Intent
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider

/** Pie slice order and configuration, adapted from Resurrected Remix PieSettings. */
class PieSettings : SettingsPreferenceFragment() {
    companion object {
        private const val ITEMS = "pie_items"
        private const val APPS = "pie_apps"
        private const val APP_ICON_PACK = "pie_app_icon_pack"
        private const val APP_COUNT = 6
        private const val DEFAULT_ITEMS = "back,home,recents,notifications,flashlight,screenshot,power"
        private const val NONE = "none"
        private const val SLOT_COUNT = 9
        @JvmField val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.rising_pie_settings)
    }

    private val slots = mutableListOf<ListPreference>()
    private val appSlots = mutableListOf<Preference>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.rising_pie_settings)
        findPreference<Preference>("pie_style")?.setOnPreferenceChangeListener { _, value ->
            updateAppearanceVisibility(value.toString().toIntOrNull() ?: 2)
            true
        }
        updateAppearanceVisibility()
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
        val appCategory = findPreference<PreferenceCategory>("pie_apps_category")
        if (appCategory != null) {
            val packPref = ListPreference(requireContext()).apply {
                key = "pie_app_icon_pack_picker"
                title = getString(R.string.rising_pie_icon_pack_title)
                isPersistent = false
            }
            appCategory.addPreference(packPref)
            packPref.dependency = "pie_apps_enabled"
            setupIconPacks(packPref)
            repeat(APP_COUNT) { index ->
                val pref = Preference(requireContext()).apply {
                    key = "pie_app_slot_$index"
                    title = getString(R.string.rising_pie_app_slot, index + 1)
                    isPersistent = false
                }
                appCategory.addPreference(pref)
                pref.dependency = "pie_apps_enabled"
                pref.setOnPreferenceClickListener { showAppPicker(index); true }
                appSlots.add(pref)
            }
            updateAppSlots()
        }
    }

    override fun onResume() {
        super.onResume()
        updateAppearanceVisibility()
        updateSlots(readItems())
        updateAppSlots()
    }

    private fun updateAppearanceVisibility(selected: Int? = null) {
        val resolver = context?.contentResolver ?: return
        val effect = selected ?: Settings.System.getIntForUser(resolver, "pie_style", 2,
            UserHandle.USER_CURRENT)
        findPreference<Preference>("pie_first_color")?.isVisible = effect in listOf(0, 3, 4, 5)
        findPreference<Preference>("pie_second_color")?.isVisible = effect == 3 || effect == 5
        findPreference<Preference>("pie_glow_intensity")?.isVisible = effect == 0 || effect == 3
    }

    private fun setupIconPacks(pref: ListPreference) {
        val pm = requireContext().packageManager
        val packs = linkedMapOf<String, String>()
        listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME",
            "com.anddoes.launcher.THEME", "com.gau.go.launcherex.theme").forEach { action ->
            pm.queryIntentActivities(Intent(action), 0).forEach resultLoop@ { result ->
                val info = result.activityInfo?.applicationInfo ?: return@resultLoop
                packs[info.packageName] = info.loadLabel(pm).toString()
            }
        }
        val saved = Settings.System.getStringForUser(requireContext().contentResolver,
            APP_ICON_PACK, UserHandle.USER_CURRENT).orEmpty()
        val names = mutableListOf(getString(R.string.rising_pie_icon_pack_default))
        val values = mutableListOf("")
        packs.entries.sortedBy { it.value.lowercase() }.forEach {
            names.add(it.value); values.add(it.key)
        }
        if (saved.isNotEmpty() && saved !in values) { names.add(saved); values.add(saved) }
        pref.entries = names.toTypedArray()
        pref.entryValues = values.toTypedArray()
        pref.value = saved
        pref.summary = names[values.indexOf(saved)]
        pref.setOnPreferenceChangeListener { _, value ->
            val pack = value.toString()
            val savedOk = Settings.System.putStringForUser(requireContext().contentResolver,
                APP_ICON_PACK, pack.ifEmpty { null }, UserHandle.USER_CURRENT)
            if (savedOk) pref.summary = names[values.indexOf(pack)]
            savedOk
        }
    }

    private fun readApps(): List<String> =
        (Settings.System.getStringForUser(requireContext().contentResolver, APPS,
            UserHandle.USER_CURRENT) ?: "").split(',').filter { it.isNotBlank() }.distinct().take(APP_COUNT)

    private fun updateAppSlots() {
        val pm = requireContext().packageManager
        val apps = readApps()
        appSlots.forEachIndexed { index, pref ->
            val pkg = apps.getOrNull(index)
            pref.summary = if (pkg == null) getString(R.string.rising_pie_app_none) else try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (e: android.content.pm.PackageManager.NameNotFoundException) { pkg }
        }
    }

    private fun showAppPicker(index: Int) {
        val pm = requireContext().packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, 0)
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
        val labels = arrayOf(getString(R.string.rising_pie_app_none)) +
            apps.map { it.loadLabel(pm).toString() }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.rising_pie_app_choose)
            .setItems(labels) { _, which ->
                val selected = if (which == 0) null else apps[which - 1].activityInfo.packageName
                val current = readApps().toMutableList()
                while (current.size <= index) current.add("")
                current[index] = selected ?: ""
                val result = current.filter { it.isNotEmpty() }.distinct().take(APP_COUNT)
                if (Settings.System.putStringForUser(requireContext().contentResolver,
                        APPS, result.joinToString(","), UserHandle.USER_CURRENT)) updateAppSlots()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
