package com.zhenl.crawler.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.util.TypedValue
import android.view.View
import android.view.ViewAnimationUtils
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListener

/**
 * Created by lin on 2021/6/19.
 */
object AnimationHelper {

    const val ANIMATION_DURATION_SHORT = 150
    const val ANIMATION_DURATION_MEDIUM = 400
    const val ANIMATION_DURATION_LONG = 800

    interface AnimationListener {
        /**
         * @return true to override parent. Else execute Parent method
         */
        fun onAnimationStart(view: View?): Boolean
        fun onAnimationEnd(view: View?): Boolean
        fun onAnimationCancel(view: View?): Boolean
    }

    fun crossFadeViews(showView: View, hideView: View) {
        crossFadeViews(showView, hideView, ANIMATION_DURATION_SHORT)
    }

    fun crossFadeViews(showView: View, hideView: View, duration: Int) {
        fadeInView(showView, duration)
        fadeOutView(hideView, duration)
    }

    fun fadeInView(view: View) {
        fadeInView(view, ANIMATION_DURATION_SHORT)
    }

    fun fadeInView(view: View, duration: Int) {
        fadeInView(view, duration, null)
    }

    fun fadeInView(view: View, duration: Int, listener: AnimationListener?) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        var vpListener: ViewPropertyAnimatorListener? = null
        if (listener != null) {
            vpListener = object : ViewPropertyAnimatorListener {
                override fun onAnimationStart(view: View) {
                    if (!listener.onAnimationStart(view)) {
                        view.isDrawingCacheEnabled = true
                    }
                }

                override fun onAnimationEnd(view: View) {
                    if (!listener.onAnimationEnd(view)) {
                        view.isDrawingCacheEnabled = false
                    }
                }

                override fun onAnimationCancel(view: View) {}
            }
        }
        ViewCompat.animate(view).alpha(1f).setDuration(duration.toLong()).setListener(vpListener)
    }

    fun reveal(view: View, listener: AnimationListener) {
        val cx = view.width - TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 24f, view.resources.displayMetrics
        ).toInt()
        val cy = view.height / 2
        val finalRadius = view.width.coerceAtLeast(view.height)
        val anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0f, finalRadius.toFloat())
        view.visibility = View.VISIBLE
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                listener.onAnimationStart(view)
            }

            override fun onAnimationEnd(animation: Animator) {
                listener.onAnimationEnd(view)
            }

            override fun onAnimationCancel(animation: Animator) {
                listener.onAnimationCancel(view)
            }

            override fun onAnimationRepeat(animation: Animator) {}
        })
        anim.start()
    }

    fun fadeOutView(view: View) {
        fadeOutView(view, ANIMATION_DURATION_SHORT)
    }

    fun fadeOutView(view: View, duration: Int) {
        fadeOutView(view, duration, null)
    }

    fun fadeOutView(view: View, duration: Int, listener: AnimationListener?) {
        ViewCompat.animate(view).alpha(0f).setDuration(duration.toLong())
            .setListener(object : ViewPropertyAnimatorListener {
                override fun onAnimationStart(view: View) {
                    if (listener == null || !listener.onAnimationStart(view)) {
                        view.isDrawingCacheEnabled = true
                    }
                }

                override fun onAnimationEnd(view: View) {
                    if (listener == null || !listener.onAnimationEnd(view)) {
                        view.visibility = View.GONE
                        view.isDrawingCacheEnabled = false
                    }
                }

                override fun onAnimationCancel(view: View) {}
            })
    }
}