package com.zhenl.crawler.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.zhenl.crawler.R
import com.zhenl.crawler.utils.AnimationHelper

/**
 * Created by lin on 2021/6/19.
 */
class MaterialSearchView : FrameLayout {
    private var mIsSearchOpen = false
    private var mAnimationDuration = 0
    private var mClearingFocus = false

    //Views
    private lateinit var mSearchLayout: ViewGroup
    private lateinit var mSearchSrcTextView: EditText
    private lateinit var mBackBtn: ImageButton
    private lateinit var mVoiceBtn: ImageButton
    private lateinit var mClearBtn: ImageButton
    private lateinit var mSearchTopBar: ViewGroup
    private var mOldQueryText: CharSequence? = null
    private var mUserQuery: CharSequence? = null
    private var mOnQueryChangeListener: OnQueryTextListener? = null
    private var mSearchViewListener: SearchViewListener? = null
    private var submit = false
    private var ellipsize = false
    private var allowVoiceSearch = false

    constructor(context: Context) : super(context) {
        initiateView(context)
        initStyle(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initiateView(context)
        initStyle(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs) {
        initiateView(context)
        initStyle(attrs, defStyleAttr)
    }

    private fun initStyle(attrs: AttributeSet?, defStyleAttr: Int) {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.MaterialSearchView,
            defStyleAttr,
            0
        )
        if (a.hasValue(R.styleable.MaterialSearchView_msv_searchBackground)) {
            background = a.getDrawable(R.styleable.MaterialSearchView_msv_searchBackground)
        }
        if (a.hasValue(R.styleable.MaterialSearchView_android_textColor)) {
            setTextColor(a.getColor(R.styleable.MaterialSearchView_android_textColor, 0))
        }
        if (a.hasValue(R.styleable.MaterialSearchView_android_textColorHint)) {
            setHintTextColor(
                a.getColor(
                    R.styleable.MaterialSearchView_android_textColorHint,
                    0
                )
            )
        }
        if (a.hasValue(R.styleable.MaterialSearchView_android_hint)) {
            setHint(a.getString(R.styleable.MaterialSearchView_android_hint))
        }
        if (a.hasValue(R.styleable.MaterialSearchView_msv_searchVoiceIcon)) {
            setVoiceIcon(a.getDrawable(R.styleable.MaterialSearchView_msv_searchVoiceIcon))
        }
        if (a.hasValue(R.styleable.MaterialSearchView_msv_searchClearIcon)) {
            setCloseIcon(a.getDrawable(R.styleable.MaterialSearchView_msv_searchClearIcon))
        }
        if (a.hasValue(R.styleable.MaterialSearchView_msv_searchBackIcon)) {
            setBackIcon(a.getDrawable(R.styleable.MaterialSearchView_msv_searchBackIcon))
        }
        if (a.hasValue(R.styleable.MaterialSearchView_android_inputType)) {
            setInputType(
                a.getInt(
                    R.styleable.MaterialSearchView_android_inputType,
                    EditorInfo.TYPE_NULL
                )
            )
        }
        a.recycle()
    }

    private fun initiateView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.layout_search_view, this, true)
        mSearchLayout = this
        mSearchTopBar = mSearchLayout.findViewById(R.id.search_top_bar)
        mSearchSrcTextView = mSearchLayout.findViewById(R.id.searchTextView)
        mBackBtn = mSearchLayout.findViewById(R.id.action_up_btn)
        mVoiceBtn = mSearchLayout.findViewById(R.id.action_voice_btn)
        mClearBtn = mSearchLayout.findViewById(R.id.action_clear_btn)
        mSearchSrcTextView.setOnClickListener(mOnClickListener)
        mBackBtn.setOnClickListener(mOnClickListener)
        mVoiceBtn.setOnClickListener(mOnClickListener)
        mClearBtn.setOnClickListener(mOnClickListener)
        allowVoiceSearch = false
        showVoice(true)
        initSearchView()
        setAnimationDuration(AnimationHelper.ANIMATION_DURATION_MEDIUM)
    }

    private fun initSearchView() {
        mSearchSrcTextView.setOnEditorActionListener { v, actionId, event ->
            onSubmitQuery()
            true
        }
        mSearchSrcTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                mUserQuery = s
                this@MaterialSearchView.onTextChanged(s)
            }

            override fun afterTextChanged(s: Editable) {}
        })
        mSearchSrcTextView.onFocusChangeListener =
            OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    showKeyboard(mSearchSrcTextView)
                }
            }
    }

    private val mOnClickListener =
        OnClickListener { v ->
            if (v === mBackBtn) {
                closeSearch()
            } else if (v === mVoiceBtn) {
                onVoiceClicked()
            } else if (v === mClearBtn) {
                mSearchSrcTextView.text = null
            }
        }

    private fun onVoiceClicked() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            //intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak an item name or number");    // user hint
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
            ) // setting recognition model, optimized for short phrases – search queries
            intent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                1
            ) // quantity of results we want to receive
            if (context is Activity) {
                (context as Activity).startActivityForResult(intent, REQUEST_VOICE)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "No recognize speech", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onTextChanged(newText: CharSequence) {
        val text: CharSequence = mSearchSrcTextView.text
        mUserQuery = text
        val hasText = !TextUtils.isEmpty(text)
        if (hasText) {
            mClearBtn.visibility = VISIBLE
            showVoice(false)
        } else {
            mClearBtn.visibility = GONE
            showVoice(true)
        }
        if (!TextUtils.equals(newText, mOldQueryText)) {
            mOnQueryChangeListener?.onQueryTextChange(newText.toString())
        }
        mOldQueryText = newText.toString()
    }

    private fun onSubmitQuery() {
        val query: CharSequence? = mSearchSrcTextView.text
        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            if (mOnQueryChangeListener == null || mOnQueryChangeListener?.onQueryTextSubmit(query.toString()) == false) {
                closeSearch()
                mSearchSrcTextView.text = null
            }
        }
    }

    private fun isVoiceAvailable(): Boolean {
        if (isInEditMode) {
            return true
        }
        val pm = context.packageManager
        val activities = pm.queryIntentActivities(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0
        )
        return activities.size == 0
    }

    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun showKeyboard(view: View?) {
        view?.requestFocus()
        val imm =
            view?.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(view, 0)
    }

    //Public Attributes
    override fun setBackground(background: Drawable?) {
        mSearchTopBar.background = background
    }

    override fun setBackgroundColor(color: Int) {
        mSearchTopBar.setBackgroundColor(color)
    }

    fun setTextColor(color: Int) {
        mSearchSrcTextView.setTextColor(color)
    }

    fun setHintTextColor(color: Int) {
        mSearchSrcTextView.setHintTextColor(color)
    }

    fun setHint(hint: CharSequence?) {
        mSearchSrcTextView.hint = hint
    }

    fun setVoiceIcon(drawable: Drawable?) {
        mVoiceBtn.setImageDrawable(drawable)
    }

    fun setCloseIcon(drawable: Drawable?) {
        mClearBtn.setImageDrawable(drawable)
    }

    fun setBackIcon(drawable: Drawable?) {
        mBackBtn.setImageDrawable(drawable)
    }

    fun setInputType(inputType: Int) {
        mSearchSrcTextView.inputType = inputType
    }

    fun setCursorDrawable(drawable: Int) {
        try {
            // https://github.com/android/platform_frameworks_base/blob/kitkat-release/core/java/android/widget/TextView.java#L562-564
            val f = TextView::class.java.getDeclaredField("mCursorDrawableRes")
            f.isAccessible = true
            f[mSearchSrcTextView] = drawable
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 设置是否允许语音搜索
     *
     * @param voiceSearch
     */
    fun setVoiceSearch(voiceSearch: Boolean) {
        allowVoiceSearch = voiceSearch
    }

    /**
     * 设置是否点击条目后直接进行查询
     * Submit the query as soon as the user clicks the item.
     *
     * @param submit submit state
     */
    fun setSubmitOnClick(submit: Boolean) {
        this.submit = submit
    }

    /**
     * Calling this will set the query to search text box. if submit is true, it'll submit the query.
     *
     * @param query
     * @param submit
     */
    fun setQuery(query: CharSequence?, submit: Boolean) {
        mSearchSrcTextView.setText(query)
        if (query != null) {
            mSearchSrcTextView.setSelection(mSearchSrcTextView.length())
            mUserQuery = query
        }
        if (submit && !TextUtils.isEmpty(query)) {
            onSubmitQuery()
        }
    }

    /**
     * if show is true, this will enable voice search. If voice is not available on the device, this method call has not effect.
     *
     * @param show
     */
    fun showVoice(show: Boolean) {
        if (show && isVoiceAvailable() && allowVoiceSearch) {
            mVoiceBtn.visibility = VISIBLE
        } else {
            mVoiceBtn.visibility = GONE
        }
    }

    /**
     * 加入到菜单中
     * Call this method and pass the menu item so this class can handle click events for the Menu Item.
     *
     * @param menuItem
     */
    fun setMenuItem(menuItem: MenuItem) {
        menuItem.setOnMenuItemClickListener {
            showSearch()
            true
        }
    }

    /**
     * Return true if search is open
     *
     * @return
     */
    fun isSearchOpen(): Boolean {
        return mIsSearchOpen
    }

    /**
     * Sets animation duration. ONLY FOR PRE-LOLLIPOP
     *
     * @param duration duration of the animation
     */
    fun setAnimationDuration(duration: Int) {
        mAnimationDuration = duration
    }
    /**
     * Open Search View. If animate is true, Animate the showing of the view.
     *
     * @param animate true for animate
     */
    /**
     * 显示搜索框
     * Open Search View. This will animate the showing of the view.
     */
    @JvmOverloads
    fun showSearch(animate: Boolean = true) {
        if (isSearchOpen()) {
            return
        }

        //Request Focus
        mSearchSrcTextView.text = null
        mSearchSrcTextView.requestFocus()
        if (animate) {
            setVisibleWithAnimation()
        } else {
            mSearchLayout.visibility = VISIBLE
            mSearchViewListener?.onSearchViewShown()
        }
        mIsSearchOpen = true
    }

    private fun setVisibleWithAnimation() {
        val animationListener: AnimationHelper.AnimationListener =
            object : AnimationHelper.AnimationListener {
                override fun onAnimationStart(view: View?): Boolean {
                    return false
                }

                override fun onAnimationEnd(view: View?): Boolean {
                    mSearchViewListener?.onSearchViewShown()
                    return false
                }

                override fun onAnimationCancel(view: View?): Boolean {
                    return false
                }
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mSearchLayout.visibility = VISIBLE
            AnimationHelper.reveal(mSearchTopBar, animationListener)
        } else {
            AnimationHelper.fadeInView(mSearchLayout, mAnimationDuration, animationListener)
        }
    }

    /**
     * 关闭搜索框
     * Close search view.
     */
    fun closeSearch() {
        if (!isSearchOpen()) {
            return
        }
        mSearchSrcTextView.text = null
        clearFocus()
        mSearchLayout.visibility = GONE
        mSearchViewListener?.onSearchViewClosed()
        mIsSearchOpen = false
    }

    /**
     * 设置查询监听
     * Set this listener to listen to Query Change events.
     *
     * @param listener
     */
    fun setOnQueryTextListener(listener: OnQueryTextListener?) {
        mOnQueryChangeListener = listener
    }

    /**
     * 设置搜索框打开关闭监听
     * Set this listener to listen to Search View open and close events
     *
     * @param listener
     */
    fun setOnSearchViewListener(listener: SearchViewListener?) {
        mSearchViewListener = listener
    }

    /**
     * Ellipsize suggestions longer than one line.
     *
     * @param ellipsize
     */
    fun setEllipsize(ellipsize: Boolean) {
        this.ellipsize = ellipsize
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        // Don't accept focus if in the middle of clearing focus
        if (mClearingFocus) {
            return false
        }
        // Check if SearchView is focusable.
        return if (!isFocusable) {
            false
        } else mSearchSrcTextView.requestFocus(direction, previouslyFocusedRect)
    }

    override fun clearFocus() {
        mClearingFocus = true
        hideKeyboard(this)
        super.clearFocus()
        mSearchSrcTextView.clearFocus()
        mClearingFocus = false
    }

    interface OnQueryTextListener {
        /**
         * Called when the user submits the query. This could be due to a key press on the
         * keyboard or due to pressing a submit button.
         * The listener can override the standard behavior by returning true
         * to indicate that it has handled the submit request. Otherwise return false to
         * let the SearchView handle the submission by launching any associated intent.
         *
         * @param query the query text that is to be submitted
         * @return true if the query has been handled by the listener, false to let the
         * SearchView perform the default action.
         */
        fun onQueryTextSubmit(query: String): Boolean

        /**
         * Called when the query text is changed by the user.
         *
         * @param newText the new content of the query text field.
         * @return false if the SearchView should perform the default action of showing any
         * suggestions if available, true if the action was handled by the listener.
         */
        fun onQueryTextChange(newText: String): Boolean
    }

    interface SearchViewListener {
        fun onSearchViewShown()
        fun onSearchViewClosed()
    }

    companion object {
        const val REQUEST_VOICE = 9999
    }
}