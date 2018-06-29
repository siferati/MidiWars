package com.midiwars.ui;

import com.midiwars.jna.MyUser32;
import com.midiwars.util.SyncBoolean;
import com.sun.jna.platform.win32.WinUser;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;

import static com.sun.jna.Pointer.nativeValue;
import static com.sun.jna.platform.win32.WinUser.*;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.awt.event.KeyEvent.VK_V;
import static java.awt.event.KeyEvent.VK_SHIFT;
import static java.awt.event.KeyEvent.VK_CONTROL;
import static java.awt.event.KeyEvent.VK_ALT;
import static java.awt.event.KeyEvent.VK_ALT_GRAPH;


import static java.util.Locale.ENGLISH;

/**
 * Represents the in-game chat.
 */
public class Chat implements WinUser.LowLevelKeyboardProc {

    /**
     * 'C++ Struct' that represents a keyboard event.
     */
    private class KbdEvent implements Runnable {

        /** Message tye. */
        public int msg;

        /** Virtual-key code. */
        public int vkCode;

        /** Hardware scan code. */
        public int scanCode;

        /** True if this event was injected, False otherwise. */
        public boolean injected;

        /** Keyboard state. */
        public byte[] lpKeyState;


        /**
         * Creates a new KbdEvent object.
         *
         * @param msg    Message type.
         * @param vkCode Virtual-key code.
         * @param scanCode Hardware scan code.
         * @param flags The extended-key flag, event-injected flags, context code, and transition-state flag.
         */
        public KbdEvent(int msg, int vkCode, int scanCode, int flags) {
            this.msg = msg;
            this.vkCode = vkCode;
            this.scanCode = scanCode;
            this.lpKeyState = new byte[256];
            injected = (flags & LLKHF_INJECTED) == LLKHF_INJECTED;
        }

        @Override
        public void run() {

            // prevent injected events from getting caught in the message loop
            if (injected) {
                return;
            }

            // (de)activate chat
            if (vkCode == WVK_RETURN) {
                if (msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN) {

                    if (openChatHeldDown.get()) {
                        // chat closes if key is held down
                        open.set(false);
                    } else {
                        open.swap();
                        openChatHeldDown.set(true);
                    }

                    // if chat is no longer opened
                    if (!open.get()) {
                        System.out.println("debug: CHAT CLOSED!");
                        buildCmd();
                    } else {
                        System.out.println("debug: CHAT OPEN!");
                    }
                }
                else if (msg == WM_KEYUP || msg == WM_SYSKEYUP) {
                    openChatHeldDown.set(false);
                }
            }
            // close chat and loose what was being typed
            else if (vkCode == VK_ESCAPE) {
                if (msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN) {
                    open.set(false);
                    openChatHeldDown.set(false);
                    synchronized (kbdEvents) {
                        kbdEvents.clear();
                    }
                    System.out.println("debug: CHAT CLOSED!");
                }
            }
            // other keys
            else if (open.get()) {
                synchronized (kbdEvents) {

                    // get current keyboard state
                    user32.GetKeyboardState(lpKeyState);

                    // store event
                    kbdEvents.add(this);
                }
            }
        }

    } // KbdEvent


    /* --- DEFINES --- */

    /** Flag for inject keyboard events. */
    public static final int LLKHF_INJECTED = 0x00000010;

    /** Enter virtual-key code (windows). */
    public static final int WVK_RETURN = 0x0D;

    /** Left arrow virtual-key code (windows). */
    public static final int WVK_LEFT = 0x25;

    /** Right arrow virtual-key code (windows). */
    public static final int WVK_RIGHT = 0x27;

    /** Backspace virtual-key code (windows). */
    public static final int WVK_BACK = 0x08;

    /** Delete virtual-key code (windows). */
    public static final int WVK_DELETE = 0x2E;

    /** List of chars to ignore when building input command. */
    public static final char[] INVALID_CHARS = {'\0', '\t', '\b', '\n', '\r'};


    /* --- ATTRIBUTES --- */

    /** The instance. */
    public static Chat instance;

    /** The user interface that owns this instance. */
    private UserInterface ui;

    /** True if chat is opened, False otherwise. */
    private volatile SyncBoolean open;

    /** True if the {@link #WVK_RETURN open chat} keybind is held down, False otherwise. */
    private volatile SyncBoolean openChatHeldDown;

    /** True while the method closeAndRestore is executing, False otherwise. */
    private volatile SyncBoolean closingAndRestoring;

    /** List of keyboard events detected while chat was active. */
    private final ArrayList<KbdEvent> kbdEvents;

    /** Used to simulate key presses. */
    private Robot robot;

    /** DLL. */
    private final MyUser32 user32;

    Clipboard clipboard;


    /* --- METHODS --- */

    /**
     * Creates a new Chat object.
     *
     * @param ui The user interface that owns this instance.
     */
    public Chat(UserInterface ui) throws AWTException {

        this.ui = ui;

        // inits
        open = new SyncBoolean(false);
        openChatHeldDown = new SyncBoolean(false);
        closingAndRestoring = new SyncBoolean(false);
        robot = new Robot();
        kbdEvents = new ArrayList<>();
        clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        // dll
        user32 = MyUser32.INSTANCE;

        instance = this;
    }


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
        int scanCode = kbdStruct.scanCode;
        int flags = kbdStruct.flags;

        // delegate message processing
        Thread kbdEventHandler = new Thread(new KbdEvent(msg, vkCode, scanCode, flags));
        kbdEventHandler.start();

        return user32.CallNextHookEx(null, nCode, wParam, lParam);
    }


    /** TODO when insert mode is turned on
     * Builds a string from the stored keyboard events.
     *
     * @return String built from the stored keyboard events.
     */
    private String buildString() {

        // current index of cursor
        int cursor = 0;

        // user typed string
        StringBuilder strBuilder = new StringBuilder();

        synchronized (kbdEvents) {

            for (KbdEvent e: kbdEvents) {

                int msg = e.msg;
                int vkCode = e.vkCode;
                int scanCode = e.scanCode;
                byte[] lpKeyState = e.lpKeyState;

                // only interested in keydown events
                if (msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN) {

                    // check for cursor movement and backspace/delete
                    switch (vkCode) {

                        case WVK_LEFT:
                            if (cursor > 0) cursor--;
                            break;

                        case WVK_RIGHT:
                            if (cursor < strBuilder.length()) cursor++;
                            break;

                        case WVK_BACK:
                            if (cursor > 0) strBuilder.deleteCharAt(--cursor);
                            break;

                        case WVK_DELETE:
                            if (cursor < strBuilder.length()) strBuilder.deleteCharAt(cursor);
                            break;

                        default:
                            break;
                    }

                    // translate virtual-key code to char
                    DWORDByReference lpChar = new DWORDByReference();
                    int result = user32.ToAscii(new UINT(vkCode), new UINT(scanCode), lpKeyState, lpChar, new UINT(0));

                    // init char as invalid
                    char c = '\0';

                    // get char
                    if (result == 1) {
                        c = (char) lpChar.getValue().intValue();
                    }

                    // check if char is valid
                    boolean valid = true;
                    for (char invalidChar : INVALID_CHARS) {
                        if (c == invalidChar) {
                            valid = false;
                            break;
                        }
                    }

                    // add char to command
                    if (valid) {
                        strBuilder.insert(cursor, c);
                        cursor++;
                    }
                }
            }
        }

        return strBuilder.toString();
    }


    /**
     * Builds a command string from the stored keyboard events
     * and parses it, in order to decide what to do.
     * Clears the list afterwards.
     */
    private void buildCmd() {

        // get cmd and make it case insensitive
        String cmd = buildString();
        cmd = cmd.toLowerCase(ENGLISH);

        System.out.println("debug: CMD: " + cmd);

        // reset list
        kbdEvents.clear();

        // get command arguments
        String[] args = cmd.split("\\s+");

        // parse arguments
        new Thread(() -> ui.parse(args)).start();
    }


    /** TODO move this to custom Robot class and call it for every keypress (and key release - test without this first)!
     * Releases modifier keys.
     */
    private void releaseModifiers() {
        if(user32.GetAsyncKeyState(VK_SHIFT) < 0) {
            robot.keyRelease(VK_SHIFT);
        }
        if(user32.GetAsyncKeyState(VK_CONTROL) < 0 && user32.GetAsyncKeyState(VK_ALT) < 0) {
            robot.keyRelease(VK_ALT_GRAPH);
        }
        else {
            if(user32.GetAsyncKeyState(VK_CONTROL) < 0) {
                robot.keyRelease(VK_CONTROL);
            }
            if(user32.GetAsyncKeyState(VK_ALT) < 0) {
                robot.keyRelease(VK_ALT);
            }
        }
    }

    /**
     * If the in-game chat is currently open,
     * closes it, presses the given key,
     * and re-opens it afterwards,
     * restoring its previous state.
     * Otherwise, just presses the given key.
     *
     * @param vkCode Virtual-key code of key to press.
     *
     * @return Amount of time slept (ms).
     */
    public long closeAndRestore(int vkCode) {

        closingAndRestoring.set(true);

        long x = System.currentTimeMillis();

        if (!open.get()) {
            releaseModifiers();
            robot.keyPress(vkCode);
            closingAndRestoring.set(false);
            return 0;
        }
        else {

            // build string from kbd events
            String str = buildString();

            if (str.equals("")) {
                // return;
            }

            // close chat
            releaseModifiers();
            robot.keyPress(VK_ESCAPE);
            robot.keyRelease(VK_ESCAPE);

            // press key
            releaseModifiers();
            robot.keyPress(vkCode);

            // re-open chat
            releaseModifiers();
            robot.keyPress(VK_ENTER);
            robot.keyRelease(VK_ENTER);

            // otherwise key presses aren't detected
            robot.delay(10);

            // copy string to type to clipboard
            StringSelection selection = new StringSelection(str);
            clipboard.setContents(selection, selection);

            // paste string to chat
            releaseModifiers();
            robot.keyPress(VK_CONTROL);
            robot.keyPress(VK_V);

            // otherwise key presses aren't detected
            robot.delay(10);

            // release pressed keys
            robot.keyRelease(VK_V);
            robot.keyRelease(VK_CONTROL);

            closingAndRestoring.set(false);
            return (System.currentTimeMillis() - x);
        }
    }
}
