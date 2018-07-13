package com.midiwars.ui.gci;

import com.midiwars.jna.MyUser32;
import com.midiwars.logic.Player;
import com.sun.jna.platform.win32.WinUser;
import com.midiwars.ui.UserInterface;
import com.midiwars.util.Pair;
import com.midiwars.util.SyncBoolean;

import java.util.ArrayList;

import static com.midiwars.jna.MyWinUser.*;

import static com.midiwars.logic.Player.State.PLAYING;
import static com.sun.jna.Pointer.nativeValue;

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
            // (for some reason, media keys are considered injected by the OS...)
            if (injected && vkCode != VK_MEDIA_NEXT_TRACK && vkCode != VK_MEDIA_PREV_TRACK && vkCode != VK_MEDIA_PLAY_PAUSE && vkCode != VK_MEDIA_STOP) {
                return;
            }

            switch (vkCode) {

                // (de)activate chat
                case VK_RETURN: {
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
                            buildCmd();
                        } else {
                            System.out.println("debug: CHAT OPEN!");
                        }
                    }
                    else if (msg == WM_KEYUP || msg == WM_SYSKEYUP) {
                        openChatHeldDown = false;
                    }
                    break;
                }

                // close chat and loose what was being typed
                case VK_ESCAPE: {
                    if (msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN) {
                        open.set(false);
                        openChatHeldDown = false;
                        synchronized (kbdEvents) {
                            kbdEvents.clear();
                        }
                        System.out.println("debug: CHAT CLOSED!");
                    }
                    break;
                }

                case VK_MEDIA_PLAY_PAUSE: {
                    if (msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN) {
                        if (Player.getInstance().getState() == PLAYING) {
                            ui.pause();
                        } else {
                            ui.resume();
                        }
                    }
                    break;
                }

                case VK_MEDIA_NEXT_TRACK: {
                    if (msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN) {
                        ui.next();
                    }
                    break;
                }

                case VK_MEDIA_PREV_TRACK: {
                    if (msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN) {
                        ui.prev();
                    }
                    break;
                }

                case VK_MEDIA_STOP: {
                    if (msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN) {
                        ui.stop();
                    }
                    break;
                }

                // other keys
                default: {
                    if (open.get()) {
                        synchronized (kbdEvents) {

                            // wait until it's safe
                            while (holdGetKeyboardState) {
                                Thread.onSpinWait();
                            }

                            // get current keyboard state
                            user32.GetKeyboardState(lpKeyState);

                            // store event
                            kbdEvents.add(this);
                        }
                    }
                    break;
                }
            }
        }

    } // KbdEvent


    /* --- DEFINES --- */

    /** List of chars to ignore when building input command. */
    public static final char[] INVALID_CHARS = {'\0', '\t', '\b', '\n', '\r'};


    /* --- ATTRIBUTES --- */

    /** True if GetKeyboardState can't be called at the moment. */
    public volatile boolean holdGetKeyboardState;

    /** The user interface that owns this instance. */
    private UserInterface ui;

    /** True if chat is opened, False otherwise. */
    private volatile SyncBoolean open;

    /** True if the open chat keybind is held down, False otherwise. */
    private volatile boolean openChatHeldDown;

    /** List of keyboard events detected while chat was active. */
    private final ArrayList<KbdEvent> kbdEvents;

    /** DLL. */
    private final MyUser32 user32;

    /** The instance. */
    private static final Chat instance = new Chat();


    /* --- METHODS --- */


    /**
     * Get the instance.
     *
     * @return The instance.
     */
    public static Chat getInstance() {
        return instance;
    }


    /**
     * Creates a new Chat object.
     */
    private Chat() {

        this.ui = UserInterface.getInstance();

        // inits
        open = new SyncBoolean(false);
        openChatHeldDown = false;
        kbdEvents = new ArrayList<>();
        holdGetKeyboardState = false;

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


    /**
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

                        case VK_LEFT:
                            if (cursor > 0) cursor--;
                            break;

                        case VK_RIGHT:
                            if (cursor < strBuilder.length()) cursor++;
                            break;

                        case VK_BACK:
                            if (cursor > 0) strBuilder.deleteCharAt(--cursor);
                            break;

                        case VK_DELETE:
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
        if (args.get(0).equals(UserInterface.CMD_GCI)) {
            args.remove(0);
            ui.parse(args.toArray(new String[0]));
        }
    }


    /**
     * Getter.
     *
     * @return True if chat is open, False otherwise.
     */
    public boolean isOpen() {
        return open.get();
    }


    /**
     * Setter.
     *
     * @param holdGetKeyboardState New value for holdGetKeyboardState.
     */
    public void setHoldGetKeyboardState(boolean holdGetKeyboardState) {
        this.holdGetKeyboardState = holdGetKeyboardState;
    }
}
