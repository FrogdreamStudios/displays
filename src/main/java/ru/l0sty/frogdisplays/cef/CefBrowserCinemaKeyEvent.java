package ru.l0sty.frogdisplays.cef;

import org.cef.event.CefKeyEvent;

import java.awt.*;
import java.awt.event.KeyEvent;

public class CefBrowserCinemaKeyEvent extends CefKeyEvent {

    private long scancode = 0; // https://github.com/CinemaMod/java-cef/blob/6f9ddcb78228fdaac0eacba04f905a6aa97cff9f/native/CefBrowser_N.cpp#L1625

    public CefBrowserCinemaKeyEvent(int id, int modifiers, int keyCode, char keyChar) {
        super(id, keyCode, keyChar, modifiers);
        //super(source, id, when, modifiers, keyCode, keyChar, keyLocation);
    }

    public CefBrowserCinemaKeyEvent(int id, int modifiers, int keyCode, char keyChar, long scanCode) {
        //super(source, id, when, modifiers, keyCode, keyChar);
        super(id, keyCode, keyChar, modifiers);
        this.scancode = scanCode;
    }

}
