package de.lemke.studiportal.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.*
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.AndroidEntryPoint
import de.lemke.studiportal.R
import de.lemke.studiportal.databinding.ActivityAboutMeBinding
import de.lemke.studiportal.databinding.ActivityAboutMeContentBinding
import de.lemke.studiportal.domain.OpenAppUseCase
import dev.oneuiproject.oneui.utils.ViewUtils
import dev.oneuiproject.oneui.utils.internal.ToolbarLayoutUtils
import dev.oneuiproject.oneui.widget.Toast
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class AboutMeActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityAboutMeBinding
    private lateinit var bottomContent: ActivityAboutMeContentBinding
    private var enableBackToHeader = false
    private var lastClickTime: Long = 0
    private val appBarListener: AboutMeActivity.AboutMeAppBarListener = AboutMeAppBarListener()

    @Inject
    lateinit var openApp: OpenAppUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutMeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bottomContent = binding.aboutBottomContent
        setSupportActionBar(binding.aboutToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        binding.aboutToolbar.setNavigationOnClickListener { finish() }
        resetAppBar(resources.configuration)
        initContent()
        initOnBackPressed()
    }

    private fun initOnBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                if (enableBackToHeader && binding.aboutAppBar.seslIsCollapsed()) binding.aboutAppBar.setExpanded(true)
                else finish()
            }
        else onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (enableBackToHeader && binding.aboutAppBar.seslIsCollapsed()) binding.aboutAppBar.setExpanded(true)
                else finish()
            }
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        resetAppBar(newConfig)
    }

    @SuppressLint("RestrictedApi")
    private fun resetAppBar(config: Configuration) {
        ToolbarLayoutUtils.hideStatusBarForLandscape(this, config.orientation)
        ToolbarLayoutUtils.updateListBothSideMargin(this, binding.aboutBottomContainer)
        if (config.orientation != Configuration.ORIENTATION_LANDSCAPE && !isInMultiWindowMode) {
            binding.aboutAppBar.seslSetCustomHeightProportion(true, 0.5f)
            enableBackToHeader = true
            binding.aboutAppBar.addOnOffsetChangedListener(appBarListener)
            binding.aboutAppBar.setExpanded(true, false)
            binding.aboutSwipeUpContainer.visibility = View.VISIBLE
            val lp: ViewGroup.LayoutParams = binding.aboutSwipeUpContainer.layoutParams
            lp.height = resources.displayMetrics.heightPixels / 2
        } else {
            binding.aboutAppBar.setExpanded(false, false)
            enableBackToHeader = false
            binding.aboutAppBar.seslSetCustomHeightProportion(true, 0F)
            binding.aboutAppBar.removeOnOffsetChangedListener(appBarListener)
            binding.aboutBottomContainer.alpha = 1f
            setBottomContentEnabled(true)
            binding.aboutSwipeUpContainer.visibility = View.GONE
        }
    }

    private fun initContent() {
        ViewUtils.semSetRoundedCorners(
            binding.aboutBottomContent.root,
            ViewUtils.SEM_ROUNDED_CORNER_TOP_LEFT or ViewUtils.SEM_ROUNDED_CORNER_TOP_RIGHT
        )
        ViewUtils.semSetRoundedCornerColor(
            binding.aboutBottomContent.root,
            ViewUtils.SEM_ROUNDED_CORNER_TOP_LEFT or ViewUtils.SEM_ROUNDED_CORNER_TOP_RIGHT,
            getColor(dev.oneuiproject.oneui.design.R.color.oui_round_and_bgcolor)
        )
        val appIcon = getDrawable(R.drawable.me4_round)
        binding.aboutHeaderIcon.setImageDrawable(appIcon)
        binding.aboutBottomIcon.setImageDrawable(appIcon)
        binding.aboutHeaderGithub.setOnClickListener(this)
        binding.aboutHeaderWebsite.setOnClickListener(this)
        binding.aboutHeaderPlayStore.setOnClickListener(this)
        binding.aboutHeaderInsta.setOnClickListener(this)
        binding.aboutHeaderTiktok.setOnClickListener(this)
        TooltipCompat.setTooltipText(binding.aboutHeaderGithub, getString(R.string.github))
        bottomContent.aboutBottomRateApp.setOnClickListener(this)
        bottomContent.aboutBottomShareApp.setOnClickListener(this)
        bottomContent.aboutBottomWriteEmail.setOnClickListener(this)
        bottomContent.aboutBottomRelativeTiktok.setOnClickListener(this)
        bottomContent.aboutBottomRelativeWebsite.setOnClickListener(this)
        bottomContent.aboutBottomRelativePlayStore.setOnClickListener(this)
        bottomContent.reviewCommentButton.setOnClickListener(this)
    }

    private fun setBottomContentEnabled(enabled: Boolean) {
        binding.aboutHeaderGithub.isEnabled = !enabled
        binding.aboutHeaderWebsite.isEnabled = !enabled
        binding.aboutHeaderPlayStore.isEnabled = !enabled
        binding.aboutHeaderInsta.isEnabled = !enabled
        binding.aboutHeaderTiktok.isEnabled = !enabled
        bottomContent.aboutBottomRateApp.isEnabled = enabled
        bottomContent.aboutBottomShareApp.isEnabled = enabled
        bottomContent.aboutBottomWriteEmail.isEnabled = enabled
        bottomContent.aboutBottomRelativeTiktok.isEnabled = enabled
        bottomContent.aboutBottomRelativeWebsite.isEnabled = enabled
        bottomContent.aboutBottomRelativePlayStore.isEnabled = enabled
    }

    override fun onClick(v: View) {
        val uptimeMillis = SystemClock.uptimeMillis()
        if (uptimeMillis - lastClickTime > 600L) {
            when (v.id) {
                binding.aboutHeaderWebsite.id, bottomContent.aboutBottomRelativeWebsite.id -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.my_website))))
                }
                binding.aboutHeaderGithub.id -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.my_github))))
                }
                binding.aboutHeaderPlayStore.id, bottomContent.aboutBottomRelativePlayStore.id -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.playstore_developer_page_link))))
                }
                binding.aboutHeaderTiktok.id, bottomContent.aboutBottomRelativeTiktok.id -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.rick_roll_troll_link))))
                }
                binding.aboutHeaderInsta.id -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.my_insta))))
                }
                bottomContent.aboutBottomWriteEmail.id -> {
                    val intent = Intent(Intent.ACTION_SENDTO)
                    intent.data = Uri.parse("mailto:") // only email apps should handle this
                    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.email)))
                    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                    intent.putExtra(Intent.EXTRA_TEXT, "")
                    try {
                        startActivity(intent)
                    } catch (ex: ActivityNotFoundException) {
                        Toast.makeText(this@AboutMeActivity, getString(R.string.no_email_app_installed), Toast.LENGTH_SHORT).show()
                    }
                }
                bottomContent.aboutBottomRateApp.id -> {
                    val manager = ReviewManagerFactory.create(this@AboutMeActivity)
                    //val manager = FakeReviewManager(context);
                    val request = manager.requestReviewFlow()
                    request.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val reviewInfo = task.result
                            val flow = manager.launchReviewFlow(this, reviewInfo)
                            flow.addOnCompleteListener { task2 ->
                                if (task2.isSuccessful) Log.d("AboutActivity", "Reviewtask was successful")
                                else Toast.makeText(this@AboutMeActivity, getString(R.string.error) + ": " + task2.exception, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // There was some problem, log or handle the error code.
                            Toast.makeText(this@AboutMeActivity, R.string.task_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                bottomContent.aboutBottomShareApp.id -> {
                    val sendIntent = Intent(Intent.ACTION_SEND)
                    sendIntent.type = "text/plain"
                    sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_text) + packageName)
                    sendIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.share_app))
                    startActivity(Intent.createChooser(sendIntent, "Share Via"))
                }
                bottomContent.reviewCommentButton.id -> {
                    AlertDialog.Builder(this@AboutMeActivity)
                        .setTitle(getString(R.string.write_review))
                        .setMessage(getString(R.string.review_comment))
                        .setNeutralButton(R.string.ok, null)
                        .setPositiveButton(R.string.to_play_store) { _, _ -> openApp(packageName, false) }
                        .show()
                }
            }
        }
        lastClickTime = uptimeMillis
    }

    // kang from com.sec.android.app.launcher
    private inner class AboutMeAppBarListener : OnOffsetChangedListener {
        override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
            // Handle the SwipeUp anim view
            val totalScrollRange = appBarLayout.totalScrollRange
            val abs = abs(verticalOffset)
            if (abs >= totalScrollRange / 2) {
                binding.aboutSwipeUpContainer.alpha = 0f
                setBottomContentEnabled(true)
            } else if (abs == 0) {
                binding.aboutSwipeUpContainer.alpha = 1f
                setBottomContentEnabled(false)
            } else {
                val offsetAlpha = appBarLayout.y / totalScrollRange
                var arrowAlpha = 1 - offsetAlpha * -3
                if (arrowAlpha < 0) {
                    arrowAlpha = 0f
                } else if (arrowAlpha > 1) {
                    arrowAlpha = 1f
                }
                binding.aboutSwipeUpContainer.alpha = arrowAlpha
            }

            // Handle the bottom part of the UI
            val alphaRange: Float = binding.aboutCtl.height * 0.143f
            val layoutPosition = abs(appBarLayout.top).toFloat()
            var bottomAlpha: Float = (150.0f / alphaRange * (layoutPosition - binding.aboutCtl.height * 0.35f))
            if (bottomAlpha < 0) {
                bottomAlpha = 0f
            } else if (bottomAlpha >= 255) {
                bottomAlpha = 255f
            }
            binding.aboutBottomContainer.alpha = bottomAlpha / 255
        }
    }
}