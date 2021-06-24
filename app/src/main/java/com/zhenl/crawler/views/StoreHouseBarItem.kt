package com.zhenl.crawler.views

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.view.animation.Animation
import android.view.animation.Transformation
import java.util.*

/**
 * Created by lin on 2021/6/19.
 */
class StoreHouseBarItem(index: Int, start: PointF, end: PointF, color: Int, lineWidth: Int) :
    Animation() {

    var midPoint: PointF
    var translationX = 0f
    var index: Int
    protected val mPaint = Paint()
    protected var mFromAlpha = 1.0f
    protected var mToAlpha = 0.4f
    protected var mCStartPoint: PointF
    protected var mCEndPoint: PointF

    init {
        this.index = index
        midPoint = PointF((start.x + end.x) / 2, (start.y + end.y) / 2)
        mCStartPoint = PointF(start.x - midPoint.x, start.y - midPoint.y)
        mCEndPoint = PointF(end.x - midPoint.x, end.y - midPoint.y)
        setColor(color)
        setLineWidth(lineWidth)
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.STROKE
    }

    fun setLineWidth(width: Int) {
        mPaint.strokeWidth = width.toFloat()
    }

    fun setColor(color: Int) {
        mPaint.color = color
    }

    fun resetPosition(horizontalRandomness: Int) {
        val random = Random()
        translationX = (-random.nextInt(horizontalRandomness) + horizontalRandomness).toFloat()
    }

    override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
        var alpha = mFromAlpha
        alpha += (mToAlpha - alpha) * interpolatedTime
        setAlpha(alpha)
    }

    fun start(fromAlpha: Float, toAlpha: Float) {
        mFromAlpha = fromAlpha
        mToAlpha = toAlpha
        super.start()
    }

    fun setAlpha(alpha: Float) {
        mPaint.alpha = (alpha * 255).toInt()
    }

    fun draw(canvas: Canvas) {
        canvas.drawLine(mCStartPoint.x, mCStartPoint.y, mCEndPoint.x, mCEndPoint.y, mPaint)
    }
}