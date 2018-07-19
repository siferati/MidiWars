package com.midiwars.util;

import com.midiwars.jna.MyUser32;
import com.midiwars.ui.gci.Chat.KbdEvent;
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTR;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinUser.INPUT;

import static com.midiwars.jna.MyWinUser.LLKHF_EXTENDED;
import static com.sun.jna.platform.win32.WinUser.INPUT.INPUT_KEYBOARD;
import static com.sun.jna.platform.win32.WinUser.KEYBDINPUT.KEYEVENTF_EXTENDEDKEY;
import static com.sun.jna.platform.win32.WinUser.KEYBDINPUT.KEYEVENTF_KEYUP;
import static com.sun.jna.platform.win32.WinUser.KEYBDINPUT.KEYEVENTF_SCANCODE;
import static com.sun.jna.platform.win32.WinUser.WM_KEYUP;
import static com.sun.jna.platform.win32.WinUser.WM_SYSKEYUP;

/**
 * Synthesizes keyboard input events through the Windows Api.
 */
public class WinRobot {

    /* --- ATTRIBUTES --- */

    /** User32.dll. */
    private final MyUser32 user32;


    /* --- METHODS --- */

    /**
     * Creates a new WinRobot object.
     */
    public WinRobot() {
        user32 = MyUser32.INSTANCE;
    }


    /**
     * Synthesizes a WM_KEYDOWN message for the given virtual-key code.
     *
         * @param vkCode Virtual-key code.
     */
    public void keyPress(int vkCode) {

        INPUT input = new INPUT();

        input.type = new DWORD(INPUT_KEYBOARD);
        input.input.setType("ki");
        input.input.ki.wVk = new WORD(vkCode);
        input.input.ki.wScan = new WORD(0);
        input.input.ki.dwFlags = new DWORD(0);
        input.input.ki.time = new DWORD(0);
        input.input.ki.dwExtraInfo = new ULONG_PTR(0);

        user32.SendInput(new DWORD(1), (INPUT[]) input.toArray(1), input.size());
    }


    /**
     * Synthesizes a WM_KEYUP message for the given virtual-key code.
     *
     * @param vkCode Virtual-key code.
     */
    public void keyRelease(int vkCode) {

        INPUT input = new INPUT();

        input.type = new DWORD(INPUT_KEYBOARD);
        input.input.setType("ki");
        input.input.ki.wVk = new WORD(vkCode);
        input.input.ki.wScan = new WORD(0);
        input.input.ki.dwFlags = new DWORD(KEYEVENTF_KEYUP);
        input.input.ki.time = new DWORD(0);
        input.input.ki.dwExtraInfo = new ULONG_PTR(0);

        user32.SendInput(new DWORD(1), (INPUT[]) input.toArray(1), input.size());
    }


    /**
     * Synthesises keyboard input, replicating the given event.
     *
     * @param kbdEvent Event to replicate.
     */
    public void replicate(KbdEvent kbdEvent) {

        INPUT input = new INPUT();

        int extended = ((kbdEvent.flags & LLKHF_EXTENDED) == LLKHF_EXTENDED) ? KEYEVENTF_EXTENDEDKEY : 0;
        int action = (kbdEvent.msg == WM_KEYUP || kbdEvent.msg == WM_SYSKEYUP) ? KEYEVENTF_KEYUP : 0;

        input.type = new DWORD(INPUT_KEYBOARD);
        input.input.setType("ki");
        input.input.ki.wVk = new WORD(kbdEvent.vkCode);
        input.input.ki.wScan = new WORD(kbdEvent.scanCode);
        input.input.ki.dwFlags = new DWORD(extended | action | KEYEVENTF_SCANCODE);
        input.input.ki.time = new DWORD(kbdEvent.time);
        input.input.ki.dwExtraInfo = kbdEvent.dwExtraInfo;

        user32.SendInput(new DWORD(1), (INPUT[]) input.toArray(1), input.size());
    }
}