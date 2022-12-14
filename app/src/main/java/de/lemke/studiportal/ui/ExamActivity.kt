package de.lemke.studiportal.ui

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import de.lemke.studiportal.R
import de.lemke.studiportal.databinding.ActivityExamBinding
import de.lemke.studiportal.domain.GetExamUseCase
import de.lemke.studiportal.domain.model.Exam
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ExamActivity : AppCompatActivity(R.layout.activity_main) {
    private lateinit var binding: ActivityExamBinding
    private lateinit var exam: Exam
    private lateinit var examInfoList: List<Pair<String, String>>

    @Inject
    lateinit var getExam: GetExamUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbarLayout.setNavigationButtonTooltip(getString(R.string.sesl_navigate_up))
        binding.toolbarLayout.setNavigationButtonOnClickListener { finish() }
        val examNumber = intent.getStringExtra("examNumber")
        lifecycleScope.launch {
            val nullableExam = getExam(examNumber)
            if (nullableExam == null) {
                Toast.makeText(this@ExamActivity, getString(R.string.exam_not_found), Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            exam = nullableExam
            binding.toolbarLayout.setTitle(exam.name)
            examInfoList = exam.getInfoPairList(this@ExamActivity, false)
            initList()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_share, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_share -> {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, exam.name + "\n" +
                        exam.getSubtitle1(this) + "\n" + (exam.getSubtitle2(this)))
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initList() {
        binding.examInfoList.layoutManager = LinearLayoutManager(this)
        binding.examInfoList.adapter = ExamInfoAdapter()
        binding.examInfoList.itemAnimator = null
        binding.examInfoList.addItemDecoration(ItemDecoration(this))
        binding.examInfoList.seslSetFastScrollerEnabled(true)
        binding.examInfoList.seslSetFillBottomEnabled(true)
        binding.examInfoList.seslSetGoToTopEnabled(true)
        binding.examInfoList.seslSetLastRoundedCorner(true)
        binding.examInfoList.seslSetSmoothScrollEnabled(true)
    }

    inner class ExamInfoAdapter : RecyclerView.Adapter<ExamInfoAdapter.ViewHolder>() {
        override fun getItemCount(): Int = examInfoList.size
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getItemViewType(position: Int): Int = 0
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.exam_info_listview_item, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textViewStart.text = examInfoList[position].first
            holder.textViewEnd.text = examInfoList[position].second
        }

        inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private var parentView: LinearLayout
            var textViewStart: TextView
            var textViewEnd: TextView

            init {
                parentView = itemView as LinearLayout
                textViewStart = parentView.findViewById(R.id.text_view_start)
                textViewEnd = parentView.findViewById(R.id.text_view_end)
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