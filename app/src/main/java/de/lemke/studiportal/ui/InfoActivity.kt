package de.lemke.studiportal.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import de.lemke.studiportal.R
import de.lemke.studiportal.databinding.ActivityInfoBinding
import de.lemke.studiportal.domain.SetWorkManagerUseCase
import javax.inject.Inject

@AndroidEntryPoint
class InfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInfoBinding

    @Inject
    lateinit var setWorkManager: SetWorkManagerUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.videoViewOffline.apply {
            focusable = View.NOT_FOCUSABLE
            setVideoPath("android.resource://" + packageName + "/" + R.raw.where)
            setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                start()
                binding.nestedScrollView.scrollTo(0, 0)
            }
        }
        binding.videoViewOnline.apply {
            focusable = View.NOT_FOCUSABLE
            setVideoPath("android.resource://" + packageName + "/" + R.raw.yeah)
            setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                start()
                binding.nestedScrollView.scrollTo(0, 0)
            }
        }
        binding.videoViewSSO.apply {
            focusable = View.NOT_FOCUSABLE
            setVideoPath("android.resource://" + packageName + "/" + R.raw.huh)
            setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                start()
                binding.nestedScrollView.scrollTo(0, 0)
            }
        }
        binding.videoViewBeg.apply {
            focusable = View.NOT_FOCUSABLE
            setVideoPath("android.resource://" + packageName + "/" + R.raw.beg)
            setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                start()
                binding.nestedScrollView.scrollTo(0, 0)
            }
        }
        binding.openOnlineButton.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.studiportal_url))))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, getString(R.string.no_browser_app_installed), Toast.LENGTH_SHORT).show()
            }
        }
        binding.imzButton.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.hfu_imz_url))))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, getString(R.string.no_browser_app_installed), Toast.LENGTH_SHORT).show()
            }
        }
        binding.contactMeButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:") // only email apps should handle this
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.email)))
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            intent.putExtra(Intent.EXTRA_TEXT, "")
            try {
                startActivity(intent)
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(this, getString(R.string.no_email_app_installed), Toast.LENGTH_SHORT).show()
            }
        }
        binding.contributeOnGithubButton.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.studiportal_github))))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, getString(R.string.no_browser_app_installed), Toast.LENGTH_SHORT).show()
            }
        }

        setWorkManager.cancelStudiportalWork()
    }
}