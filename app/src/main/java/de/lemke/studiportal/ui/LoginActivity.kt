package de.lemke.studiportal.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import de.lemke.studiportal.R
import de.lemke.studiportal.databinding.ActivityLoginBinding
import de.lemke.studiportal.domain.*
import de.lemke.studiportal.domain.model.Exam
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var successCallback: (data: Pair<Pair<String, String>?, List<Exam>>) -> Unit = {}
    private var errorCallback: (message: String) -> Unit = {}
    private var loginSuccessCallback: () -> Unit = {}
    private var loginErrorCallback: (message: String) -> Unit = {}
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

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
        }
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initEditTexts()
        initCallbacks()
        initFooterButton()
        initOnBackPressed()
    }

    private fun initEditTexts() {
        lifecycleScope.launch {
            val userSettings = getUserSettings()
            binding.editTextUsername.setText(userSettings.username)
            binding.editTextPassword.setText(userSettings.password)
            binding.editTextUsername.doAfterTextChanged { username = it.toString() }
            binding.editTextPassword.doAfterTextChanged { password = it.toString() }
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
        }
        loginErrorCallback = { message ->
            loginSuccess = false
            lifecycleScope.launch {
                updateUserSettings { it.copy(username = "", password = "") }
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
                    updateUserSettings { it.copy(username = username, password = password) }
                    if (isDemo) {
                        updateUserSettings { it.copy(studentName = getString(R.string.demo_student_name), studentInfo = getString(R.string.demo_student_info)) }
                        demo.initDemoExams()
                        openNextActivity()
                    } else getStudiportalData(
                        successCallback = successCallback,
                        errorCallback = errorCallback,
                        loginSuccessCallback = loginSuccessCallback,
                        loginErrorCallback = loginErrorCallback,
                    )
                }
            } else {
                Toast.makeText(this, getString(R.string.username_or_password_empty), Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun initOnBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                if (System.currentTimeMillis() - time < 3000) finishAffinity()
                else {
                    Toast.makeText(this@LoginActivity, resources.getString(R.string.press_again_to_exit), Toast.LENGTH_SHORT).show()
                    time = System.currentTimeMillis()
                }
            }
        else onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - time < 3000) finishAffinity()
                else {
                    Toast.makeText(this@LoginActivity, resources.getString(R.string.press_again_to_exit), Toast.LENGTH_SHORT).show()
                    time = System.currentTimeMillis()
                }
            }
        })
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