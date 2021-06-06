package be.bearhatcode.healthchecksio

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private val listener: Preference.OnPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { pref: Preference, s: Any ->
                val sV = s.toString()
                val parts = sV.split("\n")
                val cleaned = parts.map { v -> v.trim() }.filter { v -> v.isNotEmpty() }

                val bad = cleaned.filter { v -> !v.matches(Regex("^[^ ]+$")) }

                if(bad.isEmpty()){
                    pref.summary = ""+(cleaned.size)+" configured"
                }else{
                    Toast.makeText(this.context, "Failed to set API keys", Toast.LENGTH_LONG).show()
                }

                bad.isEmpty()
            }


        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val apiKeyPref = findPreference<EditTextPreference>("apiKeys")  ?: throw RuntimeException("could nto ")
            apiKeyPref.onPreferenceChangeListener =  listener
            listener.onPreferenceChange(apiKeyPref, apiKeyPref.text)
        }


    }
}