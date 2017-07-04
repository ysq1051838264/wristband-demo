package com.kingnew.health.base.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.util.SparseBooleanArray
import android.view.View
import android.view.ViewGroup
import org.jetbrains.anko.bottomPadding
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.wrapContent
import java.util.*

/**
 * RecyclerView.ViewHolder转换器,可以创建视图,并且对视图进行初始化,并且响应事件监听
 * Created by hdr on 16/3/23.
 */
abstract class HolderConverter<T : Any> : ViewCreator {
    /**
     * 这个视图转换器所
     */
    lateinit var view: View
    lateinit var viewHolder: AmazingAdapter<T, *>.SectionViewHolder
    var onClickListener: ((data: T, index: Int) -> Unit)? = null

    val context  by lazy { view.context }

    /**
     * 可扩展伸缩标识量
     */
    open val expandable = false

    fun createViewForAdapter(context: Context): View {
        val view = createView(context)
        this.view = view
        return view
    }

    /**
     * 对视图进行初始化的方法
     * @param data 视图当前对应的数据
     * @param index 视图是在数据集的索引值,注意:并非完全等同 RecyclerView 的 position
     */
    abstract fun initData(data: T, index: Int = 0)

    /**
     * 被点击时调用
     * @param data 视图当前对应的数据
     * @param index 视图是在数据集的索引值,注意:并非完全等同 RecyclerView 的 position
     */
    open fun onClick(data: T, index: Int) {
        onClickListener?.invoke(data, index)
    }

    /**
     * 如果这个组是可以展开收缩的,在onViewBind里面会调用这个方法,用来初始化展开或收缩的样式
     * @param state true为展开,false为收缩
     */
    open fun initState(state: Boolean) {
    }

    /**
     * 如果这个组是可以展开收缩的,当用户点击了组视图,这个方法会被调用,用来初始化展开或收缩的样式
     * @param state true为展开,false为收缩
     */
    open fun onStateChange(state: Boolean) {
        initState(state)
    }
}

/**
 * 子Holder转换器类型
 */
abstract class RowHolderConverter<Section, Row> : ViewCreator {
    lateinit var view: View
    lateinit var context: Context
        private set

    fun createViewForAdapter(context: Context): View {
        this.context = context
        val view = createView(context)
        this.view = view
        return view
    }

    abstract fun initData(section: Section, sectionIndex: Int, row: Row, rowIndex: Int)
    open fun onClick(section: Section, sectionIndex: Int, row: Row, rowIndex: Int) {
    }
}

/**
 * 视图构建器
 */
interface ViewCreator {
    fun createView(context: Context): View
}

class ListAdapter<T : Any>(datas: List<T> = emptyList(), holderConverterFactory: () -> HolderConverter<T>) : AmazingAdapter<T, Nothing>(datas, { holderConverterFactory() })

/**
 * 多视图的Adapter
 * Created by hdr on 16/3/23.
 */
open class MultiViewAdapter<T : Any>(datas: List<T> = emptyList(), holderConverterFactory: (Int) -> HolderConverter<out T>, viewTypeFactory: (T, Int) -> Int) :
        AmazingAdapter<T, Nothing>(datas, holderConverterFactory, viewTypeFactory)

/**
 * 带一个标题栏的RecyclerViewAdapter
 * Created by hdr on 16/3/23.
 */
class HeaderViewAdapter<H : Any, T : Any>(headData: H, datas: List<T> = emptyList(), headerHolderConverterFactory: () -> HolderConverter<H>, holderConverterFactory: () -> HolderConverter<T>) :
        AmazingAdapter<Any, Any>(mutableListOf(headData as Any).apply {
            addAll(datas)
        }, { if (it == AmazingAdapter.VIEW_TYPE_SECTION) headerHolderConverterFactory() else holderConverterFactory() },
                { h, i -> if (i == 0) AmazingAdapter.VIEW_TYPE_SECTION else AmazingAdapter.VIEW_TYPE_SECTION + 1 }) {

}


/**
 * 支持多视图,伸缩视图,分组的 adapter
 * @param sections 组数据
 * @param sectionHolderConverterFactory 组Holder工厂
 * @param sectionViewTypeFactory 组视图类型工厂方法
 * @param defaultState 决定这个组是否默认展开
 * @param rowsFactory 子数组
 * @param rowHolderConverterFactory 子Holder工厂
 * @param rowViewTypeFactory 子视图类型工厂
 * <p>
 * Created by hdr on 16/3/24.
 * </p>
 */
open class AmazingAdapter<Section : Any, Row : Any>(sections: List<Section> = emptyList(),
                                                    val sectionHolderConverterFactory: (viewType: Int) -> HolderConverter<out Section>,
                                                    val sectionViewTypeFactory: (Section, Int) -> Int = { section, i -> AmazingAdapter.VIEW_TYPE_SECTION },
                                                    val defaultState: (Section, Int) -> Boolean = { section, i -> true },

                                                    val rowsFactory: (Section, Int) -> List<Row> = { section, i -> emptyList<Row>() },
                                                    val rowHolderConverterFactory: (Int) -> RowHolderConverter<out Section, out Row> = { EmptyRowHolderConverter() },
                                                    val rowViewTypeFactory: (section: Section, sectionIndex: Int, row: Row, rowIndex: Int) -> Int = { s, si, r, ri -> AmazingAdapter.VIEW_TYPE_ROW }) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        val VIEW_TYPE_SECTION = 0
        val VIEW_TYPE_ROW = 1000

        val HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL

        val VERTICAL_LIST = LinearLayoutManager.VERTICAL

        val EMPTY_DIVIDER = Divider()
    }

    var recyclerView: RecyclerView? = null

    val states = SparseBooleanArray()
    val sections = ArrayList<Section>(sections.size).apply {
        this.addAll(sections)
    }

    inline fun rows(sectionIndex: Int): List<Row> = rowsFactory(sections[sectionIndex], sectionIndex)

    init {
        initState()
    }

    fun initState() {
        states.clear()
        sections.forEachIndexed { i, section ->
            states.put(i, defaultState(sections[i], i))
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        this.recyclerView = null
    }

    fun clearIndexCache() {
        indexCache.clear()
        itemCountCache = -1
        initState()
        notifyDataSetChanged()
    }

    fun addSection(section: Section, position: Int = -1) {
        addSections(listOf(section), position)
    }

    /**
     * 往集合中添加
     */
    fun addSections(sections: List<Section>, position: Int = -1, defaultState: (Section, Int) -> Boolean = { section, index -> true }) {
        val _position = if (position >= 0 && position < itemCount) position else itemCount
        val size = sections.size
        sections.forEachIndexed { i, section ->
            states.put(this.sections.size + i, defaultState(section, i))
        }
        this.sections.addAll(_position, sections)
        itemCountCache = -1
        notifyItemRangeInserted(_position, size)
    }

    fun updateSection(section: Section) {
        val index = sections.indexOf(section)
        if (index < 0) {
            return
        }
        updateSection(index, section)
    }

    fun updateSection(index: Int, section: Section) {
        sections[index] = section
        val sectionPosition = getSectionPosition(index)
        if (sectionPosition < 0 || sectionPosition >= itemCount) {
            return
        }
        remoteCacheAtRange(sectionPosition + 1, itemCount - sectionPosition + 1)
        itemCountCache = -1
        notifyItemChanged(sectionPosition)
    }

    fun updateSection(index: Int) {
        val sectionPosition = getSectionPosition(index)
        if (sectionPosition < 0 || sectionPosition >= itemCount) {
            return
        }
        remoteCacheAtRange(sectionPosition + 1, itemCount - sectionPosition + 1)
        itemCountCache = -1
        notifyItemChanged(sectionPosition)
    }

    fun remoteSection(section: Section) {
        val index = sections.indexOf(section)
        if (index >= 0)
            remoteSection(index)
    }

    fun remoteSection(index: Int) {
        val sectionPosition = getSectionPosition(index)
        if (sectionPosition < 0 || sectionPosition >= itemCount) {
            return
        }
        remoteCacheAtRange(sectionPosition + 1, itemCount - sectionPosition + 1)
        val changeCount = if (states[index]) rowsFactory(sections[index], index).size + 1 else 1
        this.sections.removeAt(index)
        itemCountCache = -1
        notifyItemRangeRemoved(sectionPosition, changeCount)
    }

    fun reset(sections: List<Section> = emptyList()) {
        this.sections.clear()
        this.sections.addAll(sections)
        this.itemCountCache = -1
        indexCache.clear()
        this.initState()
        this.notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        if (itemCountCache > 0) {
            return itemCountCache
        }
        var count = sections.size
        sections.forEachIndexed { i, section ->
            if (states[i]) {
                count += rowsFactory(section, i).size
            }
        }
        itemCountCache = count
        return count
    }

    override fun getItemViewType(position: Int): Int {
        val (sectionIndex, rowIndex) = getIndex(position)
        if (rowIndex == -1) {
            val viewType = sectionViewTypeFactory(sections[sectionIndex], sectionIndex)
            if (viewType >= VIEW_TYPE_ROW) {
                throw IllegalArgumentException("section view type must < $VIEW_TYPE_ROW")
            }
            return viewType
        }
        return rowViewTypeFactory(sections[sectionIndex], sectionIndex, rowsFactory(sections[sectionIndex], sectionIndex)[rowIndex], rowIndex) + VIEW_TYPE_ROW
    }

    val indexCache = SparseArray<Pair<Int, Int>>()
    var itemCountCache = -1

    fun getIndex(position: Int): Pair<Int, Int> {
        val cachedIndex = indexCache.get(position)
        if (cachedIndex != null) {
            return cachedIndex
        }
        var sectionIndex = 0
        var rowIndex = -1
        var index = 0
        for (i in 0..sections.size - 1) {
            if (index == position) {
                sectionIndex = i
                rowIndex = -1
                break
            } else {
                if (states[i]) {
                    val nextIndex = index + rows(i).size + 1
                    if (position < nextIndex) {
                        //找到目标位置
                        sectionIndex = i
                        rowIndex = position - index - 1
                        break
                    } else {
                        index = nextIndex
                    }
                } else {
                    index++
                }
            }
        }
        return Pair(sectionIndex, rowIndex).apply { indexCache.put(position, this) }
    }

    fun getSectionPosition(index: Int): Int {
        var position = 0
        for (i in 0..sections.size - 1) {
            if (index == i) {
                break;
            } else {
                position++
                if (states[i]) {
                    position += rows(i).size
                }
            }
        }
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType < VIEW_TYPE_ROW) {
            val view = sectionHolderConverterFactory(viewType) as HolderConverter<Section>
            view.createViewForAdapter(parent.context)
            initLayoutParams(view.view)
            return SectionViewHolder(view)
        } else {
            val view = rowHolderConverterFactory(viewType - VIEW_TYPE_ROW) as RowHolderConverter<Section, Row>
            view.createViewForAdapter(parent.context)
            initLayoutParams(view.view)
            return RowViewHolder(view)
        }
    }

    fun initLayoutParams(view: View) {
        if (view.layoutParams == null) {
            (recyclerView?.layoutManager as? LinearLayoutManager)?.let {
                if (it.orientation == LinearLayoutManager.VERTICAL) {
                    view.layoutParams = ViewGroup.LayoutParams(matchParent, wrapContent)
                } else {
                    view.layoutParams = ViewGroup.LayoutParams(wrapContent, matchParent)
                }
            }
        }
    }

    private fun remoteCacheAtRange(index: Int, size: Int) {
        val end = Math.min(indexCache.size(), index + size)
        for (i in index..end - 1) {
            indexCache.removeAt(i)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val (sectionIndex, rowIndex) = getIndex(position)
        //        Log.e("expandable", "position:$position sectionIndex:$sectionIndex rowIndex:$rowIndex")
        if (holder is AmazingAdapter<*, *>.SectionViewHolder) {
            holder.sectionIndex = sectionIndex
            val sectionView = holder.sectionHolderConverter as HolderConverter<Section>
            sectionView.initData(sections[sectionIndex], sectionIndex)
            if (sectionView.expandable)
                sectionView.initState(states[sectionIndex])
        } else if (holder is AmazingAdapter<*, *>.RowViewHolder) {
            (holder.rowHolder as RowHolderConverter<Section, Row>).initData(sections[sectionIndex], sectionIndex, rowsFactory(sections[sectionIndex], sectionIndex)[rowIndex], rowIndex)
        }
    }

    inner class SectionViewHolder(val sectionHolderConverter: HolderConverter<Section>) : RecyclerView.ViewHolder(sectionHolderConverter.view), View.OnClickListener {

        init {
            this.itemView.setOnClickListener(this)
            sectionHolderConverter.viewHolder = this
        }

        var sectionIndex: Int = 0

        fun toggleState() {
            if (states[sectionIndex]) {
                remoteCacheAtRange(adapterPosition + 1, itemCount - adapterPosition + 1)
                states.put(sectionIndex, false)
                notifyItemRangeRemoved(adapterPosition + 1, rowsFactory(sections[sectionIndex], sectionIndex).size)
            } else {
                remoteCacheAtRange(adapterPosition + 1, itemCount - adapterPosition + 1)
                states.put(sectionIndex, true)
                notifyItemRangeInserted(adapterPosition + 1, rowsFactory(sections[sectionIndex], sectionIndex).size)
            }
            itemCountCache = -1
        }


        override fun onClick(v: View?) {
            if (sectionHolderConverter.expandable) {
                toggleState()
                sectionHolderConverter.onStateChange(states[sectionIndex])
            } else {
                sectionHolderConverter.onClick(sections[sectionIndex], sectionIndex)
            }
        }
    }

    inner class RowViewHolder(val rowHolder: RowHolderConverter<Section, Row>) : RecyclerView.ViewHolder(rowHolder.view), View.OnClickListener {
        init {
            this.itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            var position = adapterPosition
            val (sectionIndex, rowIndex) = getIndex(position)
            rowHolder.onClick(sections[sectionIndex], sectionIndex, rowsFactory(sections[sectionIndex], sectionIndex)[rowIndex], rowIndex)
        }
    }

    fun LinearDivider(orientation: Int = VERTICAL_LIST, dividerFactory: _LinearDivider.(data: Any, position: Int, view: View, parent: RecyclerView) -> Divider? = { data: Any, position: Int, view: View, parent: RecyclerView -> null }) =
            _LinearDivider(orientation, dividerFactory)

    data class Divider(
            var size: Int = 0,
            var marginStart: Int = 0,
            var marginEnd: Int = 0,
            var color: Int = Color.TRANSPARENT,
            var backgroundColor: Int = Color.TRANSPARENT
    )

    inner class _LinearDivider(val orientation: Int, val dividerFactory: _LinearDivider.(data: Any, position: Int, view: View, parent: RecyclerView) -> Divider?) : RecyclerView.ItemDecoration() {
        val paint = Paint()

        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State?) {
            for (i in 0..parent.childCount - 1) {
                val child = parent.getChildAt(i)
                val position = parent.getChildAdapterPosition(child)
                if (position == RecyclerView.NO_POSITION || position >= itemCount) {
                    continue
                }
                val divider: Divider = getDivider(position, child, parent) ?: continue
                val (size, marginStart, marginEnd, color, backgroundColor) = divider
                //                LogUtils.log("divider", "position:$position size:$size color:$color")
                if (color == Color.TRANSPARENT && backgroundColor == Color.TRANSPARENT) {
                    continue
                }

                val params = child.layoutParams as ViewGroup.MarginLayoutParams

                val top: Int
                val bottom: Int
                val left: Int
                val right: Int
                if (orientation == VERTICAL_LIST) {
                    top = child.bottom + params.bottomMargin
                    bottom = top + size
                    left = parent.paddingLeft + marginStart
                    right = parent.width - parent.paddingRight - marginEnd
                } else {
                    top = parent.paddingTop + marginStart
                    bottom = parent.height - parent.bottomPadding - marginEnd
                    left = child.right + params.rightMargin
                    right = left + size
                }
                if (backgroundColor != Color.TRANSPARENT) {
                    paint.color = backgroundColor
                    if (orientation == VERTICAL_LIST) {
                        c.drawRect((left - marginStart).toFloat(), top.toFloat(), (right + marginEnd).toFloat(), bottom.toFloat(), paint)
                    } else {
                        c.drawRect(left.toFloat(), (top - marginStart).toFloat(), right.toFloat(), (bottom + marginEnd).toFloat(), paint)
                    }
                }
                paint.color = color
                c.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
            }
        }

        fun isLast(position: Int): Boolean = position == itemCount - 1

        fun getData(position: Int): Any? {
            val (sectionIndex, rowIndex) = getIndex(position)
            val data: Any?

            if (rowIndex == -1) {
                if (sectionIndex < sections.size)
                    data = sections[sectionIndex]
                else
                    data = null
            } else {
                //当前元素是子视图
                if (rowIndex < rows(sectionIndex).size)
                    data = rows(sectionIndex)[rowIndex]
                else
                    data = null
            }
            return data
        }

        fun getDivider(position: Int, view: View, parent: RecyclerView): Divider? {
            if (position == RecyclerView.NO_POSITION || position >= itemCount) {
                return null
            }
            return dividerFactory(getData(position)!!, position, view, parent)
        }

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val position = parent.getChildAdapterPosition(view)
            val divider = getDivider(position, view, parent) ?: return
            val size = divider.size
            if (orientation == VERTICAL_LIST) {
                outRect.bottom = size
            } else {
                outRect.right = size
            }
        }
    }

}

/**
 * 默认空实现的子Holder转换器
 */
class EmptyRowHolderConverter <Section : Any, Row : Any>(val holderConverter: HolderConverter<Row>? = null) : RowHolderConverter<Section, Row>() {

    override fun initData(section: Section, sectionIndex: Int, row: Row, rowIndex: Int) {
        holderConverter?.initData(row, rowIndex)
    }

    override fun createView(context: Context) = holderConverter?.createView(context) ?: View(context)

    override fun onClick(section: Section, sectionIndex: Int, row: Row, rowIndex: Int) {
        holderConverter?.onClick(row, rowIndex)
    }
}
