package com.zhenl.crawler.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

/**
 * Created by lin on 2018/6/8.
 */
public class GridViewForScrollView extends GridView {
    public GridViewForScrollView(Context context) {
        this(context, null);
    }

    public GridViewForScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GridViewForScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
                MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }
}
