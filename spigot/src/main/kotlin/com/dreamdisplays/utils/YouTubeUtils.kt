package com.dreamdisplays.utils

import org.jspecify.annotations.NullMarked
import java.net.URI

/**
 * Utility functions for handling YouTube URLs and video IDs.
 */
@NullMarked
object YouTubeUtils {

//    private val VIDEO_ID_REGEX = "(?<=([?&]v=))[^#&?]*".toRegex()
    private val SANITIZE_REGEX = "[^0-9A-Za-z+.-]".toRegex()

//    fun extractVideoId(youtubeUrl: String): String? {
//        extractVideoIdFromUri(youtubeUrl)?.let { return it }
//
//        return VIDEO_ID_REGEX.find(youtubeUrl)?.value
//    }

    fun extractVideoIdFromUri(url: String): String? {
        return runCatching {
            val uri = URI(url)

            // Check youtube.com/watch?v=ID format
            uri.query?.let { query ->
                parseQueryParameter(query, "v")?.let { return it }
            }

            // Check youtu.be/ID format
            if (uri.host?.contains("youtu.be") == true) {
                return uri.path?.trimStart('/')?.takeIf { it.isNotEmpty() }
            }

            // Check youtube.com/shorts/ID format
            if (uri.host?.contains("youtube.com") == true && uri.path?.contains("shorts") == true) {
                return uri.path?.split("/")?.lastOrNull { it.isNotEmpty() }
            }

            null
        }.getOrNull()
    }

    private fun parseQueryParameter(query: String, paramName: String): String? {
        return query.split("&")
            .firstNotNullOfOrNull { param ->
                val (key, value) = param.split("=", limit = 2)
                    .takeIf { it.size == 2 } ?: return@firstNotNullOfOrNull null

                if (key == paramName) value else null
            }
    }

    fun sanitize(raw: String?): String? {
        if (raw == null) return null
        return raw.trim().replace(SANITIZE_REGEX, "")
    }
}
