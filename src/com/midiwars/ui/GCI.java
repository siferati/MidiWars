package com.midiwars.ui;

import static com.sun.jna.Pointer.nativeValue;
import static com.sun.jna.platform.win32.WinUser.*;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinUser.MSG;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.KBDLLHOOKSTRUCT;
import com.sun.jna.platform.win32.WinUser.LowLevelKeyboardProc;

/**
 * Game Chat Interface.
 */
public class GCI {

    /**
     * Implementation of the message loop to listen to in-game key presses.
     */
    private class MessageLoop implements Runnable {

        @Override
        public void run() {

            // dll
            User32 user32 = User32.INSTANCE;

            // install hook and get its handle
            hHook = user32.SetWindowsHookEx(WH_KEYBOARD_LL, keyboardProc, null, 0);

            // message loop
            MSG msg = new MSG();
            while(user32.GetMessage(msg, null, 0, 0) > 0) {

                user32.TranslateMessage(msg);
                user32.DispatchMessage(msg);
            }

            // free system resources
            user32.UnhookWindowsHookEx(hHook);
        }
    }


    /**
     * Implementation of the callback for keyboard events.
     */
    private class KeyboardProc implements LowLevelKeyboardProc {

        /**
         * Implementation of the handling of a keyboard event.
         */
        private class KbdEventHandler implements Runnable {

            /** Keyboard message. */
            private final int msg;

            /** Virtual-key code. */
            private final int vkCode;

            /**
             * Creates a new KbdEventHandler object.
             *
             * @param msg    Message received.
             * @param vkCode Virtual-key code of key that generated the event.
             */
            public KbdEventHandler(int msg, int vkCode) {
                this.msg = msg;
                this.vkCode = vkCode;
            }

            @Override
            public void run() {

                if (msg == WM_KEYUP) {
                    System.out.println("debug: Key up: " + vkCode);
                } else if (msg == WM_KEYDOWN) {
                    System.out.println("debug: Key down: " + vkCode);
                } else if (msg == WM_SYSKEYUP) {
                    System.out.println("debug: System Key up: " + vkCode);
                } else if (msg == WM_SYSKEYDOWN) {
                    System.out.println("debug: System Key down: " + vkCode);

                }
            }
        }

        @Override
        public LRESULT callback(int nCode, WPARAM wParam, KBDLLHOOKSTRUCT kbdStruct) {

            // dll
            final User32 user32 = User32.INSTANCE;

            // lparam object
            LPARAM lParam = new LPARAM(nativeValue(kbdStruct.getPointer()));

            // no further processing allowed
            if (nCode < 0) {
                return user32.CallNextHookEx(null, nCode, wParam, lParam);
            }

            // get message and virtual-key code
            int msg = wParam.intValue();
            int vkCode = kbdStruct.vkCode;

            // delegate message processing
            Thread kbdEventHandler = new Thread(new KbdEventHandler(msg, vkCode));
            kbdEventHandler.start();

            return user32.CallNextHookEx(null, nCode, wParam, lParam);
        }
    }


    /* --- ATTRIBUTES --- */

    /** True if chat is opened (ie user pressed Enter), False otherwise. */
    private volatile boolean open;

    /** Handle to the hook procedure. */
    private HHOOK hHook;

    /** Callback for keyboard events. */
    private LowLevelKeyboardProc keyboardProc;

    /** Listener of the in-game chat. */
    private Thread chatListener;


    /* --- METHODS --- */

    public GCI() {

        // dll
        final User32 user32 = User32.INSTANCE;

        // keyboard procedure
        keyboardProc = new KeyboardProc();

        // create and start the chat listener
        chatListener = new Thread(new MessageLoop());
        chatListener.start();
    }
}
