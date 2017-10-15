package pozzo.apps.playbacksensor

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceManager

/**
 * @author galien
 * @since 15/10/17.
 */
class SettingsActivity : AppCompatPreferenceActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isEnabled()) {
            startService()
        }
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this)

        fragmentManager.beginTransaction().replace(android.R.id.content,
                GeneralPreferenceFragment()).commit()
    }

    private fun startService() {
        startService(Intent(this, SensorService::class.java))
    }

    private fun stopService() {
        val intent = Intent(this, SensorService::class.java)
        intent.putExtra("stop", true)
        startService(intent)
    }

    override fun onIsMultiPane(): Boolean = isXLargeTablet(this)

    override fun isValidFragment(fragmentName: String): Boolean =
            PreferenceFragment::class.java.name == fragmentName || GeneralPreferenceFragment::class.java.name == fragmentName

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == "enabled") {
            val runningDialog = sharedPreferences.getBoolean("enabled", true)
            if (runningDialog) {
                startService()
            } else {
                stopService()
            }
        }
    }

    private fun isEnabled(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        return sharedPreferences.getBoolean("enabled", false)
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
