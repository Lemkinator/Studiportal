package de.lemke.studiportal.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import android.widget.SectionIndexer
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import de.lemke.studiportal.R
import de.lemke.studiportal.databinding.FragmentSearchBinding
import de.lemke.studiportal.domain.GetSearchListUseCase
import de.lemke.studiportal.domain.GetUserSettingsUseCase
import de.lemke.studiportal.domain.MakeSectionOfTextBoldUseCase
import de.lemke.studiportal.domain.model.Exam
import de.lemke.studiportal.ui.Refreshable
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.utils.internal.ReflectUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class MainActivitySearchFragment : Fragment(), Refreshable {
    private lateinit var binding: FragmentSearchBinding
    private lateinit var searchList: MutableList<Exam>
    private lateinit var search: String
    private var initListJob: Job? = null

    @Inject
    lateinit var getUserSettings: GetUserSettingsUseCase

    @Inject
    lateinit var getSearchList: GetSearchListUseCase

    @Inject
    lateinit var makeSectionOfTextBold: MakeSectionOfTextBoldUseCase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onRefresh()
        requireActivity().findViewById<ToolbarLayout>(R.id.toolbar_layout_main).appBarLayout.addOnOffsetChangedListener { layout: AppBarLayout, verticalOffset: Int ->
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
        searchList = mutableListOf()
        if (searchList.isEmpty()) {
            binding.searchList.visibility = View.GONE
            binding.noEntryScrollView.visibility = View.VISIBLE
            binding.noEntryLottie.cancelAnimation()
            binding.noEntryLottie.progress = 0f
            binding.noEntryLottie.postDelayed({ binding.noEntryLottie.playAnimation() }, 400)
        } else {
            binding.noEntryScrollView.visibility = View.GONE
            binding.searchList.visibility = View.VISIBLE
        }
        binding.searchList.adapter = ExamAdapter()
        binding.searchList.layoutManager = LinearLayoutManager(context)
        binding.searchList.addItemDecoration(ItemDecoration(requireContext()))
        binding.searchList.itemAnimator = null
        binding.searchList.seslSetFastScrollerEnabled(true)
        binding.searchList.seslSetFillBottomEnabled(true)
        binding.searchList.seslSetGoToTopEnabled(true)
        binding.searchList.seslSetLastRoundedCorner(true)
    }

    inner class ExamAdapter : RecyclerView.Adapter<ExamAdapter.ViewHolder>(), SectionIndexer {
        private var sections: MutableList<String> = mutableListOf()
        private var positionForSection: MutableList<Int> = mutableListOf()
        private var sectionForPosition: MutableList<Int> = mutableListOf()
        override fun getSections(): Array<Any> = sections.toTypedArray()
        override fun getPositionForSection(i: Int): Int = positionForSection.getOrElse(i) { 0 }
        override fun getSectionForPosition(i: Int): Int = sectionForPosition.getOrElse(i) { 0 }
        override fun getItemCount(): Int = searchList.size
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getItemViewType(position: Int): Int = 0
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            var resId = 0
            when (viewType) {
                0 -> resId = R.layout.listview_item
                //1 -> resId = R.layout.listview_bottom_spacing
            }
            return ViewHolder(LayoutInflater.from(parent.context).inflate(resId, parent, false), viewType)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (holder.isItem) {
                holder.textView.text = searchList[position].name

            }
        }

        inner class ViewHolder internal constructor(itemView: View, viewType: Int) : RecyclerView.ViewHolder(itemView) {
            var isItem: Boolean = viewType == 0
            lateinit var parentView: RelativeLayout
            lateinit var textView: TextView

            init {
                if (isItem) {
                    parentView = itemView as RelativeLayout
                }
            }
        }

        init {
            for (i in searchList.indices) {
                sections.add(searchList[i].name.substring(0, 1))
                positionForSection.add(i)
                sectionForPosition.add(sections.size)
            }
        }
    }

    private class ItemDecoration(context: Context) : RecyclerView.ItemDecoration() {
        private val divider: Drawable
        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            super.onDraw(c, parent, state)
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val top = (child.bottom + (child.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin)
                val bottom = divider.intrinsicHeight + top
                divider.setBounds(parent.left, top, parent.right, bottom)
                divider.draw(c)
            }
        }

        init {
            val outValue = TypedValue()
            context.theme.resolveAttribute(androidx.appcompat.R.attr.isLightTheme, outValue, true)
            divider = context.getDrawable(
                if (outValue.data == 0) androidx.appcompat.R.drawable.sesl_list_divider_dark
                else androidx.appcompat.R.drawable.sesl_list_divider_light
            )!!
        }
    }
}