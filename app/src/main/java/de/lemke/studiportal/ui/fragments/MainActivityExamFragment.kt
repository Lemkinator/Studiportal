package de.lemke.studiportal.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import de.lemke.studiportal.R
import de.lemke.studiportal.databinding.FragmentExamBinding
import de.lemke.studiportal.domain.*
import de.lemke.studiportal.domain.model.Exam
import de.lemke.studiportal.domain.utils.ExamAdapter
import de.lemke.studiportal.domain.utils.ItemDecoration
import dev.oneuiproject.oneui.layout.DrawerLayout
import dev.oneuiproject.oneui.utils.internal.ReflectUtils
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import javax.inject.Inject
import kotlin.math.abs


@AndroidEntryPoint
class MainActivityExamFragment : Fragment() {
    private lateinit var binding: FragmentExamBinding
    private lateinit var exams: MutableList<Pair<Exam?, String>>
    private lateinit var examAdapter: ExamAdapter
    private lateinit var toolbarLayout: DrawerLayout

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


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentExamBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbarLayout = requireActivity().findViewById(R.id.drawer_layout_main)
        toolbarLayout.appBarLayout.addOnOffsetChangedListener { layout: AppBarLayout, verticalOffset: Int ->
            val totalScrollRange = layout.totalScrollRange
            val inputMethodWindowVisibleHeight = ReflectUtils.genericInvokeMethod(
                InputMethodManager::class.java,
                requireActivity().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE),
                "getInputMethodWindowVisibleHeight"
            ) as Int
            if (totalScrollRange != 0) binding.examNoEntryView.translationY = (abs(verticalOffset) - totalScrollRange).toFloat() / 2.0f
            else binding.examNoEntryView.translationY = (abs(verticalOffset) - inputMethodWindowVisibleHeight).toFloat() / 2.0f
        }
        lifecycleScope.launch {
            val userSettings = getUserSettings()
            initList()
            setSubtitle(userSettings.lastRefresh)
            if (userSettings.username == demo.username) toolbarLayout.setTitle(getString(R.string.app_name) + " (Demo)")
            else toolbarLayout.setTitle(getString(R.string.app_name))
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                val userSettings = getUserSettings()
                if (userSettings.username == demo.username) {
                    if (demo.updateExams(true)) {
                        initList()
                        binding.swipeRefreshLayout.isRefreshing = false
                    } else {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(requireContext(), getString(R.string.no_change), Toast.LENGTH_SHORT).show()
                    }
                    setSubtitle(userSettings.lastRefresh)
                } else getStudiportalData(
                    successCallback = { exams ->
                        lifecycleScope.launch {
                            if (updateExams(exams, false)) {
                                initList(getExamsWithSeparator(exams))
                                binding.swipeRefreshLayout.isRefreshing = false
                            } else {
                                binding.swipeRefreshLayout.isRefreshing = false
                                Toast.makeText(requireContext(), getString(R.string.no_change), Toast.LENGTH_SHORT).show()
                            }
                            setSubtitle(userSettings.lastRefresh)
                        }
                    },
                    errorCallback = { message ->
                        binding.swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
        binding.buttonFilter.setOnClickListener {
            //TODO
        }
    }

    private fun setSubtitle(localDateTime: LocalDateTime) {
        val lastRefresh = getString(
            R.string.last_updated,
            localDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)) //TODO
        )
        toolbarLayout.setExpandedSubtitle(lastRefresh)
        toolbarLayout.setCollapsedSubtitle(lastRefresh)
    }

    private suspend fun initList(newExams: MutableList<Pair<Exam?, String>>? = null) {
        if (!this::binding.isInitialized) return
        exams = newExams ?: getExamsWithSeparator(getExams())
        val categories = exams.mapNotNull { it.first?.category }.distinct().toMutableList()
        categories.add(0, getString(R.string.all))
        var categoryFilter = getUserSettings().categoryFilter
        if (!exams.any { it.second == categoryFilter }) categoryFilter = getString(R.string.all)
        updateUserSettings { it.copy(categoryFilter = categoryFilter) }
        if (categoryFilter != getString(R.string.all)) exams =
            exams.filter { it.second == categoryFilter || it.first?.category == categoryFilter }.toMutableList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(dev.oneuiproject.oneui.design.R.layout.support_simple_spinner_dropdown_item)
        binding.examListCategorySpinner.adapter = adapter
        binding.examListCategorySpinner.setSelection(categories.indexOf(categoryFilter))
        binding.examListCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                lifecycleScope.launch {
                    if (categories[position] != getUserSettings().categoryFilter) {
                        updateUserSettings { it.copy(categoryFilter = categories[position]) }
                        initList()
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        if (exams.isEmpty()) {
            Log.d("MainActivityExamFragment", "initList: no exams")
            binding.examListLayout.visibility = View.GONE
            binding.examListLottie.cancelAnimation()
            binding.examListLottie.progress = 0f
            binding.examNoEntryScrollView.visibility = View.VISIBLE
            binding.examListLottie.postDelayed({ binding.examListLottie.playAnimation() }, 400)
        } else {
            Log.d("MainActivityExamFragment", "initList: ${exams.size} exams")
            binding.examNoEntryScrollView.visibility = View.GONE
            binding.examListLayout.visibility = View.VISIBLE
        }
        binding.examList.layoutManager = LinearLayoutManager(context)
        examAdapter = ExamAdapter(requireContext(), exams)
        binding.examList.adapter = examAdapter
        binding.examList.itemAnimator = null
        binding.examList.addItemDecoration(ItemDecoration(requireContext(), binding.examList))
        binding.examList.seslSetFastScrollerEnabled(true)
        binding.examList.seslSetFillBottomEnabled(true)
        binding.examList.seslSetGoToTopEnabled(true)
        binding.examList.seslSetLastRoundedCorner(true)
        binding.examList.seslSetSmoothScrollEnabled(true)
    }
}