package com.midiwars.jna;

import com.sun.jna.platform.win32.WinUser;

/**
 * Extension of JNA's Winuser.h mapping.
 */
public interface MyWinUser extends WinUser {

    /** Flag for injected keyboard events. */
    int LLKHF_INJECTED = 0x00000010;

    /**
     * The callback function is not mapped into the address space of the process that generates the event.
     * Because the hook function is called across process boundaries, the system must queue events.
     * Although this method is asynchronous, events are guaranteed to be in sequential order.
     */
    int WINEVENT_OUTOFCONTEXT  = 0x0000;

    /**
     * The foreground window has changed.
     * The system sends this event even if the foreground window has changed to another window in the same thread.
     * Server applications never send this event.
     * For this event, the WinEventProc callback function's hwnd parameter is the handle
     * to the window that is in the foreground, the idObject parameter is OBJID_WINDOW,
     * and the idChild parameter is CHILDID_SELF.
     */
    int EVENT_SYSTEM_FOREGROUND = 0x0003;

    /** BACKSPACE key. */
    int VK_BACK = 0x08;

    /** ENTER key. */
    int VK_RETURN = 0x0D;

    /** ESC key. */
    int VK_ESCAPE = 0x1B;

    /** LEFT ARROW key. */
    int VK_LEFT = 0x25;

    /** RIGHT ARROW key. */
    int VK_RIGHT = 0x27;

    /** DEL key. */
    int VK_DELETE = 0x2E;

    /** Next Track key. */
    int VK_MEDIA_NEXT_TRACK = 0xB0;

    /** Previous Track key. */
    int VK_MEDIA_PREV_TRACK = 0xB1;

    /** Stop Media key. */
    int VK_MEDIA_STOP = 0xB2;

    /** Play/Pause Media key. */
    int VK_MEDIA_PLAY_PAUSE = 0xB3;
}
