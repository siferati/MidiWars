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

    /* --- ATTRIBUTES --- */

    /** Midi Wars app. */
    private MidiWars app;

    /** Handle to the hook procedure. */
    private HHOOK hHook;

    /** Handle to the event hook instance. */
    private HANDLE hWinEventHook;

    /** DLL. */
    private MyUser32 user32;

    /** The icon shown in the system tray. */
    private TrayIcon trayIcon;


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

        // system tray icon
        try {
            trayIcon = addToSystemTray();
        } catch (AWTException e) {
            // TODO
            e.printStackTrace();
        }

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
     * Adds the app to the system tray.
     *
     * @return The icon shown in the system tray.
     */
    private TrayIcon addToSystemTray() throws AWTException {

        if (!SystemTray.isSupported()) {
            return null;
        }

        // popup menu
        PopupMenu popup = new PopupMenu();
        MenuItem item1 = new MenuItem("Quit");
        item1.addActionListener(e -> quit());
        popup.add(item1);

        // load image
        String filename = active ? ACTIVE_ICON : INACTIVE_ICON;
        Image image = Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource(filename));

        // tooltip
        String tooltip = active ? "(active)" : "(inactive)";
        tooltip = APP_NAME + " " + tooltip;

        // tray icon
        TrayIcon trayIcon = new TrayIcon(image, tooltip, popup);
        trayIcon.setImageAutoSize(true);

        // add icon to system tray
        SystemTray.getSystemTray().add(trayIcon);

        return trayIcon;
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

        System.out.println(active);

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

        // update tray icon image
        String filename = active ? ACTIVE_ICON : INACTIVE_ICON;
        trayIcon.setImage(Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource(filename)));

        // update tray icon tooltip
        String tooltip = active ? "(active)" : "(inactive)";
        tooltip = APP_NAME + " " + tooltip;
        trayIcon.setToolTip(tooltip);
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

        active = false;

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

        SystemTray.getSystemTray().remove(trayIcon);
        System.exit(0);
    }
}
