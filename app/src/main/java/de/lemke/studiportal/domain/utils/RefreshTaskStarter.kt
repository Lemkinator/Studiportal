package de.lemke.studiportal.domain.utils

import dagger.hilt.android.AndroidEntryPoint
import android.content.BroadcastReceiver
import android.content.Intent
import android.net.wifi.WifiManager
import android.app.PendingIntent
import android.app.AlarmManager
import android.app.Activity
import android.content.Context
import de.lemke.studiportal.ui.LoginActivity
import de.lemke.studiportal.R
import de.lemke.studiportal.domain.GetUserSettingsUseCase
import de.lemke.studiportal.domain.UpdateUserSettingsUseCase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class RefreshTaskStarter : BroadcastReceiver() {

    @Inject
    lateinit var getUserSettings: GetUserSettingsUseCase

    @Inject
    lateinit var updateUserSettings: UpdateUserSettingsUseCase

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        GlobalScope.launch {
            val userSettings = getUserSettings()
            //If the Broadcast CHECK_FOR_UPDATE arrives, let's check
            if (intent.action == context.getString(R.string.action_check_for_updates)) {
                //Only check if wifi is enabled or we are allowed to check over cellular
                if (wifiManager.isWifiEnabled || userSettings.useMobileData) {
                    //RefreshTask(context).execute();
                } else {
                    updateUserSettings { it.copy(isRefreshOverdue = true) }
                }
            }

            //init after reboot
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) startRefreshTask(context)

            //If the Network state changed and Wifi is now on and the last update is delayed -> update and reset overdue flag
            if (intent.action == WifiManager.NETWORK_STATE_CHANGED_ACTION && wifiManager.isWifiEnabled && userSettings.isRefreshOverdue) {
                updateUserSettings { it.copy(isRefreshOverdue = false) }
                //RefreshTask(context).execute();
            }
        }
    }

    private fun createPendingIntent(context: Context?): PendingIntent {
        val intent = Intent()
        intent.action = context?.getString(R.string.action_check_for_updates)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun cancelRefreshTask(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val toCancel = createPendingIntent(context)
        toCancel.cancel()
        alarmManager.cancel(toCancel)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun startRefreshTask(context: Context) {
        //Check if user and password is available, if not start Login
        GlobalScope.launch {
            val userSettings = getUserSettings()
            if ((userSettings.username.isEmpty() || userSettings.password.isEmpty()) && context is Activity) {
                val intent = Intent(context, LoginActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)

                //Quit the old Activity to prevent going back
                context.finish()
                return@launch
            }

            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                TimeUnit.MILLISECONDS.convert(userSettings.refreshInterval, TimeUnit.MINUTES),
                createPendingIntent(context)
            )
        }
    }

}