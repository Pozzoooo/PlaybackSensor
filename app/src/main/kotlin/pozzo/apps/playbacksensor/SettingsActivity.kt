package pozzo.apps.playbacksensor

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import pozzo.apps.tools.Log

/**
 * todo need to create an image to explain how to use it :(
 *      I can maybe even add to the main screen, as I don't know what else I can add
 *
 * @author galien
 * @since 15/10/17.
 */
class SettingsActivity : PreferenceActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupLogs()
        initialiseBaseServiceStateBasedOnSettings()
        registerItlsefForPreferenceChanges()
        setupLayout()
    }

    private fun setupLogs() {
        Log.SHOW_LOGS = BuildConfig.DEBUG
    }

    private fun initialiseBaseServiceStateBasedOnSettings() {
        if (isEnabled()) {
            startService()
        }
    }

    private fun registerItlsefForPreferenceChanges() {
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this)
    }

    private fun setupLayout() {
        fragmentManager.beginTransaction().replace(android.R.id.content,
                GeneralPreferenceFragment()).commit()
    }

    override fun onIsMultiPane(): Boolean = isXLargeTablet(this)

    override fun isValidFragment(fragmentName: String): Boolean =
            PreferenceFragment::class.java.name == fragmentName || GeneralPreferenceFragment::class.java.name == fragmentName

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (Settings.ENABLED != key) {
            return
        }

        if (isEnabled()) {
            startService()
        } else {
            stopService()
        }
    }

    private fun startService() {
        startService(Intent(this, SensorService::class.java))
    }

    private fun stopService() {
        startService(SensorService.getStopIntent(this))
    }

    private fun isEnabled(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        return sharedPreferences.getBoolean(Settings.ENABLED, false)
    }

    class GeneralPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_general)
        }
    }

    companion object {
        private fun isXLargeTablet(context: Context): Boolean =
                context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
    }
}
