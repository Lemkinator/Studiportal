package de.lemke.studiportal.ui

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import de.lemke.studiportal.R
import de.lemke.studiportal.databinding.ActivityMainBinding
import de.lemke.studiportal.domain.GetUserSettingsUseCase
import de.lemke.studiportal.domain.UpdateUserSettingsUseCase
import de.lemke.studiportal.ui.fragments.MainActivityExamFragment
import de.lemke.studiportal.ui.fragments.MainActivitySearchFragment
import dev.oneuiproject.oneui.layout.ToolbarLayout
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {
    companion object {
        var refreshView = false
    }

    private lateinit var binding: ActivityMainBinding
    private val examFragment: MainActivityExamFragment = MainActivityExamFragment()
    private val searchFragment: MainActivitySearchFragment = MainActivitySearchFragment()
    private var isSearchFragmentVisible = false
    private var isSearchUserInputEnabled = false
    private var time: Long = 0

    @Inject
    lateinit var getUserSettings: GetUserSettingsUseCase

    @Inject
    lateinit var updateUserSettings: UpdateUserSettingsUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        time = System.currentTimeMillis()
        initFragments()
        binding.toolbarLayoutMain.setSearchModeListener(SearchModeListener())
        binding.toolbarLayoutMain.searchView.setSearchableInfo(
            (getSystemService(SEARCH_SERVICE) as SearchManager).getSearchableInfo(
                componentName
            )
        )
        binding.toolbarLayoutMain.searchView.seslSetOverflowMenuButtonIcon(getDrawable(dev.oneuiproject.oneui.R.drawable.ic_oui_list_filter))
        binding.toolbarLayoutMain.searchView.seslSetOverflowMenuButtonVisibility(View.VISIBLE)
        //binding.toolbarLayoutMain.searchView.seslSetOnOverflowMenuButtonClickListener { SearchFilterDialog { setSearchFragment() }.show(supportFragmentManager, "") }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                lifecycleScope.launch {
                    when {
                        binding.toolbarLayoutMain.isSearchMode -> binding.toolbarLayoutMain.dismissSearchMode()
                        else -> {
                            if (!getUserSettings().confirmExit) finishAffinity()
                            else {
                                if (System.currentTimeMillis() - time < 3000) finishAffinity()
                                else {
                                    Toast.makeText(this@MainActivity, resources.getString(R.string.press_again_to_exit), Toast.LENGTH_SHORT)
                                        .show()
                                    time = System.currentTimeMillis()
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.action == Intent.ACTION_SEARCH) binding.toolbarLayoutMain.searchView.setQuery(
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
                binding.toolbarLayoutMain.showSearchMode()
                setSearchFragment()
                return true
            }
            R.id.menu_item_open_online -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.studiportal_url))))
                return true
            }
            R.id.menu_item_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            R.id.menu_item_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                return true
            }
            R.id.menu_item_about_me -> {
                startActivity(Intent(this, AboutMeActivity::class.java))
                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }

    private fun initFragments() {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragment_container, examFragment)
        transaction.add(R.id.fragment_container, searchFragment)
        transaction.commit()
        supportFragmentManager.executePendingTransactions()
        setExamFragment()
    }

    fun setExamFragment() {
        //if (isSearchFragmentVisible) {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        for (fragment in supportFragmentManager.fragments) transaction.hide(fragment)
        transaction.show(examFragment).commit()
        supportFragmentManager.executePendingTransactions()
        //}
        isSearchFragmentVisible = false
    }

    fun setSearchFragment() {
        if (!isSearchFragmentVisible) {
            val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
            for (fragment in supportFragmentManager.fragments) transaction.hide(fragment)
            transaction.show(searchFragment).commit()
            supportFragmentManager.executePendingTransactions()
        }
        searchFragment.onRefresh()
        isSearchFragmentVisible = true
    }

    inner class SearchModeListener : ToolbarLayout.SearchModeListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            if (!isSearchUserInputEnabled) return false
            lifecycleScope.launch {
                //updateUserSettings { it.copy(search = query ?: "") }
                setSearchFragment()
            }
            return true
        }

        override fun onQueryTextChange(query: String?): Boolean {
            if (!isSearchUserInputEnabled) return false
            lifecycleScope.launch {
                //updateUserSettings { it.copy(search = query ?: "") }
                setSearchFragment()
            }
            return true
        }

        override fun onSearchModeToggle(searchView: SearchView, visible: Boolean) {
            if (visible) {
                isSearchUserInputEnabled = true
                lifecycleScope.launch {
                    val search = "" //getUserSettings().search
                    searchView.setQuery(search, false)
                    val autoCompleteTextView = searchView.seslGetAutoCompleteView()
                    autoCompleteTextView.setText(search)
                    autoCompleteTextView.setSelection(autoCompleteTextView.text.length)
                }
            } else {
                isSearchUserInputEnabled = false
                setExamFragment()
            }
        }
    }


}