package de.lemke.studiportal.ui

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import de.lemke.studiportal.R
import de.lemke.studiportal.databinding.ActivityMainBinding
import de.lemke.studiportal.domain.*
import de.lemke.studiportal.domain.model.Exam
import de.lemke.studiportal.domain.utils.ExamAdapter
import de.lemke.studiportal.domain.utils.ItemDecoration
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.utils.internal.ReflectUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {
    companion object {
        var refreshView = false
    }

    private lateinit var binding: ActivityMainBinding
    private var isSearchUserInputEnabled = false
    private var time: Long = 0
    private lateinit var exams: MutableList<Pair<Exam?, String>>
    private var initListJob: Job? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        time = System.currentTimeMillis()
        lifecycleScope.launch {
            initDrawer()
            initList()
        }
        binding.swipeRefreshLayout.setOnRefreshListener { lifecycleScope.launch { refresh() } }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                lifecycleScope.launch {
                    when {
                        binding.drawerLayoutMain.isSearchMode -> {
                            isSearchUserInputEnabled = false
                            binding.drawerLayoutMain.dismissSearchMode()
                        }
                        !getUserSettings().confirmExit || System.currentTimeMillis() - time < 3000 -> finishAffinity()
                        else -> {
                            Toast.makeText(this@MainActivity, resources.getString(R.string.press_again_to_exit), Toast.LENGTH_SHORT).show()
                            time = System.currentTimeMillis()
                        }
                    }
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.action == Intent.ACTION_SEARCH) binding.drawerLayoutMain.searchView.setQuery(
            intent.getStringExtra(SearchManager.QUERY),
            true
        )
    }

    override fun onResume() {
        super.onResume()
        if (refreshView) {
            refreshView = false
            recreate()
        }
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
                lifecycleScope.launch { refresh() }
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
                    isSearchUserInputEnabled = true
                    val search = getUserSettings().search
                    searchView.setQuery(search, false)
                    val autoCompleteTextView = searchView.seslGetAutoCompleteView()
                    autoCompleteTextView.setText(search)
                    autoCompleteTextView.setSelection(autoCompleteTextView.text.length)
                    setSearchList(search)
                } else {
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
        val aboutAppOption = findViewById<LinearLayout>(R.id.draweritem_about_app)
        val aboutMeOption = findViewById<LinearLayout>(R.id.draweritem_about_me)
        val settingsOption = findViewById<LinearLayout>(R.id.draweritem_settings)
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
                Intent().setClass(
                    this@MainActivity,
                    AboutActivity::class.java
                )
            )
        }
        binding.drawerLayoutMain.setDrawerButtonTooltip(getText(R.string.about_app))
        binding.drawerLayoutMain.setSearchModeListener(SearchModeListener())
        binding.drawerLayoutMain.searchView.setSearchableInfo(
            (getSystemService(SEARCH_SERVICE) as SearchManager).getSearchableInfo(componentName)
        )
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
            if (demo.updateExams(userSettings.notificationsEnabled)) initList()
            else Toast.makeText(this@MainActivity, getString(R.string.no_change), Toast.LENGTH_SHORT).show()
            binding.swipeRefreshLayout.isRefreshing = false
            setSubtitle(userSettings.lastRefresh)
        } else getStudiportalData(
            successCallback = { exams ->
                lifecycleScope.launch {
                    if (updateExams(exams, false)) initList(getExamsWithSeparator(exams))
                    else Toast.makeText(this@MainActivity, getString(R.string.no_change), Toast.LENGTH_SHORT).show()
                    binding.swipeRefreshLayout.isRefreshing = false
                    setSubtitle(userSettings.lastRefresh)
                }
            },
            errorCallback = { message ->
                binding.swipeRefreshLayout.isRefreshing = false
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
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

            init {
                parentView = itemView as LinearLayout
                textView = parentView.findViewById(R.id.drawer_item_text_view)
            }
        }
    }

    private fun initRecycler(search: String?) {
        if (exams.isEmpty()) {
            Log.d("MainActivityExamFragment", "initList: no exams")
            binding.examList.visibility = View.GONE
            binding.examListLottie.cancelAnimation()
            binding.examListLottie.progress = 0f
            binding.examNoEntryScrollView.visibility = View.VISIBLE
            binding.examListLottie.postDelayed({ binding.examListLottie.playAnimation() }, 400)
        } else {
            Log.d("MainActivityExamFragment", "initList: ${exams.size} exams")
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