package com.kuikly.kuiklychartkit.h5

import com.tencent.kuikly.core.render.web.expand.KuiklyRenderViewDelegatorDelegate
import com.tencent.kuikly.core.render.web.ktx.SizeI
import com.tencent.kuikly.core.render.web.runtime.web.expand.KuiklyRenderViewDelegator
import kotlinx.browser.document
import kotlinx.browser.window

private const val CONTAINER_ID = "root"
private const val DEFAULT_PAGE_NAME = "chart_showcase"

/**
 * Minimal host adapter recommended by Kuikly Web Render.
 *
 * Business UI stays in chartkit/nativevue2.js; this class only owns the browser
 * lifecycle and the Web Render root.
 */
private class ChartWebRenderDelegator : KuiklyRenderViewDelegatorDelegate {
    private val delegate = KuiklyRenderViewDelegator(this)

    fun attach(pageName: String, pageData: Map<String, Any>, size: SizeI) {
        delegate.onAttach(CONTAINER_ID, pageName, pageData, size)
    }

    fun resume() = delegate.onResume()

    fun pause() = delegate.onPause()

    fun detach() = delegate.onDetach()
}

fun main() {
    val width = window.innerWidth
    val height = window.innerHeight
    val pageName = queryParameter("page_name") ?: DEFAULT_PAGE_NAME
    val pageData: Map<String, Any> = mapOf(
        "statusBarHeight" to 0f,
        "activityWidth" to width,
        "activityHeight" to height,
        "param" to mapOf("is_H5" to "1"),
    )

    val delegator = ChartWebRenderDelegator()
    delegator.attach(pageName, pageData, SizeI(width, height))
    delegator.resume()

    document.addEventListener("visibilitychange", {
        if (document.asDynamic().hidden as Boolean) {
            delegator.pause()
        } else {
            delegator.resume()
        }
    })
    window.addEventListener("beforeunload", {
        delegator.detach()
    })
}

private fun queryParameter(name: String): String? {
    val query = window.location.search.removePrefix("?")
    return query
        .split("&")
        .asSequence()
        .mapNotNull { item ->
            val separator = item.indexOf('=')
            if (separator < 0) null else item.substring(0, separator) to item.substring(separator + 1)
        }
        .firstOrNull { it.first == name }
        ?.second
        ?.takeIf { it.isNotBlank() }
}
