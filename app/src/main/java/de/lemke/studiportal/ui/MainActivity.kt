package de.lemke.studiportal.ui

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import de.lemke.studiportal.R
import de.lemke.studiportal.data.UserSettings
import de.lemke.studiportal.databinding.ActivityMainBinding
import de.lemke.studiportal.domain.*
import de.lemke.studiportal.domain.model.Exam
import de.lemke.studiportal.domain.utils.ExamAdapter
import de.lemke.studiportal.domain.utils.ItemDecoration
import dev.oneuiproject.oneui.layout.DrawerLayout
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.utils.internal.ReflectUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private lateinit var binding: ActivityMainBinding
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private lateinit var onBackInvokedCallback: OnBackInvokedCallback
    private lateinit var exams: MutableList<Pair<Exam?, String>>
    private var isSearchUserInputEnabled = false
    private var time: Long = 0
    private var initListJob: Job? = null
    private var isUIReady = false

    @Inject
    lateinit var getUserSettings: GetUserSettingsUseCase

    @Inject
    lateinit var updateUserSettings: UpdateUserSettingsUseCase

    @Inject
    lateinit var getStudiportalData: GetStudiportalDataUseCase

    @Inject
    lateinit var getExams: GetExamsUseCase

    @Inject
    lateinit var updateExams: UpdateExamsUseCase

    @Inject
    lateinit var demo: DemoUseCase

    @Inject
    lateinit var getExamsWithSeparator: AddSeparatorToExamsUseCase

    @Inject
    lateinit var getSearchList: GetSearchListUseCase

    @Inject
    lateinit var addSeparatorToExams: AddSeparatorToExamsUseCase

    @Inject
    lateinit var checkAppStart: CheckAppStartUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        /*  Note: https://stackoverflow.com/a/69831106/18332741
        On Android 12 just running the app via android studio doesn't show the full splash screen.
        You have to kill it and open the app from the launcher.
        */
        val splashScreen = installSplashScreen()
        time = System.currentTimeMillis()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        splashScreen.setKeepOnScreenCondition { !isUIReady }
        /*
        there is a bug in the new splash screen api, when using the onExitAnimationListener -> splash icon flickers
        therefore setting a manual delay in openMain()
        splashScreen.setOnExitAnimationListener { splash ->
            val splashAnimator: ObjectAnimator = ObjectAnimator.ofPropertyValuesHolder(
                splash.view,
                PropertyValuesHolder.ofFloat(View.ALPHA, 0f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1.2f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.2f)
            )
            splashAnimator.interpolator = AccelerateDecelerateInterpolator()
            splashAnimator.duration = 400L
            splashAnimator.doOnEnd { splash.remove() }
            val contentAnimator: ObjectAnimator = ObjectAnimator.ofPropertyValuesHolder(
                binding.root,
                PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1.2f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.2f, 1f)
            )
            contentAnimator.interpolator = AccelerateDecelerateInterpolator()
            contentAnimator.duration = 400L

            val remainingDuration = splash.iconAnimationDurationMillis - (System.currentTimeMillis() - splash.iconAnimationStartMillis)
                .coerceAtLeast(0L)
            lifecycleScope.launch {
                delay(remainingDuration)
                splashAnimator.start()
                contentAnimator.start()
            }
        }*/

        lifecycleScope.launch {
            when (checkAppStart()) {
                AppStart.FIRST_TIME -> openOOBE()
                AppStart.NORMAL -> checkTOS(getUserSettings())
                AppStart.FIRST_TIME_VERSION -> checkTOS(getUserSettings())
            }
        }
    }

    private suspend fun openOOBE() {
        //manually waiting for the animation to finish :/
        delay(700 - (System.currentTimeMillis() - time).coerceAtLeast(0L))
        startActivity(Intent(applicationContext, OOBEActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private suspend fun checkTOS(userSettings: UserSettings) {
        if (!userSettings.tosAccepted) openOOBE()
        else checkLogin(userSettings)
    }

    private suspend fun checkLogin(userSettings: UserSettings) {
        if (userSettings.username.isBlank()) {
            //manually waiting for the animation to finish :/
            delay(700 - (System.currentTimeMillis() - time).coerceAtLeast(0L))
            startActivity(Intent(applicationContext, LoginActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        } else openMain()
    }

    private fun openMain() {
        lifecycleScope.launch {
            initOnBackPressed()
            initDrawer()
            initList()
            binding.swipeRefreshLayout.setOnRefreshListener { lifecycleScope.launch { refresh() } }
            //manually waiting for the animation to finish :/
            delay(700 - (System.currentTimeMillis() - time).coerceAtLeast(0L))
            isUIReady = true
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            setSubtitle(getUserSettings().lastRefresh)
        }
    }

    override fun onPause() {
        super.onPause()
        binding.drawerLayoutMain.setDrawerOpen(false, true)
    }

    private fun initOnBackPressed() {
        //set custom callback to prevent app from exiting on back press when in search mode
        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                checkBackPressed()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedCallback = OnBackInvokedCallback { checkBackPressed() }
        }
    }

    private fun checkBackPressed() {
        if (binding.drawerLayoutMain.isSearchMode) {
            isSearchUserInputEnabled = false
            binding.drawerLayoutMain.dismissSearchMode()
        } else finishAffinity()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.action == Intent.ACTION_SEARCH) binding.drawerLayoutMain.searchView.setQuery(
            intent.getStringExtra(SearchManager.QUERY),
            true
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_search -> {
                binding.drawerLayoutMain.showSearchMode()
                return true
            }
            R.id.menu_item_open_online -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.studiportal_url))))
                return true
            }
            R.id.menu_item_refresh_now -> {
                lifecycleScope.launch { if (!binding.swipeRefreshLayout.isRefreshing) refresh() }
                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }

    inner class SearchModeListener : ToolbarLayout.SearchModeListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            if (!isSearchUserInputEnabled) return false
            lifecycleScope.launch {
                updateUserSettings { it.copy(search = query ?: "") }
                setSearchList(query ?: "")
            }
            return true
        }

        override fun onQueryTextChange(query: String?): Boolean {
            if (!isSearchUserInputEnabled) return false
            lifecycleScope.launch {
                updateUserSettings { it.copy(search = query ?: "") }
                setSearchList(query ?: "")
            }
            return true
        }

        override fun onSearchModeToggle(searchView: SearchView, visible: Boolean) {
            lifecycleScope.launch {
                if (visible) {
                    onBackPressedCallback.isEnabled = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        onBackInvokedDispatcher.registerOnBackInvokedCallback(
                            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                            onBackInvokedCallback
                        )
                    }
                    isSearchUserInputEnabled = true
                    val search = getUserSettings().search
                    searchView.setQuery(search, false)
                    val autoCompleteTextView = searchView.seslGetAutoCompleteView()
                    autoCompleteTextView.setText(search)
                    autoCompleteTextView.setSelection(autoCompleteTextView.text.length)
                    setSearchList(search)
                } else {
                    onBackPressedCallback.isEnabled = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        onBackInvokedDispatcher.unregisterOnBackInvokedCallback(onBackInvokedCallback)
                    }
                    isSearchUserInputEnabled = false
                    exams = getExamsWithSeparator(getExams())
                    initList()
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private suspend fun initDrawer() {
        val userSettings = getUserSettings()
        setSubtitle(userSettings.lastRefresh)
        if (userSettings.username == demo.username) binding.drawerLayoutMain.setTitle(getString(R.string.app_name) + " (Demo)")
        else binding.drawerLayoutMain.setTitle(getString(R.string.app_name))
        val helpOption = findViewById<LinearLayout>(R.id.draweritem_help)
        val aboutAppOption = findViewById<LinearLayout>(R.id.draweritem_about_app)
        val aboutMeOption = findViewById<LinearLayout>(R.id.draweritem_about_me)
        val settingsOption = findViewById<LinearLayout>(R.id.draweritem_settings)

        helpOption.setOnClickListener {
            startActivity(Intent(this@MainActivity, HelpActivity::class.java))
            binding.drawerLayoutMain.setDrawerOpen(false, true)
        }
        aboutAppOption.setOnClickListener {
            startActivity(Intent(this@MainActivity, AboutActivity::class.java))
            binding.drawerLayoutMain.setDrawerOpen(false, true)
        }
        aboutMeOption.setOnClickListener {
            startActivity(Intent(this@MainActivity, AboutMeActivity::class.java))
            binding.drawerLayoutMain.setDrawerOpen(false, true)
        }
        settingsOption.setOnClickListener {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            binding.drawerLayoutMain.setDrawerOpen(false, true)
        }
        binding.drawerLayoutMain.setDrawerButtonIcon(getDrawable(dev.oneuiproject.oneui.R.drawable.ic_oui_info_outline))
        binding.drawerLayoutMain.setDrawerButtonOnClickListener {
            startActivity(
                Intent().setClass(this@MainActivity, AboutActivity::class.java)
            )
        }
        binding.drawerLayoutMain.setDrawerButtonTooltip(getText(R.string.about_app))
        binding.drawerLayoutMain.setSearchModeListener(SearchModeListener())
        binding.drawerLayoutMain.searchView.setSearchableInfo(
            (getSystemService(SEARCH_SERVICE) as SearchManager).getSearchableInfo(componentName)
        )
        AppUpdateManagerFactory.create(this).appUpdateInfo.addOnSuccessListener { appUpdateInfo: AppUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE)
                binding.drawerLayoutMain.setButtonBadges(ToolbarLayout.N_BADGE, DrawerLayout.N_BADGE)
        }
        binding.drawerLayoutMain.appBarLayout.addOnOffsetChangedListener { layout: AppBarLayout, verticalOffset: Int ->
            val totalScrollRange = layout.totalScrollRange
            val inputMethodWindowVisibleHeight = ReflectUtils.genericInvokeMethod(
                InputMethodManager::class.java,
                getSystemService(INPUT_METHOD_SERVICE),
                "getInputMethodWindowVisibleHeight"
            ) as Int
            if (totalScrollRange != 0) binding.examNoEntryView.translationY = (abs(verticalOffset) - totalScrollRange).toFloat() / 2.0f
            else binding.examNoEntryView.translationY = (abs(verticalOffset) - inputMethodWindowVisibleHeight).toFloat() / 2.0f
        }
    }

    private suspend fun refresh() {
        binding.swipeRefreshLayout.isRefreshing = true
        val userSettings = getUserSettings()
        if (userSettings.username == demo.username) {
            if (demo.updateDemoExams()) initList()
            else Toast.makeText(this@MainActivity, getString(R.string.no_change), Toast.LENGTH_SHORT).show()
            binding.swipeRefreshLayout.isRefreshing = false
            setSubtitle(userSettings.lastRefresh)
        } else getStudiportalData(
            successCallback = { exams ->
                lifecycleScope.launch {
                    if (updateExams(exams)) initList(getExamsWithSeparator(exams))
                    else Toast.makeText(this@MainActivity, getString(R.string.no_change), Toast.LENGTH_SHORT).show()
                    binding.swipeRefreshLayout.isRefreshing = false
                    setSubtitle(userSettings.lastRefresh)
                }
            },
            errorCallback = { message ->
                lifecycleScope.launch {
                    binding.swipeRefreshLayout.isRefreshing = false
                    if (message == getString(R.string.no_internet))
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    else AlertDialog.Builder(this@MainActivity)
                        .setTitle(getString(R.string.error))
                        .setMessage(message)
                        .setPositiveButton(R.string.ok, null)
                        .create()
                        .show()
                }
            }
        )
    }

    private fun setSubtitle(zonedDateTime: ZonedDateTime?) {
        val lastRefresh = getString(
            R.string.last_updated,
            if (zonedDateTime == null) getString(R.string.never)
            else zonedDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
        )
        binding.drawerLayoutMain.setExpandedSubtitle(lastRefresh)
        binding.drawerLayoutMain.setCollapsedSubtitle(lastRefresh)
    }

    fun setSearchList(search: String?) {
        initListJob?.cancel()
        initListJob = lifecycleScope.launch { initList(search = search) }
    }

    private suspend fun initList(newExams: MutableList<Pair<Exam?, String>>? = null, search: String? = null) {
        if (!this::binding.isInitialized) return
        if (search == null) {
            exams = newExams ?: getExamsWithSeparator(getExams())
            val categories = exams.mapNotNull { it.first?.category }.distinct().toMutableList()
            categories.add(0, getString(R.string.all))
            var categoryFilter = getUserSettings().categoryFilter
            if (!exams.any { it.second == categoryFilter }) categoryFilter = getString(R.string.all)
            updateUserSettings { it.copy(categoryFilter = categoryFilter) }
            if (categoryFilter != getString(R.string.all)) exams =
                exams.filter { it.second == categoryFilter || it.first?.category == categoryFilter }.toMutableList()

            val categoryList = findViewById<RecyclerView>(R.id.drawer_category_list)
            categoryList.layoutManager = LinearLayoutManager(this)
            categoryList.adapter = CategoryAdapter(categories, categoryFilter)
        } else exams = addSeparatorToExams(getSearchList(search)).filter { it.first != null }.toMutableList()
        initRecycler(search)
    }

    inner class CategoryAdapter(private val categories: List<String>, private val categoryFilter: String) :
        RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryAdapter.ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.category_listview_item, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = categories[position]
            holder.icon.setImageDrawable(getDrawable(Exam.getCategoryIconResource(this@MainActivity, categories[position])))
            holder.textView.setOnClickListener {
                lifecycleScope.launch {
                    if (categories[position] != getUserSettings().categoryFilter) {
                        updateUserSettings { it.copy(categoryFilter = categories[position]) }
                        initList()
                        binding.drawerLayoutMain.setDrawerOpen(false, true)
                    }
                }
            }
            holder.parentView.isSelected = categories[position] == categoryFilter
        }

        override fun getItemCount(): Int {
            return categories.size
        }

        inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var parentView: LinearLayout
            var textView: TextView
            var icon: ImageView

            init {
                parentView = itemView as LinearLayout
                textView = parentView.findViewById(R.id.drawer_item_text_view)
                icon = parentView.findViewById(R.id.drawer_item_icon)
            }
        }
    }

    private fun initRecycler(search: String?) {
        if (exams.isEmpty()) {
            binding.examList.visibility = View.GONE
            binding.examListLottie.cancelAnimation()
            binding.examListLottie.progress = 0f
            binding.examNoEntryScrollView.visibility = View.VISIBLE
            binding.examListLottie.postDelayed({ binding.examListLottie.playAnimation() }, 400)
        } else {
            binding.examNoEntryScrollView.visibility = View.GONE
            binding.examList.visibility = View.VISIBLE
        }
        binding.examList.layoutManager = LinearLayoutManager(this)
        binding.examList.adapter = ExamAdapter(this, exams, search)
        binding.examList.itemAnimator = null
        binding.examList.addItemDecoration(ItemDecoration(this, binding.examList))
        binding.examList.seslSetFastScrollerEnabled(true)
        binding.examList.seslSetFillBottomEnabled(true)
        binding.examList.seslSetGoToTopEnabled(true)
        binding.examList.seslSetLastRoundedCorner(true)
        binding.examList.seslSetSmoothScrollEnabled(true)
    }
}