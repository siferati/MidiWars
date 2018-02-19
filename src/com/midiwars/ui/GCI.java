package com.midiwars.ui;

import static com.sun.jna.Pointer.nativeValue;
import static com.sun.jna.platform.win32.WinUser.*;
import static java.util.Locale.ENGLISH;

import com.midiwars.jna.MyUser32;
import com.midiwars.logic.MidiWars;
import com.midiwars.logic.instruments.Instrument;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinUser.MSG;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.KBDLLHOOKSTRUCT;
import com.sun.jna.platform.win32.WinUser.LowLevelKeyboardProc;
import org.xml.sax.SAXException;

import javax.sound.midi.InvalidMidiDataException;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Game Chat Interface.
 */
public class GCI implements UserInterface, LowLevelKeyboardProc {

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

                    // if chat is no longer opened
                    if (!open.get()) {
                        System.out.println("debug: CHAT CLOSED!");
                        synchronized (kbdEvents) {
                            parse();
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
                    // TODO construct string every time a new event is added (create a cursor to handle key presses)
                    kbdEvents.add(new KbdEvent(msg, vkCode));
                }
            }
        }

    } // KbdEventHandler


    /* --- DEFINES --- */

    /** Keybind to open in-game chat. */
    public static final int OPEN_CHAT = 0x0D;


    /* --- ATTRIBUTES --- */

    /** True when the user asks to quit the program, False otherwise. */
    private volatile boolean quit;

    /** Midi Wars app. */
    private MidiWars app;

    /** True if chat is opened, False otherwise. */
    private volatile SyncBoolean open;

    /** True if the {@link #OPEN_CHAT open chat} keybind is held down, False otherwise. */
    private volatile boolean openChatHeldDown;

    /** True when the keyboard events are being echoed, False otherwie. */
    private volatile boolean echoing;

    /** Handle to the hook procedure. */
    private HHOOK hHook;

    /** List of keyboard events detected while chat was active. */
    private final ArrayList<KbdEvent> kbdEvents;

    /** Used to simulate key presses. */
    private WinRobot robot;

    /** DLL. */
    private final MyUser32 user32;


    /* --- METHODS --- */

    /**
     * Creates a new GCI object.
     */
    public GCI() {

        try {
            app = new MidiWars();
        } catch (IOException | SAXException | ParserConfigurationException | Instrument.InvalidInstrumentException e) {
            // TODO
        }

        // inits
        quit = false;
        open = new SyncBoolean(false);
        openChatHeldDown = false;
        echoing = false;
        kbdEvents = new ArrayList<>();
        robot = new WinRobot();

        // dll
        user32 = MyUser32.INSTANCE;

        // install hook and get its handle
        hHook = user32.SetWindowsHookEx(WH_KEYBOARD_LL, this, null, 0);

        // program exit
        new Thread(() -> {

            // wait for user to quit the program
            while (!quit) Thread.onSpinWait();

            // free system resources
            user32.UnhookWindowsHookEx(hHook);

            System.exit(0);

        }).start();

        // --- NOTE ---
        // user32.GetMessage() never returns, causing the thread to block.
        // because of this, program exit happens in the above thread.

        // message loop
        MSG msg = new MSG();
        while(user32.GetMessage(msg, null, 0, 0) > 0) {

            user32.TranslateMessage(msg);
            user32.DispatchMessage(msg);
        }

        // free system resources
        user32.UnhookWindowsHookEx(hHook);

        System.exit(0);
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

        // delegate message processing
        Thread kbdEventHandler = new Thread(new KbdEventHandler(msg, vkCode));
        kbdEventHandler.start();

        return user32.CallNextHookEx(null, nCode, wParam, lParam);
    }


    /**
     * Creates a command string from the stored keyboard events
     * and parses it, in order to decide what to do.
     * Clears the list afterwards.
     */
    private void parse() {

        // user typed command
        StringBuilder cmdBuilder = new StringBuilder();

        for (KbdEvent e: kbdEvents) {

            int msg = e.msg;
            int vkCode = e.vkCode;

            // only interested in keydown events
            if (msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN) {

                // translate virtual-key code to char
                char c = (char) user32.MapVirtualKey(new UINT(vkCode), new UINT(2)).intValue();

                cmdBuilder.append(c);
            }
        }

        String cmd = cmdBuilder.toString();

        kbdEvents.clear();

        cmd = cmd.toLowerCase(ENGLISH);
        System.out.println("debug: CMD: " + cmd);

        // get command arguments
        String[] args = cmd.split("\\s+");

        // parse arguments
        Parser parser = new Parser(this);
        parser.parse(args);
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
        }

        echoing = false;
    }


    @Override
    public void displayUsage() {
        // TODO
    }


    @Override
    public void play(Instrument instrument, String filename) {
        try {
            app.play(instrument, filename);
        } catch (InvalidMidiDataException | IOException | AWTException | MidiWars.GameNotRunningException e) {
            // TODO
        }
    }


    @Override
    public void canPlay(Instrument instrument, String filename) {
        // TODO
    }


    @Override
    public void quit() {
        System.out.println("debug: Entered quit().");
        quit = true;
    }
}
