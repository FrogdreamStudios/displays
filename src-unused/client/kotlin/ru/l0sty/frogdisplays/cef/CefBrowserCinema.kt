package ru.l0sty.frogdisplays.cef

import net.minecraft.client.MinecraftClient
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefBrowserOsr
import org.cef.browser.CefRequestContext
import java.awt.Rectangle
import java.nio.ByteBuffer

class CefBrowserCinema(client: CefClient?, url: String?, transparent: Boolean, context: CefRequestContext?): CefBrowserOsr(client, url, transparent, context) {

    val renderer = CefBrowserCinemaRenderer(true)

    init {
        MinecraftClient.getInstance().submit { renderer.initialize() }
    }

    override fun onPaint(
        browser: CefBrowser?,
        popup: Boolean,
        dirtyRects: Array<Rectangle?>?,
        buffer: ByteBuffer?,
        width: Int,
        height: Int
    ) {
        renderer.onPaint(buffer, width, height)
    }
    fun resize(width: Int, height: Int) {
        browser_rect_.setBounds(0, 0, width, height)
        wasResized(width, height)
    }
}