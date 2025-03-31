package ru.l0sty.frogdisplays.cef;

import net.minecraft.client.MinecraftClient;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserOsr;
import org.cef.browser.CefRequestContext;
import org.cef.event.CefKeyEvent;
import org.cef.event.CefMouseEvent;
import org.cef.event.CefMouseWheelEvent;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.nio.ByteBuffer;

public class CefBrowserCinema extends CefBrowserOsr {

    public final CefBrowserCinemaRenderer renderer = new CefBrowserCinemaRenderer(true);

    public CefBrowserCinema(CefClient client, String url, boolean transparent, CefRequestContext context) {
        super(client, url, transparent, context);
        MinecraftClient.getInstance().submit(renderer::initialize);
    }

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        renderer.onPaint(buffer, width, height);
    }

    public void sendKeyPress(int keyCode, int modifiers, long scanCode) {
        CefBrowserCinemaKeyEvent keyEvent = new CefBrowserCinemaKeyEvent(
                KeyEvent.KEY_PRESSED,
                modifiers,
                keyCode,
                KeyEvent.CHAR_UNDEFINED,
                scanCode);
        sendKeyEvent(keyEvent);
    }

    public void sendKeyRelease(int keyCode, int modifiers, long scanCode) {
        CefBrowserCinemaKeyEvent keyEvent = new CefBrowserCinemaKeyEvent(
                KeyEvent.KEY_RELEASED,
                modifiers,
                keyCode,
                KeyEvent.CHAR_UNDEFINED,
                scanCode);
        sendKeyEvent(keyEvent);
    }

    public void sendKeyTyped(char c, int modifiers) {
        CefKeyEvent keyEvent = new CefKeyEvent(
                KeyEvent.KEY_TYPED,
                KeyEvent.VK_UNDEFINED,
                c,
                modifiers
        );
        sendKeyEvent(keyEvent);
    }

    public void sendMouseMove(int mouseX, int mouseY) {
        CefMouseEvent mouseEvent = new CefMouseEvent(
                CefMouseEvent.MOUSE_MOVED,
                mouseX,
                mouseY,
                0,
                MouseEvent.BUTTON1_DOWN_MASK,
                0);
        sendMouseEvent(mouseEvent);
    }

    public void sendMousePress(int mouseX, int mouseY, int button) {
        CefMouseEvent mouseEvent = new CefMouseEvent(
                MouseEvent.MOUSE_PRESSED,
                mouseX,
                mouseY,
                1,
                button + 1,0);
        sendMouseEvent(mouseEvent);
    }

    public void sendMouseRelease(int mouseX, int mouseY, int button) {
        CefMouseEvent mouseEvent = new CefMouseEvent(
                MouseEvent.MOUSE_RELEASED,
                mouseX,
                mouseY,
                1,
                button + 1,
                0);
        sendMouseEvent(mouseEvent);

        mouseEvent = new CefMouseEvent(
                MouseEvent.MOUSE_CLICKED,
                mouseX,
                mouseY,
                1,
                button + 1,
                0);
        sendMouseEvent(mouseEvent);
    }

    public void sendMouseWheel(int mouseX, int mouseY, int mods, int amount, int rotation) {
        CefMouseWheelEvent mouseWheelEvent = new CefMouseWheelEvent(
              CefMouseWheelEvent.WHEEL_UNIT_SCROLL,
                mouseX,
                mouseY,
                amount,
                mods);
        sendMouseWheelEvent(mouseWheelEvent);
    }

    public void resize(int width, int height) {
        browser_rect_.setBounds(0, 0, width, height);
        wasResized(width, height);
    }

    public void close() {
        renderer.cleanup();
        super.close(true);
    }

}
