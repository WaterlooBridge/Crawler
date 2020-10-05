package com.zhenl.crawler.adapter

import com.zhenl.crawler.R
import com.zhenl.crawler.models.DramasModel
import com.zhenl.violet.base.BasePagedListAdapter
import com.zhenl.violet.base.BaseViewHolder

/**
 * Created by lin on 2020/10/3.
 */
class DramasAdapter : BasePagedListAdapter<DramasModel>() {
    override fun getLayoutResId(viewType: Int): Int = R.layout.item_movie_dramas

    override fun convert(holder: BaseViewHolder, item: DramasModel) {
        holder.setText(R.id.tv, item.text)
    }
}