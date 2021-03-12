package ru.transservice.routemanager.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import ru.transservice.routemanager.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}