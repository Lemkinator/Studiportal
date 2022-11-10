package de.lemke.studiportal.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import de.lemke.studiportal.R
import de.lemke.studiportal.databinding.ActivityOobeBinding
import de.lemke.studiportal.domain.GetUserSettingsUseCase
import de.lemke.studiportal.domain.UpdateUserSettingsUseCase
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity(), Refreshable {
    private lateinit var binding: ActivityOobeBinding
    private var time: Long = 0

    @Inject
    lateinit var getUserSettings: GetUserSettingsUseCase

    @Inject
    lateinit var updateUserSettings: UpdateUserSettingsUseCase

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOobeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initOnBackPressed()

    }

    private fun initOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                lifecycleScope.launch {
                    if (System.currentTimeMillis() - time < 3000) finishAffinity()
                    else {
                        Toast.makeText(this@LoginActivity, resources.getString(R.string.press_again_to_exit), Toast.LENGTH_SHORT).show()
                        time = System.currentTimeMillis()
                    }
                }
            }
        })
    }

    private fun openNextActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onRefresh() {

    }
}