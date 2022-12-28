package de.lemke.studiportal.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
import androidx.preference.Preference.OnPreferenceClickListener
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import de.lemke.studiportal.R
import de.lemke.studiportal.data.RefreshInterval
import de.lemke.studiportal.databinding.ActivitySettingsBinding
import de.lemke.studiportal.domain.*
import dev.oneuiproject.oneui.preference.HorizontalRadioPreference
import dev.oneuiproject.oneui.preference.internal.PreferenceRelatedCard
import dev.oneuiproject.oneui.utils.DialogUtils
import dev.oneuiproject.oneui.utils.PreferenceUtils.createRelatedCard
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

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
        private lateinit var notifyAboutChangePref: SwitchPreferenceCompat
        private lateinit var showGradeInNotificationPref: SwitchPreferenceCompat
        private lateinit var useMeteredNetworkPref: SwitchPreferenceCompat
        private lateinit var logoutPref: PreferenceScreen
        private lateinit var refreshIntervalPref: DropDownPreference
        private var relatedCard: PreferenceRelatedCard? = null
        private var lastTimeVersionClicked: Long = 0

        @Inject
        lateinit var getUserSettings: GetUserSettingsUseCase

        @Inject
        lateinit var updateUserSettings: UpdateUserSettingsUseCase

        @Inject
        lateinit var deleteExams: DeleteExamsUseCase

        @Inject
        lateinit var setWorkManager: SetWorkManagerUseCase

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

        private fun initPreferences() {
            AppCompatDelegate.getDefaultNightMode()
            darkModePref = findPreference("dark_mode_pref")!!
            autoDarkModePref = findPreference("dark_mode_auto_pref")!!
            notifyAboutChangePref = findPreference("notify_about_change_pref")!!
            showGradeInNotificationPref = findPreference("show_grade_in_notification_pref")!!
            useMeteredNetworkPref = findPreference("use_metered_network_pref")!!
            logoutPref = findPreference("logout_pref")!!
            refreshIntervalPref = findPreference("refresh_interval_pref")!!

            autoDarkModePref.onPreferenceChangeListener = this
            darkModePref.onPreferenceChangeListener = this
            darkModePref.setDividerEnabled(false)
            darkModePref.setTouchEffectEnabled(false)
            notifyAboutChangePref.onPreferenceChangeListener = this
            showGradeInNotificationPref.onPreferenceChangeListener = this
            useMeteredNetworkPref.onPreferenceChangeListener = this
            refreshIntervalPref.onPreferenceChangeListener = this

            logoutPref.onPreferenceClickListener = OnPreferenceClickListener {
                val dialog = AlertDialog.Builder(settingsActivity)
                    .setTitle(getString(R.string.logout))
                    .setMessage(getString(R.string.logout_message))
                    .setNegativeButton(getString(R.string.sesl_cancel), null)
                    .setPositiveButton(R.string.ok, null)
                    .create()
                dialog.show()
                DialogUtils.setDialogProgressForButton(dialog, DialogInterface.BUTTON_POSITIVE) {
                    lifecycleScope.launch {
                        updateUserSettings { it.copy(username = "", password = "", allowMeteredConnection = true) }
                        deleteExams()
                        setWorkManager.cancelStudiportalWork()
                        startActivity(Intent(settingsActivity, LoginActivity::class.java))
                        settingsActivity.finish()
                    }
                }
                true
            }
            lifecycleScope.launch {
                val userSettings = getUserSettings()
                autoDarkModePref.isChecked = userSettings.autoDarkMode
                darkModePref.isEnabled = !autoDarkModePref.isChecked
                darkModePref.value = if (userSettings.darkMode) "1" else "0"
                logoutPref.summary = userSettings.username
                setRefreshIntervalPrefSummary(userSettings.refreshInterval, userSettings.lastRefresh)
            }

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
                    .setTitle(R.string.delete_appdata_and_exit)
                    .setMessage(R.string.delete_appdata_and_exit_warning)
                    .setNegativeButton(R.string.sesl_cancel, null)
                    .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                        (settingsActivity.getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
                    }
                    .create()
                    .show()
                true
            }
        }

        private fun setRefreshIntervalPrefSummary(refreshInterval: RefreshInterval, zonedDateTime: ZonedDateTime?) {
            refreshIntervalPref.summary = refreshInterval.getLocalString(requireContext()) + " | " + getString(
                R.string.last_updated,
                if (zonedDateTime == null) getString(R.string.never)
                else zonedDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
            )
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
                notifyAboutChangePref.isChecked =
                    userSettings.notificationsEnabled && areNotificationsEnabled(getString(R.string.exam_notification_channel_id))
                showGradeInNotificationPref.isChecked = userSettings.showGradeInNotification
                showGradeInNotificationPref.isEnabled = notifyAboutChangePref.isChecked
                useMeteredNetworkPref.isChecked = userSettings.allowMeteredConnection
            }
            setRelatedCardView()
        }

        override fun onResume() {
            super.onResume()
            lifecycleScope.launch {
                findPreference<PreferenceCategory>("dev_options")?.isVisible = getUserSettings().devModeEnabled
            }
        }

        @SuppressLint("WrongConstant", "RestrictedApi")
        @Suppress("UNCHECKED_CAST")
        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
            when (preference.key) {
                "dark_mode_pref" -> {
                    val darkMode = newValue as String == "1"
                    AppCompatDelegate.setDefaultNightMode(
                        if (darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                    )
                    lifecycleScope.launch {
                        updateUserSettings { it.copy(darkMode = darkMode) }
                    }
                    return true
                }
                "dark_mode_auto_pref" -> {
                    val autoDarkMode = newValue as Boolean
                    darkModePref.isEnabled = !autoDarkMode
                    lifecycleScope.launch {
                        if (autoDarkMode) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        else {
                            if (getUserSettings().darkMode) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                            else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        }
                        updateUserSettings { it.copy(autoDarkMode = newValue) }
                    }
                    return true
                }
                "notify_about_change_pref" -> {
                    if (newValue as Boolean) {
                        when {
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                                requireContext(),
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED -> {
                                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                notifyAboutChangePref.isChecked = false
                            }
                            !areNotificationsEnabled(getString(R.string.exam_notification_channel_id)) -> {
                                val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, settingsActivity.packageName)
                                //.putExtra(Settings.EXTRA_CHANNEL_ID, getString(R.string.exam_notification_channel_id))
                                startActivity(settingsIntent)
                                notifyAboutChangePref.isChecked = false
                            }
                            else -> {
                                setNotificationEnabled(true)
                            }
                        }
                    } else {
                        setNotificationEnabled(false)
                    }
                    return true
                }
                "show_grade_in_notification_pref" -> {
                    lifecycleScope.launch { updateUserSettings { it.copy(showGradeInNotification = newValue as Boolean) } }
                    return true
                }
                "use_metered_network_pref" -> {
                    lifecycleScope.launch {
                        updateUserSettings { it.copy(allowMeteredConnection = newValue as Boolean) }
                        setWorkManager()
                    }
                    return true
                }
                "refresh_interval_pref" -> {
                    lifecycleScope.launch {
                        val refreshInterval = RefreshInterval.fromMinutes(Integer.parseInt(newValue as String))
                        updateUserSettings { it.copy(refreshInterval = refreshInterval) }
                        setWorkManager()
                        setRefreshIntervalPrefSummary(refreshInterval, getUserSettings().lastRefresh)
                    }
                    return true
                }
            }
            return false
        }

        private fun setRelatedCardView() {
            if (relatedCard == null) {
                relatedCard = createRelatedCard(settingsActivity)
                relatedCard?.setTitleText(getString(dev.oneuiproject.oneui.design.R.string.oui_relative_description))
                relatedCard?.addButton(getString(R.string.help)) { startActivity(Intent(settingsActivity, HelpActivity::class.java)) }
                    ?.addButton(getString(R.string.about_me)) { startActivity(Intent(settingsActivity, AboutMeActivity::class.java)) }
                    ?.show(this)
            }
        }

        private val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your app.
                    setNotificationEnabled(true)
                } else {
                    // Explain to the user that the feature is unavailable because the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to system settings in an effort to convince the user
                    // to change their decision.
                    setNotificationEnabled(false)
                }
            }

        private fun setNotificationEnabled(enabled: Boolean) {
            showGradeInNotificationPref.isEnabled = enabled
            lifecycleScope.launch { updateUserSettings { it.copy(notificationsEnabled = enabled) } }
        }

        private fun areNotificationsEnabled(
            channelId: String? = null,
            notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(requireContext())
        ): Boolean = notificationManager.areNotificationsEnabled() &&
                if (channelId != null) {
                    notificationManager.getNotificationChannel(channelId)?.importance != NotificationManagerCompat.IMPORTANCE_NONE &&
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) ==
                                        PackageManager.PERMISSION_GRANTED
                            } else true
                } else true

    }
}