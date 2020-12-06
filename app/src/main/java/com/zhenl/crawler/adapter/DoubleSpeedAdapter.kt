package com.zhenl.crawler.adapter

import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.paging.PagingData
import com.zhenl.crawler.R
import com.zhenl.violet.base.BasePagedListAdapter
import com.zhenl.violet.base.BaseViewHolder

/**
 * Created by lin on 2020/12/6.
 */
class DoubleSpeedAdapter(lifecycle: Lifecycle) : BasePagedListAdapter<String>() {

    init {
        submitData(lifecycle, PagingData.from(arrayListOf("2.0", "1.5", "1.25", "1.0", "0.75", "0.5")))
    }

    override fun getLayoutResId(viewType: Int): Int = R.layout.item_double_speed

    override fun convert(holder: BaseViewHolder, item: String) {
        val text = item + "X"
        (holder.itemView as TextView).text = text
    }
}