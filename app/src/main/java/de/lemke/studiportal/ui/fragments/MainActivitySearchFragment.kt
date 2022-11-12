package de.lemke.studiportal.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import de.lemke.studiportal.R
import de.lemke.studiportal.databinding.FragmentSearchBinding
import de.lemke.studiportal.domain.AddSeparatorToExamsUseCase
import de.lemke.studiportal.domain.GetSearchListUseCase
import de.lemke.studiportal.domain.GetUserSettingsUseCase
import de.lemke.studiportal.domain.model.Exam
import de.lemke.studiportal.domain.utils.ExamAdapter
import de.lemke.studiportal.domain.utils.ItemDecoration
import de.lemke.studiportal.ui.Refreshable
import dev.oneuiproject.oneui.layout.DrawerLayout
import dev.oneuiproject.oneui.utils.internal.ReflectUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class MainActivitySearchFragment : Fragment(), Refreshable {
    private lateinit var binding: FragmentSearchBinding
    private lateinit var searchList: MutableList<Pair<Exam?, String>>
    private lateinit var search: String
    private var initListJob: Job? = null

    @Inject
    lateinit var getUserSettings: GetUserSettingsUseCase

    @Inject
    lateinit var getSearchList: GetSearchListUseCase

    @Inject
    lateinit var addSeparatorToExams: AddSeparatorToExamsUseCase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onRefresh()
        requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout_main).appBarLayout.addOnOffsetChangedListener { layout: AppBarLayout, verticalOffset: Int ->
            val totalScrollRange = layout.totalScrollRange
            val inputMethodWindowVisibleHeight = ReflectUtils.genericInvokeMethod(
                InputMethodManager::class.java,
                requireActivity().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE),
                "getInputMethodWindowVisibleHeight"
            ) as Int
            if (totalScrollRange != 0) binding.noEntryView.translationY = (abs(verticalOffset) - totalScrollRange).toFloat() / 2.0f
            else binding.noEntryView.translationY = (abs(verticalOffset) - inputMethodWindowVisibleHeight).toFloat() / 2.0f
        }
    }

    override fun onRefresh() {
        initListJob?.cancel()
        initListJob = lifecycleScope.launch {
            search = getUserSettings().search
            initList()
        }
    }

    private suspend fun initList() {
        if (!this::binding.isInitialized) return
        searchList = addSeparatorToExams(getSearchList(search)).filter { it.first != null }.toMutableList()
        if (searchList.isEmpty()) {
            binding.searchList.visibility = View.GONE
            binding.noEntryLottie.cancelAnimation()
            binding.noEntryLottie.progress = 0f
            binding.noEntryScrollView.visibility = View.VISIBLE
            binding.noEntryLottie.postDelayed({ binding.noEntryLottie.playAnimation() }, 400)
        } else {
            binding.noEntryScrollView.visibility = View.GONE
            binding.searchList.visibility = View.VISIBLE
        }
        binding.searchList.adapter = ExamAdapter(requireContext(), searchList, search)
        binding.searchList.layoutManager = LinearLayoutManager(context)
        binding.searchList.addItemDecoration(ItemDecoration(requireContext(), binding.searchList))
        binding.searchList.itemAnimator = null
        binding.searchList.seslSetFastScrollerEnabled(true)
        binding.searchList.seslSetFillBottomEnabled(true)
        binding.searchList.seslSetGoToTopEnabled(true)
        binding.searchList.seslSetLastRoundedCorner(true)
    }
}