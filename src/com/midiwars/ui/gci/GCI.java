package com.midiwars.ui.gci;

import static com.midiwars.jna.MyWinUser.WINEVENT_OUTOFCONTEXT;
import static com.midiwars.jna.MyWinUser.EVENT_SYSTEM_FOREGROUND;
import static com.sun.jna.platform.win32.WinUser.WH_KEYBOARD_LL;
import static java.awt.TrayIcon.MessageType.*;

import com.midiwars.jna.MyUser32;
import com.midiwars.logic.MidiWars;
import com.midiwars.ui.UserInterface;
import com.midiwars.util.MyExceptions.MidiPathNotFoundException;
import com.midiwars.util.MyExceptions.MidifilesNotFoundException;
import com.midiwars.logic.instruments.Instrument;
import com.midiwars.logic.instruments.Instrument.Warning;
import com.midiwars.util.MyExceptions.InvalidInstrumentException;
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
import java.util.ArrayList;

/**
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

            // inits
            app = new MidiWars();

            // dll
            user32 = MyUser32.INSTANCE;

            // check if the game is the current foreground window
            HWND hwnd = user32.GetForegroundWindow();
            active.set(getWindowTitle(hwnd).equals(GAME_WINDOW));

            // system tray icon
            trayIcon = addToSystemTray();

        } catch (AWTException e) {
            displayError(true, "Couldn't add application to the system tray.\nDesktop system tray is missing.");
        } catch (InvalidInstrumentException e) {
            displayError(true, "Default instrument listed in the configurations file is invalid.");
        } catch (IOException e) {
            displayError(true, "couldn't extract configurations file from resources.");
        } catch (MidiPathNotFoundException e) {
            displayError(true, "Default path listed in the configurations file is invalid.");
        } catch (NullPointerException e) {
            displayError(true, "Configurations file doesn't have required format.");
        } catch (ParserConfigurationException e) {
            displayError(true, "There was a configuration error within the parser.");
        } catch (SAXException e) {
            displayError(true, "Couldn't parse configurations file.");
        }

        // install hooks and get their handles
        hHook = active.get() ? user32.SetWindowsHookEx(WH_KEYBOARD_LL, Chat.getInstance(), null, 0) : null;
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
     * Displays the given error message.
     *
     * @param crit True if it's a critical error and the app should shutdown.
     * @param msg Message to display.
     */
    private void displayError(boolean crit, String msg) {

        String caption = crit ? "Critical Error" : "Error";

        if (trayIcon != null) {
            trayIcon.displayMessage(caption, msg, NONE);
        } else {
            System.out.println(caption + ": " + msg);
        }

        if (crit) {
            freeSystemResources();
            System.exit(1);
        }
    }


    /**
     * Displays the given warnings.
     *
     * @param warnings List of warnings to display.
     */
    public void displayWarnings(ArrayList<Warning> warnings ) {

        if (warnings.size() == 0) {
            return;
        }

        StringBuilder strBuilder = new StringBuilder();

        for (Warning warning: warnings) {
            switch (warning) {
                case NOT_IN_RANGE:
                    strBuilder.append("\nSome notes are unplayable.");
                    break;
                case TEMPO_TOO_FAST:
                    strBuilder.append("\nTempo is too fast.");
                    break;
                case NOTES_TOO_LONG:
                    strBuilder.append("\nSome notes are too long.");
                    break;
                case PAUSES_TOO_LONG:
                    strBuilder.append("\nSome pauses are too long.");
                    break;
                default:
                    strBuilder.append("\nUnknown warning.");
                    break;
            }
        }

        if (trayIcon != null) {
            trayIcon.displayMessage("Warning", strBuilder.substring(1), NONE);
        } else {
            System.out.println("Warning: " + strBuilder.substring(1));
        }
    }


    /**
     * Adds the app to the system tray.
     *
     * @return The icon shown in the system tray.
     *
     * @throws AWTException If the desktop system tray is missing.
     */
    private TrayIcon addToSystemTray() throws AWTException {

        if (!SystemTray.isSupported()) {
            return null;
        }

        // popup menu
        PopupMenu popup = new PopupMenu();
        MenuItem item = new MenuItem("quit");
        item.addActionListener(e -> quit());
        popup.add(item);

        Image image;
        String tooltip;
        synchronized (active) {

            // load image
            String filename = active.get() ? ACTIVE_ICON : INACTIVE_ICON;
            image = Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource(filename));

            // tooltip
            tooltip = active.get() ? "(active)" : "(inactive)";
            tooltip = APP_NAME + " " + tooltip;

            // tray icon
            TrayIcon trayIcon = new TrayIcon(image, tooltip, popup);
            trayIcon.setImageAutoSize(true);

            // add icon to system tray
            SystemTray.getSystemTray().add(trayIcon);

            return trayIcon;
        }
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

        synchronized (active) {

            // no need to do stuff if value of active didn't change
            boolean newActive = getWindowTitle(hwnd).equals(GAME_WINDOW);
            if (newActive == active.get()) {
                return;
            }

            // check if the game is the current foreground window
            active.set(newActive);

            // pause playback and uninstall the hook
            if (!active.get()) {
                pause();
                user32.UnhookWindowsHookEx(hHook);
                hHook = null;
            }
            // re-install the hook
            else {
                hHook = user32.SetWindowsHookEx(WH_KEYBOARD_LL, Chat.getInstance(), null, 0);
            }

            // update tray icon image
            String filename = active.get() ? ACTIVE_ICON : INACTIVE_ICON;
            trayIcon.setImage(Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource(filename)));

            // update tray icon tooltip
            String tooltip = active.get() ? "(active)" : "(inactive)";
            tooltip = APP_NAME + " " + tooltip;
            trayIcon.setToolTip(tooltip);
        }
    }


    @Override
    public void displayUsage() {

        if (trayIcon != null) {
            trayIcon.displayMessage("Invalid command", "Usage: /mw <command> [options]", NONE);
        } else {
            System.out.println("Invalid command. Usage: /mw <command> [options]");
        }
    }


    @Override
    public void play(Instrument instrument, String filename) {
        try {
            app.play(instrument, filename);
        } catch (AWTException e) {
            displayError(true, "Platform configuration does not allow low-level input control.");
        } catch (InterruptedException e) {
            displayError(true, "A thread was interrupted.");
        } catch (InvalidMidiDataException e) {
            displayError(false, "Invalid MIDI data was encountered.\nPlease provide a valid MIDI file for playback.");
        } catch (IOException e) {
            displayError(false, "Couldn't find the given MIDI file.\nPlease provide a valid filename.");
        } catch (MidifilesNotFoundException e) {
            displayError(false, "Couldn't find the MIDI files listed in the playlist.\nPlease provide valid filenames.");
        } catch (ParserConfigurationException e) {
            displayError(false, "There was a configuration error within the parser.");
        } catch (SAXException e) {
            displayError(false, "Couldn't parse playlist file.");
        }
    }


    @Override
    public void pause() {
        try {
            app.pause();
        } catch (InterruptedException e) {
            displayError(true, "A thread was interrupted.");
        }
    }


    @Override
    public void resume() {
        try {
            app.resume();
        } catch (AWTException e) {
            displayError(true, "Platform configuration does not allow low-level input control.");
        } catch (InterruptedException e) {
            displayError(true, "A thread was interrupted.");
        } catch (InvalidMidiDataException e) {
            displayError(false, "Invalid MIDI data was encountered.\nPlease provide a valid MIDI file for playback.");
        } catch (IOException e) {
            displayError(false, "Couldn't find the given MIDI file.\nPlease provide a valid filename.");
        }
    }


    @Override
    public void stop() {
        try {
            app.stop();
        } catch (InterruptedException e) {
            displayError(true, "A thread was interrupted.");
        }
    }


    @Override
    public void prev() {
        try {
            app.prev();
        } catch (AWTException e) {
            displayError(true, "Platform configuration does not allow low-level input control.");
        } catch (InterruptedException e) {
            displayError(true, "A thread was interrupted.");
        } catch (InvalidMidiDataException e) {
            displayError(false, "Invalid MIDI data was encountered.\nPlease provide a valid MIDI file for playback.");
        } catch (IOException e) {
            displayError(false, "Couldn't find the given MIDI file.\nPlease provide a valid filename.");
        }
    }


    @Override
    public void next() {
        try {
            app.next();
        } catch (AWTException e) {
            displayError(true, "Platform configuration does not allow low-level input control.");
        } catch (InterruptedException e) {
            displayError(true, "A thread was interrupted.");
        } catch (InvalidMidiDataException e) {
            displayError(false, "Invalid MIDI data was encountered.\nPlease provide a valid MIDI file for playback.");
        } catch (IOException e) {
            displayError(false, "Couldn't find the given MIDI file.\nPlease provide a valid filename.");
        }
    }

    @Override
    public void canPlay(Instrument instrument, String filename, boolean explicit) {

        if (!explicit) {
            filename = filename.replace(app.getMidiPath(), "");
        }

        try {
            ArrayList<Warning> warnings = app.canPlay(instrument, filename);
            if (warnings.size() == 0 && explicit) {
                trayIcon.displayMessage("No problems found", "MIDI file is ready for playback.", NONE);
            } else {
                displayWarnings(warnings);
            }
        } catch (InvalidMidiDataException e) {
            displayError(false, "Invalid MIDI data was encountered.\nPlease provide a valid MIDI file for playback.");
        } catch (IOException e) {
            displayError(false, "Couldn't find the given MIDI file.\nPlease provide a valid filename.");
        }
    }


    /**
     * Frees whatever system resources were being used.
     */
    private void freeSystemResources() {

        if (hHook != null) user32.UnhookWindowsHookEx(hHook);
        if (hWinEventHook != null) user32.UnhookWinEvent(hWinEventHook);
        if (trayIcon != null) SystemTray.getSystemTray().remove(trayIcon);
    }


    @Override
    public void quit() {

        active.set(false);

        try {
            app.stop();
        } catch (InterruptedException e) {
            displayError(true, "A thread was interrupted.");
        }

        if (trayIcon != null) trayIcon.displayMessage("Application exit", "Goodbye.", NONE);

        freeSystemResources();
        System.exit(0);
    }
}
