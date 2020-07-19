package com.zhenl.crawler.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Created by lin on 2020/7/19.
 */
@Parcelize
data class VideoModel(val title: String?, val subtitle: String?, val url: String, var videoPath: String?) : Parcelable {
    constructor(title: String?, subtitle: String?, url: String) : this(title, subtitle, url, null)
}