package com.midiwars.ui;

import com.midiwars.jna.MyUser32;
import com.midiwars.util.Pair;
import com.midiwars.util.SyncBoolean;
import com.sun.jna.platform.win32.WinUser;

import java.util.ArrayList;

import static com.sun.jna.Pointer.nativeValue;
import static com.sun.jna.platform.win32.WinUser.*;
import static java.awt.event.KeyEvent.VK_ESCAPE;

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

    /** The user interface that owns this instance. */
    private UserInterface ui;

    /** True if chat is opened, False otherwise. */
    private volatile SyncBoolean open;

    /** True if the {@link #WVK_RETURN open chat} keybind is held down, False otherwise. */
    private volatile SyncBoolean openChatHeldDown;

    /** List of keyboard events detected while chat was active. */
    private final ArrayList<KbdEvent> kbdEvents;

    /** DLL. */
    private final MyUser32 user32;


    /* --- METHODS --- */

    /**
     * Creates a new Chat object.
     *
     * @param ui The user interface that owns this instance.
     */
    public Chat(UserInterface ui) {

        this.ui = ui;

        // inits
        open = new SyncBoolean(false);
        openChatHeldDown = new SyncBoolean(false);
        kbdEvents = new ArrayList<>();

        // dll
        user32 = MyUser32.INSTANCE;
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
    public Pair<String, Integer> buildString() {

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

        return new Pair<>(strBuilder.toString(), cursor);
    }


    /**
     * Builds a command string from the stored keyboard events
     * and parses it, in order to decide what to do.
     * Clears the list afterwards.
     */
    private void buildCmd() {

        // get cmd
        String cmd = buildString().first;
        System.out.println("debug: CMD: " + cmd);

        // reset list
        kbdEvents.clear();

        // get words in string
        String[] words = cmd.split("\\s+");

        // inits
        ArrayList<String> args = new ArrayList<>();
        boolean argHasSpaces = false;
        StringBuilder arg = new StringBuilder();

        // get args from command
        // there are so many ifs in order to try to emulate shell behaviour as best as possible,
        // including missing double quotes, nested double quotes, etc
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.equals("\"")) {
                argHasSpaces = true;
                arg = new StringBuilder(" ");
            } else if (word.startsWith("\"") && word.endsWith("\"")) {
                if (argHasSpaces) {
                    argHasSpaces = false;
                    arg.append(" ").append(word);
                    args.add(arg.substring(1, arg.length() - 1));
                } else {
                    args.add(word.substring(1, word.length() - 1));
                }
            } else if (word.startsWith("\"")) {
                argHasSpaces = true;
                arg = new StringBuilder(word);
                if (i == words.length - 1) {
                    args.add(arg.substring(1));
                }
            } else if (word.endsWith("\"")) {
                if (argHasSpaces) {
                    argHasSpaces = false;
                    arg.append(" ").append(word);
                    args.add(arg.substring(1, arg.length() - 1));
                } else {
                    argHasSpaces = true;
                    arg = new StringBuilder("\"" + word.substring(0, word.length() - 1));
                    if (i == words.length - 1) {
                        args.add(arg.substring(1));
                    }
                }
            } else {
                if (argHasSpaces) {
                    arg.append(" ").append(word);
                    if (i == words.length - 1) {
                        args.add(arg.substring(1));
                    }
                } else {
                    args.add(word);
                }
            }
        }

        // remove double quotes from arguments
        for (int i = 0; i < args.size(); i++) {
            args.set(i, args.get(i).replace("\"", ""));
        }

        // parse arguments
        new Thread(() -> ui.parse(args.toArray(new String[0]))).start();
    }


    /**
     * Getter.
     *
     * @return True if chat is open, False otherwise.
     */
    public boolean isOpen() {
        return open.get();
    }
}
