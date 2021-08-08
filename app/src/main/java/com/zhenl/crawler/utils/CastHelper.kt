package com.zhenl.crawler.utils

import org.fourthline.cling.support.model.DIDLObject
import org.fourthline.cling.support.model.ProtocolInfo
import org.fourthline.cling.support.model.Res
import org.fourthline.cling.support.model.item.VideoItem
import org.seamless.util.MimeType
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by lin on 2021/8/8.
 */
object CastHelper {

    private const val DIDL_LITE_FOOTER = "</DIDL-Lite>"
    private const val DIDL_LITE_HEADER = "<?xml version=\"1.0\"?>" +
            "<DIDL-Lite " + "xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" " +
            "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " + "xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\" " +
            "xmlns:dlna=\"urn:schemas-dlna-org:metadata-1-0/\">"

    fun createMetadata(url: String, id: String, name: String): String {
        val res = Res(MimeType(ProtocolInfo.WILDCARD, ProtocolInfo.WILDCARD), 0, url)
        val videoItem = VideoItem(id, "0", name, "unknow", res)
        return createItemMetadata(videoItem)
    }

    private fun createItemMetadata(item: DIDLObject): String {
        val metadata = StringBuilder()
        metadata.append(DIDL_LITE_HEADER)
        metadata.append(
            String.format(
                "<item id=\"%s\" parentID=\"%s\" restricted=\"%s\">",
                item.id,
                item.parentID,
                if (item.isRestricted) "1" else "0"
            )
        )
        metadata.append(String.format("<dc:title>%s</dc:title>", item.title))
        var creator = item.creator
        if (creator != null) {
            creator = creator.replace("<".toRegex(), "_")
            creator = creator.replace(">".toRegex(), "_")
        }
        metadata.append(String.format("<upnp:artist>%s</upnp:artist>", creator))
        metadata.append(
            String.format(
                "<upnp:class>%s</upnp:class>",
                item.clazz.value
            )
        )
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        val now = Date()
        val time: String = sdf.format(now)
        metadata.append(String.format("<dc:date>%s</dc:date>", time))
        val res = item.firstResource
        if (res != null) {
            // protocol info
            var protocolinfo = ""
            val pi = res.protocolInfo
            if (pi != null) {
                protocolinfo = String.format(
                    "protocolInfo=\"%s:%s:%s:%s\"",
                    pi.protocol,
                    pi.network,
                    "video/x-mpegurl",
                    pi.additionalInfo
                )
            }

            // resolution, extra info, not adding yet
            var resolution = ""
            if (!res.resolution.isNullOrEmpty()) {
                resolution = String.format("resolution=\"%s\"", res.resolution)
            }

            // duration
            var duration = ""
            if (!res.duration.isNullOrEmpty()) {
                duration = String.format("duration=\"%s\"", res.duration)
            }

            // res begin
            metadata.append(String.format("<res %s %s %s>", protocolinfo, resolution, duration))

            // url
            val url: String = res.value
            metadata.append(url)

            // res end
            metadata.append("</res>")
        }
        metadata.append("</item>")
        metadata.append(DIDL_LITE_FOOTER)
        return metadata.toString()
    }
}