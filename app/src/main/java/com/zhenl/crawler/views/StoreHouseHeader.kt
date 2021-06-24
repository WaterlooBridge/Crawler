package com.zhenl.crawler.views

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.annotation.ColorInt
import androidx.annotation.NonNull
import com.scwang.smart.refresh.layout.api.RefreshHeader
import com.scwang.smart.refresh.layout.api.RefreshKernel
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.simple.SimpleComponent
import com.scwang.smart.refresh.layout.util.SmartUtil
import com.zhenl.crawler.R
import com.zhenl.crawler.utils.StoreHousePathHelper
import java.util.*
import kotlin.math.ceil

/**
 * Created by lin on 2021/6/19.
 */
class StoreHouseHeader @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    SimpleComponent(context, attrs, 0), RefreshHeader {

    companion object {
        private const val mInternalAnimationFactor = 0.7f
        private const val mBarDarkAlpha = 0.4f
        private const val mFromAlpha = 1.0f
        private const val mToAlpha = 0.4f
        private const val mLoadingAniItemDuration = 400L
    }

    var mItemList = ArrayList<StoreHouseBarItem>()
    private var mScale = 1f
    private var mLineWidth = -1
    private var mDropHeight = -1
    private var mHorizontalRandomness = -1
    private var mProgress = 0f
    private var mDrawZoneWidth = 0
    private var mDrawZoneHeight = 0
    private var mOffsetX = 0
    private var mOffsetY = 0
    private var mLoadingAniDuration = 1000
    private var mLoadingAniSegDuration = 1000
    private var mTextColor = Color.WHITE
    private var mBackgroundColor = 0
    private var mIsInLoading = false
    private var mEnableFadeAnimation = false
    private var mMatrix = Matrix()
    private var mRefreshKernel: RefreshKernel? = null
    private var mAniController: AniController = AniController()
    private var mTransformation = Transformation()

    init {
        mLineWidth = SmartUtil.dp2px(1f)
        mDropHeight = SmartUtil.dp2px(40f)
        mHorizontalRandomness = Resources.getSystem().displayMetrics.widthPixels / 2
        mBackgroundColor = -0xcccccd
        setTextColor(-0x333334)
        val ta = context.obtainStyledAttributes(attrs, R.styleable.StoreHouseHeader)
        mLineWidth =
            ta.getDimensionPixelOffset(R.styleable.StoreHouseHeader_srlLineWidth, mLineWidth)
        mDropHeight =
            ta.getDimensionPixelOffset(R.styleable.StoreHouseHeader_srlDropHeight, mDropHeight)
        mEnableFadeAnimation =
            ta.getBoolean(R.styleable.StoreHouseHeader_srlEnableFadeAnimation, mEnableFadeAnimation)
        mLineWidth =
            ta.getDimensionPixelOffset(R.styleable.StoreHouseHeader_shhLineWidth, mLineWidth)
        mDropHeight =
            ta.getDimensionPixelOffset(R.styleable.StoreHouseHeader_shhDropHeight, mDropHeight)
        mEnableFadeAnimation =
            ta.getBoolean(R.styleable.StoreHouseHeader_shhEnableFadeAnimation, mEnableFadeAnimation)
        val defaultText = "StoreHouse"
        when {
            ta.hasValue(R.styleable.StoreHouseHeader_shhText) -> {
                initWithString(ta.getString(R.styleable.StoreHouseHeader_shhText) ?: defaultText)
            }
            ta.hasValue(R.styleable.StoreHouseHeader_srlText) -> {
                initWithString(ta.getString(R.styleable.StoreHouseHeader_srlText) ?: defaultText)
            }
            else -> {
                initWithString(defaultText)
            }
        }
        ta.recycle()
        val thisView: View = this
        thisView.minimumHeight = mDrawZoneHeight + SmartUtil.dp2px(40f)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val thisView: View = this
        super.setMeasuredDimension(
            resolveSize(super.getSuggestedMinimumWidth(), widthMeasureSpec),
            resolveSize(super.getSuggestedMinimumHeight(), heightMeasureSpec)
        )
        mOffsetX = (thisView.measuredWidth - mDrawZoneWidth) / 2
        mOffsetY = (thisView.measuredHeight - mDrawZoneHeight) / 2 //getTopOffset();
        mDropHeight = thisView.measuredHeight / 2 //getTopOffset();
    }

    override fun dispatchDraw(canvas: Canvas) {
        val thisView: View = this
        val c1 = canvas.save()
        val len: Int = mItemList.size
        val progress: Float = if (thisView.isInEditMode) 1f else mProgress
        for (i in 0 until len) {
            canvas.save()
            val storeHouseBarItem: StoreHouseBarItem = mItemList[i]
            var offsetX: Float = mOffsetX + storeHouseBarItem.midPoint.x
            var offsetY: Float = mOffsetY + storeHouseBarItem.midPoint.y
            if (mIsInLoading) {
                storeHouseBarItem.getTransformation(thisView.drawingTime, mTransformation)
                canvas.translate(offsetX, offsetY)
            } else {
                if (progress == 0f) {
                    storeHouseBarItem.resetPosition(mHorizontalRandomness)
                    continue
                }
                val startPadding = (1 - mInternalAnimationFactor) * i / len
                val endPadding = 1 - mInternalAnimationFactor - startPadding

                // done
                if (progress == 1f || progress >= 1 - endPadding) {
                    canvas.translate(offsetX, offsetY)
                    storeHouseBarItem.setAlpha(mBarDarkAlpha)
                } else {
                    val realProgress: Float = if (progress <= startPadding) {
                        0f
                    } else {
                        1f.coerceAtMost((progress - startPadding) / mInternalAnimationFactor)
                    }
                    offsetX += storeHouseBarItem.translationX * (1 - realProgress)
                    offsetY += -mDropHeight * (1 - realProgress)
                    mMatrix.reset()
                    mMatrix.postRotate(360 * realProgress)
                    mMatrix.postScale(realProgress, realProgress)
                    mMatrix.postTranslate(offsetX, offsetY)
                    storeHouseBarItem.setAlpha(mBarDarkAlpha * realProgress)
                    canvas.concat(mMatrix)
                }
            }
            storeHouseBarItem.draw(canvas)
            canvas.restore()
        }
        if (mIsInLoading) {
            thisView.invalidate()
        }
        canvas.restoreToCount(c1)
        super.dispatchDraw(canvas)
    }

    fun setLoadingAniDuration(duration: Int): StoreHouseHeader {
        mLoadingAniDuration = duration
        mLoadingAniSegDuration = duration
        return this
    }

    fun setLineWidth(width: Int): StoreHouseHeader {
        mLineWidth = width
        for (i in mItemList.indices) {
            mItemList[i].setLineWidth(width)
        }
        return this
    }

    fun setTextColor(@ColorInt color: Int): StoreHouseHeader {
        mTextColor = color
        for (i in mItemList.indices) {
            mItemList[i].setColor(color)
        }
        return this
    }

    fun setDropHeight(height: Int): StoreHouseHeader {
        mDropHeight = height
        return this
    }

    fun initWithString(str: String): StoreHouseHeader {
        initWithString(str, 25)
        return this
    }

    fun initWithString(str: String, fontSize: Int): StoreHouseHeader {
        val pointList: MutableList<FloatArray> =
            StoreHousePathHelper.getPath(str, fontSize * 0.01f, 14)
        initWithPointList(pointList)
        return this
    }

    fun initWithStringArray(id: Int): StoreHouseHeader {
        val thisView: View = this
        val points = thisView.resources.getStringArray(id)
        val pointList = ArrayList<FloatArray>()
        for (point in points) {
            val x: Array<String> = point.split(",").toTypedArray()
            val f = FloatArray(4)
            for (j in 0..3) {
                f[j] = x[j].toFloat()
            }
            pointList.add(f)
        }
        initWithPointList(pointList)
        return this
    }

    fun setScale(scale: Float): StoreHouseHeader {
        mScale = scale
        return this
    }

    fun initWithPointList(pointList: MutableList<FloatArray>): StoreHouseHeader {
        var drawWidth = 0f
        var drawHeight = 0f
        val shouldLayout: Boolean = mItemList.size > 0
        mItemList.clear()
        for (i in pointList.indices) {
            val line: FloatArray = pointList[i]
            val startPoint =
                PointF(SmartUtil.dp2px(line[0]) * mScale, SmartUtil.dp2px(line[1]) * mScale)
            val endPoint =
                PointF(SmartUtil.dp2px(line[2]) * mScale, SmartUtil.dp2px(line[3]) * mScale)
            drawWidth = drawWidth.coerceAtLeast(startPoint.x)
            drawWidth = drawWidth.coerceAtLeast(endPoint.x)
            drawHeight = drawHeight.coerceAtLeast(startPoint.y)
            drawHeight = drawHeight.coerceAtLeast(endPoint.y)
            val item = StoreHouseBarItem(i, startPoint, endPoint, mTextColor, mLineWidth)
            item.resetPosition(mHorizontalRandomness)
            mItemList.add(item)
        }
        mDrawZoneWidth = ceil(drawWidth.toDouble()).toInt()
        mDrawZoneHeight = ceil(drawHeight.toDouble()).toInt()
        if (shouldLayout) {
            val thisView: View = this
            thisView.requestLayout()
        }
        return this
    }

    override fun onInitialized(@NonNull kernel: RefreshKernel, height: Int, maxDragHeight: Int) {
        mRefreshKernel = kernel
        kernel.requestDrawBackgroundFor(this, mBackgroundColor)
    }

    override fun onMoving(
        isDragging: Boolean,
        percent: Float,
        offset: Int,
        height: Int,
        maxDragHeight: Int
    ) {
        mProgress = percent * .8f
        val thisView: View = this
        thisView.invalidate()
    }

    override fun onReleased(@NonNull layout: RefreshLayout, height: Int, maxDragHeight: Int) {
        mIsInLoading = true
        mAniController.start()
        val thisView: View = this
        thisView.invalidate()
    }

    override fun onFinish(@NonNull layout: RefreshLayout, success: Boolean): Int {
        mIsInLoading = false
        mAniController.stop()
        if (success && mEnableFadeAnimation) {
            startAnimation(object : Animation() {
                override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                    mProgress = 1 - interpolatedTime
                    invalidate()
                    if (interpolatedTime == 1f) {
                        for (i in mItemList.indices) {
                            mItemList[i].resetPosition(mHorizontalRandomness)
                        }
                    }
                }

                init {
                    super.setDuration(250)
                    super.setInterpolator(AccelerateInterpolator())
                }
            })
            return 250
        } else {
            for (i in mItemList.indices) {
                mItemList[i].resetPosition(mHorizontalRandomness)
            }
        }
        return 0
    }

    /**
     * @param colors 对应Xml中配置的 srlPrimaryColor srlAccentColor
     */
    @Deprecated("请使用 {@link RefreshLayout#setPrimaryColorsId(int...)}")
    override fun setPrimaryColors(@ColorInt vararg colors: Int) {
        if (colors.isNotEmpty()) {
            mBackgroundColor = colors[0]
            mRefreshKernel?.requestDrawBackgroundFor(this, mBackgroundColor)
            if (colors.size > 1) {
                setTextColor(colors[1])
            }
        }
    }

    private inner class AniController : Runnable {
        var mTick = 0
        var mCountPerSeg = 0
        var mSegCount = 0
        var mInterval = 0
        var mRunning = true
        fun start() {
            mRunning = true
            mTick = 0
            mInterval = mLoadingAniDuration / mItemList.size
            mCountPerSeg = mLoadingAniSegDuration / mInterval
            mSegCount = mItemList.size / mCountPerSeg + 1
            run()
        }

        override fun run() {
            val pos = mTick % mCountPerSeg
            for (i in 0 until mSegCount) {
                var index = i * mCountPerSeg + pos
                if (index > mTick) {
                    continue
                }
                index %= mItemList.size
                val item: StoreHouseBarItem = mItemList[index]
                item.fillAfter = false
                item.isFillEnabled = true
                item.fillBefore = false
                item.duration = mLoadingAniItemDuration
                item.start(mFromAlpha, mToAlpha)
            }
            mTick++
            if (mRunning) {
                val refreshView = mRefreshKernel?.refreshLayout?.layout
                refreshView?.postDelayed(this, mInterval.toLong())
            }
        }

        fun stop() {
            mRunning = false
            val thisView: View = this@StoreHouseHeader
            thisView.removeCallbacks(this)
        }
    }
}