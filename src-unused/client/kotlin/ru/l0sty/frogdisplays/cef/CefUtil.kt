package ru.l0sty.frogdisplays.cef

import org.cef.CefApp
import org.cef.CefClient
import org.cef.CefSettings
import kotlin.math.floor

object CefUtil {
    var isInit: Boolean = false
        private set
    var cefApp: CefApp? = null
        private set
    var cefClient: CefClient? = null
        private set

    fun init(): Boolean {
        val cefSwitches = arrayOf(
            "--autoplay-policy=no-user-gesture-required",
            "--disable-web-security"
        )

        if (!CefApp.startup(cefSwitches)) {
            return false
        }

        val cefSettings = CefSettings()
        cefSettings.windowless_rendering_enabled = true
        cefSettings.background_color = cefSettings.ColorType(0, 255, 255, 255)

        cefApp = CefApp.getInstance(cefSwitches, cefSettings)

        //        CefApp.addAppHandler(new CefCinemaAppHandler(cefSwitches));
        cefClient = cefApp!!.createClient()
        //cefClient.addLoadHandler(CefBrowserCinemaLoadHandler())

        return true.also { isInit = it }
    }

    fun createBrowser(startUrl: String?, widthPx: Int, heightPx: Int): CefBrowserCinema? {
        if (!isInit) return null
        val browser = CefBrowserCinema(cefClient, startUrl, false, null)
        browser.setCloseAllowed()
        browser.createImmediately()
        browser.resize(widthPx, heightPx)
        return browser
    }

    fun createBrowser(startUrl: String?): CefBrowserCinema? {
        if (!isInit) return null
        val browser = CefBrowserCinema(cefClient, startUrl, true, null)
        browser.setCloseAllowed()
        browser.createImmediately()
        // Adjust screen size
        run {
            val widthBlocks: Float = 1f
            val heightBlocks: Float = 1f
            val scale = widthBlocks / heightBlocks
            val height: Int = 400
            val width = floor((height * scale).toDouble()) as Int
            browser.resize(width, height)
        }
        return browser
    }
}