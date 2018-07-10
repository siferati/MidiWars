package com.midiwars.ui;

import static com.midiwars.jna.MyWinUser.WINEVENT_OUTOFCONTEXT;
import static com.midiwars.jna.MyWinUser.EVENT_SYSTEM_FOREGROUND;
import static com.sun.jna.platform.win32.WinUser.WH_KEYBOARD_LL;

import com.midiwars.jna.MyUser32;
import com.midiwars.logic.Chat;
import com.midiwars.logic.MidiWars;
import com.midiwars.logic.instruments.Instrument;
import com.midiwars.logic.instruments.InstrumentFactory;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.MSG;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import org.xml.sax.SAXException;

import javax.sound.midi.InvalidMidiDataException;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.IOException;

/** TODO move all custom exceptions to util package
 * Game Chat Interface.
 */
public class GCI extends UserInterface implements WinUser.WinEventProc {

    /* --- DEFINES --- */

    /** Name of the window of the game. */
    public final static String GAME_WINDOW = "Guild Wars 2";


    /* --- ATTRIBUTES --- */

    /** True if the game is the active window, False otherwise. */
    private volatile boolean active;

    /** Midi Wars app. */
    private MidiWars app;

    /** Handle to the hook procedure. */
    private HHOOK hHook;

    /** Handle to the event hook instance. */
    private HANDLE hWinEventHook;

    /** DLL. */
    private final MyUser32 user32;


    /* --- METHODS --- */

    /**
     * Creates a new GCI object.
     */
    public GCI() {

        try {
            app = new MidiWars();
            Chat.init(this);
        } catch (IOException | SAXException | ParserConfigurationException | InstrumentFactory.InvalidInstrumentException | MidiWars.MidiPathNotFoundException e) {
            // TODO
            e.printStackTrace();
        }

        // dll
        user32 = MyUser32.INSTANCE;

        // check if the game is the current foreground window
        HWND hwnd = user32.GetForegroundWindow();
        active = getWindowTitle(hwnd).equals(GAME_WINDOW);

        // install hooks and get their handles
        hHook = active ? user32.SetWindowsHookEx(WH_KEYBOARD_LL, Chat.getInstance(), null, 0) : null;
        hWinEventHook = user32.SetWinEventHook(EVENT_SYSTEM_FOREGROUND, EVENT_SYSTEM_FOREGROUND, null, this, 0, 0, WINEVENT_OUTOFCONTEXT);


        // --- NOTE ---
        // user32.GetMessage() never returns, causing the thread to block.
        // Because of this, program exit happens when some other thread calls quit()

        // message loop
        MSG msg = new MSG();
        while(user32.GetMessage(msg, null, 0, 0) > 0) {

            user32.TranslateMessage(msg);
            user32.DispatchMessage(msg);
        }

        // execution never gets this far
        quit();
    }


    /**
     * Returns the title of the given window.
     *
     * @param hwnd Window to check.
     *
     * @return Title of given window.
     */
    private String getWindowTitle(HWND hwnd) {

        char[] lpString = new char[user32.GetWindowTextLength(hwnd) + 1];
        user32.GetWindowText(hwnd, lpString, lpString.length);
        return new String(lpString, 0, lpString.length - 1);
    }


    @Override
    public void callback(HANDLE hWinEventHook, DWORD event, HWND hwnd, LONG idObject, LONG idChild, DWORD idEventThread,DWORD dwmsEventTime) {

        if (!event.equals(new DWORD(EVENT_SYSTEM_FOREGROUND)) || hwnd == null) {
            return;
        }

        // check if the game is the current foreground window
        active = getWindowTitle(hwnd).equals(GAME_WINDOW);

        // stop playback and uninstall the hook
        if (!active) {
            pause();
            user32.UnhookWindowsHookEx(hHook);
            hHook = null;
        }
        // re-install the hook
        else {
            hHook = user32.SetWindowsHookEx(WH_KEYBOARD_LL, Chat.getInstance(), null, 0);
        }
    }

    @Override
    public void displayUsage() {
        // TODO
    }

    @Override
    public void play(Instrument instrument, String filename) {
        try {
            app.play(instrument, filename);
        } catch (InvalidMidiDataException | IOException | AWTException | InterruptedException | ParserConfigurationException | MidiWars.MidifilesNotFoundException | SAXException e) {
            // TODO
            e.printStackTrace();
        }
    }

    @Override
    public void pause() {
        try {
            app.pause();
        } catch (InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    @Override
    public void resume() {
        try {
            app.resume();
        } catch (AWTException | InvalidMidiDataException | InterruptedException | IOException e) {
            // TODO
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            app.stop();
        } catch (InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    @Override
    public void prev() {
        try {
            app.prev();
        } catch (InterruptedException | AWTException | IOException | InvalidMidiDataException e) {
            // TODO
            e.printStackTrace();
        }
    }

    @Override
    public void next() {
        try {
            app.next();
        } catch (InterruptedException | AWTException | IOException | InvalidMidiDataException e) {
            // TODO
            e.printStackTrace();
        }
    }

    @Override
    public void canPlay(Instrument instrument, String filename) {
        // TODO
    }


    @Override
    public void quit() {
        try {
            app.stop();
        } catch (InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
        finally {
            // free system resources
            user32.UnhookWindowsHookEx(hHook);
            user32.UnhookWinEvent(hWinEventHook);
        }

        System.exit(0);
    }
}
