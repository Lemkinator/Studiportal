package de.lemke.studiportal.ui

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.util.SeslMisc
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
import androidx.preference.Preference.OnPreferenceClickListener
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import de.lemke.studiportal.R
import de.lemke.studiportal.databinding.ActivitySettingsBinding
import de.lemke.studiportal.domain.*
import dev.oneuiproject.oneui.preference.HorizontalRadioPreference
import dev.oneuiproject.oneui.preference.internal.PreferenceRelatedCard
import dev.oneuiproject.oneui.utils.PreferenceUtils.createRelatedCard
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.log

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbarLayout.setNavigationButtonTooltip(getString(R.string.sesl_navigate_up))
        binding.toolbarLayout.setNavigationButtonOnClickListener { finish() }
        if (savedInstanceState == null) supportFragmentManager.beginTransaction().replace(R.id.settings, SettingsFragment()).commit()
    }

    @AndroidEntryPoint
    class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
        private lateinit var settingsActivity: SettingsActivity
        private lateinit var darkModePref: HorizontalRadioPreference
        private lateinit var autoDarkModePref: SwitchPreferenceCompat
        private lateinit var confirmExitPref: SwitchPreferenceCompat
        private lateinit var logoutPref: PreferenceScreen
        private var relatedCard: PreferenceRelatedCard? = null
        private var lastTimeVersionClicked: Long = 0

        @Inject
        lateinit var getUserSettings: GetUserSettingsUseCase

        @Inject
        lateinit var updateUserSettings: UpdateUserSettingsUseCase

        override fun onAttach(context: Context) {
            super.onAttach(context)
            if (activity is SettingsActivity) settingsActivity = activity as SettingsActivity
        }

        override fun onCreatePreferences(bundle: Bundle?, str: String?) {
            addPreferencesFromResource(R.xml.preferences)
        }

        override fun onCreate(bundle: Bundle?) {
            super.onCreate(bundle)
            lastTimeVersionClicked = System.currentTimeMillis()
            initPreferences()
        }

        @SuppressLint("RestrictedApi", "UnspecifiedImmutableFlag")
        private fun initPreferences() {
            val darkMode = AppCompatDelegate.getDefaultNightMode()
            darkModePref = findPreference("dark_mode_pref")!!
            autoDarkModePref = findPreference("dark_mode_auto_pref")!!
            confirmExitPref = findPreference("confirm_exit_pref")!!
            logoutPref = findPreference("logout_pref")!!
            logoutPref.onPreferenceClickListener = OnPreferenceClickListener {
                //TODO
                true
            }
            lifecycleScope.launch {
                logoutPref.summary = getUserSettings().username
            }
            autoDarkModePref.onPreferenceChangeListener = this
            autoDarkModePref.isChecked = darkMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM ||
                    darkMode == AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY ||
                    darkMode == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
            darkModePref.onPreferenceChangeListener = this
            darkModePref.setDividerEnabled(false)
            darkModePref.setTouchEffectEnabled(false)
            darkModePref.isEnabled = !autoDarkModePref.isChecked
            darkModePref.value = if (SeslMisc.isLightTheme(settingsActivity)) "0" else "1"

            confirmExitPref.onPreferenceChangeListener = this

            findPreference<PreferenceScreen>("privacy_pref")!!.onPreferenceClickListener = OnPreferenceClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_website))))
                true
            }

            findPreference<PreferenceScreen>("tos_pref")!!.onPreferenceClickListener = OnPreferenceClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.tos))
                    .setMessage(getString(R.string.tos_content))
                    .setPositiveButton(R.string.ok) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                    .create()
                    .show()
                true
            }
            findPreference<PreferenceScreen>("report_bug_pref")!!.onPreferenceClickListener = OnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto:") // only email apps should handle this
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.email)))
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                intent.putExtra(Intent.EXTRA_TEXT, "")
                try {
                    startActivity(intent)
                } catch (ex: ActivityNotFoundException) {
                    Toast.makeText(requireContext(), getString(R.string.no_email_app_installed), Toast.LENGTH_SHORT).show()
                }
                true
            }

            AppUpdateManagerFactory.create(requireContext()).appUpdateInfo.addOnSuccessListener { appUpdateInfo: AppUpdateInfo ->
                findPreference<Preference>("about_app_pref")?.widgetLayoutResource =
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) R.layout.sesl_preference_badge else 0
            }

            lifecycleScope.launch {
                if (!getUserSettings().devModeEnabled) preferenceScreen.removePreference(findPreference("dev_options"))
            }
            findPreference<PreferenceScreen>("delete_app_data_pref")?.setOnPreferenceClickListener {
                AlertDialog.Builder(settingsActivity)
                    .setTitle(R.string.delete_app_data_and_exit)
                    .setMessage(R.string.delete_app_data_and_exit_warning)
                    .setNegativeButton(R.string.sesl_cancel, null)
                    .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                        (settingsActivity.getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
                    }
                    .create()
                    .show()
                true
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            requireView().setBackgroundColor(
                resources.getColor(dev.oneuiproject.oneui.design.R.color.oui_background_color, settingsActivity.theme)
            )
        }

        override fun onStart() {
            super.onStart()
            lifecycleScope.launch {
                val userSettings = getUserSettings()
                confirmExitPref.isChecked = userSettings.confirmExit
            }
            setRelatedCardView()
        }

        @SuppressLint("WrongConstant", "RestrictedApi")
        @Suppress("UNCHECKED_CAST")
        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
            when (preference.key) {
                "dark_mode_pref" -> {
                    AppCompatDelegate.setDefaultNightMode(
                        if (newValue == "0") AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
                    )
                    return true
                }
                "dark_mode_auto_pref" -> {
                    if (newValue as Boolean) {
                        darkModePref.isEnabled = false
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    } else {
                        darkModePref.isEnabled = true
                        if (SeslMisc.isLightTheme(settingsActivity)) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                    return true
                }
                "confirm_exit_pref" -> {
                    lifecycleScope.launch { updateUserSettings { it.copy(confirmExit = newValue as Boolean) } }
                    return true
                }
            }
            return false
        }

        private fun setRelatedCardView() {
            if (relatedCard == null) {
                relatedCard = createRelatedCard(settingsActivity)
                relatedCard?.setTitleText(getString(dev.oneuiproject.oneui.design.R.string.oui_relative_description))
                relatedCard?.addButton(getString(R.string.about_me)) {
                    startActivity(
                        Intent(
                            settingsActivity,
                            AboutMeActivity::class.java
                        )
                    )
                }
                    ?.show(this)
            }
        }
    }
}