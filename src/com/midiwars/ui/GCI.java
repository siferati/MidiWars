package com.midiwars.ui;

import static com.sun.jna.Pointer.nativeValue;
import static com.sun.jna.platform.win32.WinUser.*;

import com.midiwars.logic.WinRobot;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinUser.MSG;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.KBDLLHOOKSTRUCT;
import com.sun.jna.platform.win32.WinUser.LowLevelKeyboardProc;
import java.util.ArrayList;

/**
 * Game Chat Interface.
 */
public class GCI {

    /**
     * Represents a synchronized boolean,
     * allowing for atomic get(), set() and swap().
     */
    private class SyncBoolean {

        /** Primitive boolean value. */
        private boolean value;

        /**
         * Creates a new SyncBoolean object.
         *
         * @param value Initial value.
         */
        public SyncBoolean(boolean value) {
            this.value = value;
        }

        /**
         * Returns the current value.
         *
         * @return Current value.
         */
        public synchronized boolean get() {
            return value;
        }

        /**
         * Sets the current value to a new one.
         *
         * @param newValue New value.
         */
        public synchronized void set(boolean newValue) {
            value = newValue;
        }

        /**
         * Swaps the current value.
         */
        public synchronized void swap() {
            value = !value;
        }

    } // SyncBoolean


    /**
     * 'C++ Struct' that represents a keyboard event.
     */
    private class KbdEvent {

        /** Message tye. */
        public int msg;

        /** Virtual-key code. */
        public int vkCode;

        /**
         * Creates a new KbdEvent object.
         *
         * @param msg    Message type.
         * @param vkCode Virtual-key code.
         */
        public KbdEvent(int msg, int vkCode) {
            this.msg = msg;
            this.vkCode = vkCode;
        }

    } // KbdEvent


    /**
     * Implementation of the message loop to listen to in-game key presses.
     */
    private class MessageLoop implements Runnable {

        @Override
        public void run() {

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

    } // MessageLoop


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

                // prevent echoes from getting caught in the message loop
                if (echoing) {
                    return;
                }

                // (de)activate chat
                if (vkCode == OPEN_CHAT) {
                    if (msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN) {

                        if (openChatHeldDown) {
                            // chat closes if key is held down
                            open.set(false);
                        } else {
                            open.swap();
                            openChatHeldDown = true;
                        }

                        // if chat is no longer opened, echo
                        if (!open.get()) {
                            System.out.println("debug: CLOSED");
                            synchronized (kbdEvents) {
                                echo();
                            }
                        } else {
                            System.out.println("debug: OPEN");
                        }
                    }
                    else if (msg == WM_KEYUP || msg == WM_SYSKEYUP) {
                        openChatHeldDown = false;
                    }
                }
                // other keys
                else if (open.get()) {
                    synchronized (kbdEvents) {
                        kbdEvents.add(new KbdEvent(msg, vkCode));
                    }
                }
            }

        } // KbdEventHandler


        @Override
        public LRESULT callback(int nCode, WPARAM wParam, KBDLLHOOKSTRUCT kbdStruct) {

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

    } // KeyboardProc


    /* --- DEFINES --- */

    public static final int OPEN_CHAT = 0x0D;

    /* --- ATTRIBUTES --- */

    /** True if chat is opened, False otherwise. */
    private volatile SyncBoolean open;

    /** True if the {@link #OPEN_CHAT open chat} keybind is held down, False otherwise. */
    private volatile boolean openChatHeldDown;

    /** True when the keyboard events are being echoed, False otherwie. */
    private volatile boolean echoing;

    /** Handle to the hook procedure. */
    private HHOOK hHook;

    /** Callback for keyboard events. */
    private LowLevelKeyboardProc keyboardProc;

    /** Listener of the in-game chat. */
    private Thread chatListener;

    /** List of keyboard events detected while chat was active. */
    private final ArrayList<KbdEvent> kbdEvents;

    /** Used to simulate key presses. */
    private WinRobot robot;

    /** DLL. */
    private final User32 user32;


    /* --- METHODS --- */

    public GCI() {

        // inits
        open = new SyncBoolean(false);
        openChatHeldDown = false;
        echoing = false;
        kbdEvents = new ArrayList<>();
        robot = new WinRobot();

        // dll
        user32 = User32.INSTANCE;

        // keyboard procedure
        keyboardProc = new KeyboardProc();

        // create and start the chat listener
        chatListener = new Thread(new MessageLoop());
        chatListener.start();
    }


    /**
     * Echoes every keyboard event stored in {@link #kbdEvents}
     * and clears the list afterwards.
     */
    private void echo() {

        if (kbdEvents.size() == 0) {
            return;
        }

        // prevent echoes from getting caught in the message loop
        echoing = true;

        // open chat to echo
        robot.keyPress(OPEN_CHAT);
        robot.keyRelease(OPEN_CHAT);

        for (KbdEvent e: kbdEvents) {

            int msg = e.msg;
            int vkCode = e.vkCode;

            if (msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN) {
                robot.keyPress(vkCode);
            } else if (msg == WM_KEYUP || msg == WM_SYSKEYUP) {
                robot.keyRelease(vkCode);
            }
        }

        kbdEvents.clear();

        // close chat
        robot.keyPress(OPEN_CHAT);
        robot.keyRelease(OPEN_CHAT);

        // needed in order for above keypress to be ignored
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        echoing = false;
    }
}
