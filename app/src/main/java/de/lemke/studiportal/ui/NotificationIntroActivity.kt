package de.lemke.studiportal.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import de.lemke.studiportal.R
import de.lemke.studiportal.databinding.ActivityNotificationIntroBinding
import de.lemke.studiportal.domain.GetUserSettingsUseCase
import de.lemke.studiportal.domain.UpdateUserSettingsUseCase
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class NotificationIntroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationIntroBinding
    private var time: Long = 0

    @Inject
    lateinit var getUserSettings: GetUserSettingsUseCase

    @Inject
    lateinit var updateUserSettings: UpdateUserSettingsUseCase

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
        }
        binding = ActivityNotificationIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initOnBackPressed()
        initFooterButton()
        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.notificationShowGradeSwitch.isEnabled = isChecked
            binding.notificationShowGradeDescription.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun initOnBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                if (System.currentTimeMillis() - time < 3000) finishAffinity()
                else {
                    Toast.makeText(this@NotificationIntroActivity, resources.getString(R.string.press_again_to_exit), Toast.LENGTH_SHORT).show()
                    time = System.currentTimeMillis()
                }
            }
        else onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - time < 3000) finishAffinity()
                else {
                    Toast.makeText(this@NotificationIntroActivity, resources.getString(R.string.press_again_to_exit), Toast.LENGTH_SHORT).show()
                    time = System.currentTimeMillis()
                }
            }
        })
    }

    private fun initFooterButton() {
        if (resources.configuration.screenWidthDp < 360) {
            binding.notificationFooterButton.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        }
        binding.notificationFooterButton.setOnClickListener {
            binding.notificationFooterButton.visibility = View.GONE
            binding.notificationFooterButtonProgress.visibility = View.VISIBLE
            lifecycleScope.launch { checkNotifications() }
        }
    }

    // Register the permissions callback, which handles the user's response to the system permissions dialog. Save the return value,
    // an instance of ActivityResultLauncher. You can use either a val, as shown in this snippet,
    // or a lateinit var in your onAttach() or onCreate() method.
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        lifecycleScope.launch {
            // Permission is granted. Continue the action or workflow in your app.
            if (isGranted) updateUserSettings { it.copy(notificationsEnabled = true) }
            // Explain to the user that the feature is unavailable because the features requires a permission that the user has denied.
            // At the same time, respect the user's decision. Don't link to system settings in an effort to convince the user
            // to change their decision.
            else updateUserSettings { it.copy(notificationsEnabled = false) }
            openNextActivity()
        }
    }

    private suspend fun checkNotifications() {
        updateUserSettings { it.copy(showGradeInNotification = binding.notificationShowGradeSwitch.isChecked) }
        if (binding.notificationSwitch.isChecked) {
            //Enable Notifications when < Android 13 or permission is granted, else ask for permission
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
                    this@NotificationIntroActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                updateUserSettings { it.copy(notificationsEnabled = true) }
                openNextActivity()
            } else requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

        } else {
            updateUserSettings { it.copy(notificationsEnabled = false) }
            openNextActivity()
        }
    }

    private suspend fun openNextActivity() {
        updateUserSettings { it.copy(tosAccepted = true) }
        if (getUserSettings().username.isBlank()) startActivity(Intent(this, LoginActivity::class.java))
        else startActivity(Intent(this, MainActivity::class.java))
        if (Build.VERSION.SDK_INT < 34) {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        finish()
    }
}