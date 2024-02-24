package de.lemke.studiportal.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import de.lemke.studiportal.R
import de.lemke.studiportal.databinding.ActivityLoginBinding
import de.lemke.studiportal.domain.DemoUseCase
import de.lemke.studiportal.domain.GetStudiportalDataUseCase
import de.lemke.studiportal.domain.GetUserSettingsUseCase
import de.lemke.studiportal.domain.SetWorkManagerUseCase
import de.lemke.studiportal.domain.UpdateExamsUseCase
import de.lemke.studiportal.domain.UpdateUserSettingsUseCase
import de.lemke.studiportal.domain.model.Exam
import de.lemke.studiportal.domain.setCustomOnBackPressedLogic
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var successCallback: (data: Pair<Pair<String, String>?, List<Exam>>) -> Unit = {}
    private var errorCallback: (message: String) -> Unit = {}
    private var loginSuccessCallback: () -> Unit = {}
    private var loginErrorCallback: (message: String) -> Unit = {}
    private var timeoutErrorCallback: () -> Unit = {}
    private var username: String = ""
    private var password: String = ""
    private var time: Long = 0
    private var loginSuccess = false

    @Inject
    lateinit var getUserSettings: GetUserSettingsUseCase

    @Inject
    lateinit var updateUserSettings: UpdateUserSettingsUseCase

    @Inject
    lateinit var getStudiportalData: GetStudiportalDataUseCase

    @Inject
    lateinit var updateExams: UpdateExamsUseCase

    @Inject
    lateinit var demo: DemoUseCase

    @Inject
    lateinit var setWorkManager: SetWorkManagerUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
        }
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.editTextUsername.doAfterTextChanged { username = it.toString() }
        binding.editTextPassword.doAfterTextChanged { password = it.toString() }
        initCallbacks()
        initFooterButton()
        setCustomOnBackPressedLogic {
            if (System.currentTimeMillis() - time < 3000) finishAffinity()
            else {
                Toast.makeText(this@LoginActivity, resources.getString(R.string.press_again_to_exit), Toast.LENGTH_SHORT).show()
                time = System.currentTimeMillis()
            }
        }
    }

    private fun initCallbacks() {
        successCallback = { data ->
            lifecycleScope.launch {
                updateUserSettings {
                    it.copy(studentName = data.first?.first ?: it.studentName, studentInfo = data.first?.second ?: it.studentInfo)
                }
                updateExams(data.second, false)
                openNextActivity()
            }
        }
        errorCallback = { message ->
            if (loginSuccess) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.error)
                    .setMessage(getString(R.string.login_success_observe_error, message))
                    .setPositiveButton(R.string.ok) { _, _ -> lifecycleScope.launch { openNextActivity() } }
                    .create()
                    .show()
            }
        }
        loginSuccessCallback = {
            loginSuccess = true
            lifecycleScope.launch { updateUserSettings { it.copy(username = username, password = password) } }
        }
        loginErrorCallback = { message ->
            loginSuccess = false
            lifecycleScope.launch {
                AlertDialog.Builder(this@LoginActivity)
                    .setTitle(getString(R.string.error))
                    .setMessage(message)
                    .setPositiveButton(R.string.ok, null)
                    .create()
                    .show()
                binding.loginFooterButtonProgress.visibility = View.GONE
                binding.loginFooterButton.visibility = View.VISIBLE
            }
        }
        timeoutErrorCallback = {
            loginSuccess = false
            val videoView = FrameLayout(this@LoginActivity).apply {
                addView(
                    VideoView(this@LoginActivity).apply {
                        setVideoPath("android.resource://$packageName/" + R.raw.where)
                        setOnPreparedListener { mediaPlayer ->
                            mediaPlayer.isLooping = true
                            start()
                        }
                    },
                    FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                        val pxValue = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20F, resources.displayMetrics).toInt()
                        setMargins(pxValue, pxValue, pxValue, pxValue)
                    })
            }
            AlertDialog.Builder(this@LoginActivity)
                .setTitle(getString(R.string.error_timeout))
                .setView(videoView)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, null)
                .create()
                .show()
            binding.loginFooterButtonProgress.visibility = View.GONE
            binding.loginFooterButton.visibility = View.VISIBLE
        }
    }

    private fun initFooterButton() {
        if (resources.configuration.screenWidthDp < 360) {
            binding.loginFooterButton.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        }
        binding.loginFooterButton.setOnClickListener {
            val isDemo = username.equals(demo.username, ignoreCase = true)
            if (isDemo) username = demo.username
            if (username.isNotEmpty() && password.isNotEmpty() || isDemo) {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
                    currentFocus?.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
                binding.loginFooterButton.visibility = View.GONE
                binding.loginFooterButtonProgress.visibility = View.VISIBLE
                lifecycleScope.launch {
                    if (isDemo) {
                        updateUserSettings {
                            it.copy(
                                username = demo.username,
                                password = "",
                                studentName = getString(R.string.demo_student_name),
                                studentInfo = getString(R.string.demo_student_info)
                            )
                        }
                        demo.initDemoExams()
                        openNextActivity()
                    } else getStudiportalData(
                        successCallback = successCallback,
                        errorCallback = errorCallback,
                        loginSuccessCallback = loginSuccessCallback,
                        loginErrorCallback = loginErrorCallback,
                        timeoutErrorCallback = timeoutErrorCallback
                    )
                }
            } else {
                Toast.makeText(this, getString(R.string.username_or_password_empty), Toast.LENGTH_SHORT).show()
            }

        }
    }

    private suspend fun openNextActivity() {
        setWorkManager()
        startActivity(Intent(this, MainActivity::class.java))
        if (Build.VERSION.SDK_INT < 34) {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        finish()
    }
}