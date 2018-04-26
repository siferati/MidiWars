package com.midiwars.ui;

import com.midiwars.jna.MyUser32;
import com.midiwars.util.SyncBoolean;
import com.midiwars.util.WinRobot;
import com.sun.jna.platform.win32.WinUser;

import java.util.ArrayList;

import static com.sun.jna.Pointer.nativeValue;
import static com.sun.jna.platform.win32.WinUser.*;
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

        /* Hardware scan code. */
        public int scanCode;

        /* Keyboard state. */
        public byte[] lpKeyState;


        /**
         * Creates a new KbdEvent object.
         *
         * @param msg    Message type.
         * @param vkCode Virtual-key code.
         * @param scanCode Hardware scan code.
         */
        public KbdEvent(int msg, int vkCode, int scanCode) {
            this.msg = msg;
            this.vkCode = vkCode;
            this.scanCode = scanCode;
            this.lpKeyState = new byte[256];
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

                    // if chat is no longer opened
                    if (!open.get()) {
                        System.out.println("debug: CHAT CLOSED!");
                        synchronized (kbdEvents) {
                            buildCmd();
                        }
                    } else {
                        System.out.println("debug: CHAT OPEN!");
                    }
                }
                else if (msg == WM_KEYUP || msg == WM_SYSKEYUP) {
                    openChatHeldDown = false;
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

    /** Keybind to open in-game chat. TODO read from config.xml  */
    public static final int OPEN_CHAT = 0x0D;

    /** Left arrow virtual key code. */
    public static final int VK_LEFT = 0x25;

    /** Right arrow virtual key code. */
    public static final int VK_RIGHT = 0x27;

    /** Backspace virtual key code. */
    public static final int VK_BACK = 0x08;

    /** Delete virtual key code. */
    public static final int VK_DELETE = 0x2E;

    /** List of chars to ignore when building input command. */
    public static final char[] INVALID_CHARS = {'\0', '\t', '\b', '\n', '\r'};


    /* --- ATTRIBUTES --- */

    /* The user interface that owns this instance. */
    private UserInterface ui;

    /** True if chat is opened, False otherwise. */
    private volatile SyncBoolean open;

    /** True if the {@link #OPEN_CHAT open chat} keybind is held down, False otherwise. */
    private volatile boolean openChatHeldDown;

    /** True when the keyboard events are being echoed, False otherwise. */
    private volatile boolean echoing;

    /** List of keyboard events detected while chat was active. */
    private final ArrayList<KbdEvent> kbdEvents;

    /** Used to simulate key presses. */
    private WinRobot robot;

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
        openChatHeldDown = false;
        echoing = false;
        kbdEvents = new ArrayList<>();
        robot = new WinRobot();

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

        // delegate message processing
        Thread kbdEventHandler = new Thread(new KbdEvent(msg, vkCode, scanCode));
        kbdEventHandler.start();

        return user32.CallNextHookEx(null, nCode, wParam, lParam);
    }


    /** TODO when insert mode is turned on
     * Builds a command string from the stored keyboard events
     * and parses it, in order to decide what to do.
     * Clears the list afterwards.
     */
    private void buildCmd() {

        // current index of cursor
        int cursor = 0;

        // user typed command
        StringBuilder cmdBuilder = new StringBuilder();

        for (KbdEvent e: kbdEvents) {

            int msg = e.msg;
            int vkCode = e.vkCode;
            int scanCode = e.scanCode;
            byte[] lpKeyState = e.lpKeyState;

            // only interested in keydown events
            if (msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN) {

                // check for cursor movement and backspace/delete
                switch (vkCode) {

                    case VK_LEFT:
                        if (cursor > 0) cursor--;
                        break;

                    case VK_RIGHT:
                        if (cursor < cmdBuilder.length()) cursor++;
                        break;

                    case VK_BACK:
                        if (cursor > 0) cmdBuilder.deleteCharAt(--cursor);
                        break;

                    case VK_DELETE:
                        if (cursor < cmdBuilder.length()) cmdBuilder.deleteCharAt(cursor);
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
                    cmdBuilder.insert(cursor, c);
                    cursor++;
                }
            }
        }

        // lower case
        String cmd = cmdBuilder.toString();
        cmd = cmd.toLowerCase(ENGLISH);

        System.out.println("debug: CMD: " + cmd);

        // reset list
        kbdEvents.clear();

        // get command arguments
        String[] args = cmd.split("\\s+");

        // parse arguments
        ui.parse(args);
    }


    /**
     * Echoes every keyboard event stored in {@link #kbdEvents}.
     * Clears the list afterwards.
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
            // TODO
            e.printStackTrace();
        }

        echoing = false;
    }
}
