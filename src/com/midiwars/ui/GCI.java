package com.midiwars.ui;

import static com.midiwars.jna.MyWinUser.WINEVENT_OUTOFCONTEXT;
import static com.midiwars.jna.MyWinUser.EVENT_SYSTEM_FOREGROUND;
import static com.sun.jna.platform.win32.WinUser.WH_KEYBOARD_LL;
import static java.awt.TrayIcon.MessageType.*;

import com.midiwars.jna.MyUser32;
import com.midiwars.logic.Chat;
import com.midiwars.logic.MidiWars;
import com.midiwars.logic.MidiWars.MidiPathNotFoundException;
import com.midiwars.logic.MidiWars.MidifilesNotFoundException;
import com.midiwars.logic.instruments.Instrument;
import com.midiwars.logic.instruments.Instrument.Warning;
import com.midiwars.logic.instruments.InstrumentFactory.InvalidInstrumentException;
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

            // inits
            app = new MidiWars();
            Chat.init(this);

            // dll
            user32 = MyUser32.INSTANCE;

            // check if the game is the current foreground window
            HWND hwnd = user32.GetForegroundWindow();
            active = getWindowTitle(hwnd).equals(GAME_WINDOW);

            // system tray icon
            trayIcon = addToSystemTray();

        } catch (AWTException | InvalidInstrumentException | IOException | MidiPathNotFoundException | ParserConfigurationException | SAXException e) {

            String msg;

            if (e instanceof AWTException) msg = "Couldn't add application to the system tray - desktop system tray is missing.";
            else if (e instanceof InvalidInstrumentException) msg = "Default instrument listed in the configurations file is invalid.";
            else if (e instanceof IOException) msg = "Configurations file is missing.";
            else if (e instanceof MidiPathNotFoundException) msg = "Default path listed in the configurations file is invalid.";
            else if (e instanceof ParserConfigurationException) msg = "There was a configuration error within the parser.";
            else msg = "Couldn't parse configurations file.";

            trayIcon.displayMessage("Application exit", msg, ERROR);

            freeSystemResources();
            System.exit(1);
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

            canPlay(instrument, filename, false);

            app.play(instrument, filename);

        } catch (AWTException | InterruptedException | InvalidMidiDataException | IOException | MidifilesNotFoundException | ParserConfigurationException | SAXException e) {

            String msg;

            if (e instanceof AWTException) msg = "Platform configuration does not allow low-level input control.";
            else if (e instanceof InterruptedException) msg = "A thread was interrupted.";
            else if (e instanceof InvalidMidiDataException) msg = "Invalid MIDI data was encountered. Please provide a valid MIDI file for playback.";
            else if (e instanceof IOException) msg = "Couldn't find the given MIDI file. Please provide a valid filename.";
            else if (e instanceof MidifilesNotFoundException) msg = "Couldn't find the MIDI files listed in the playlist. Please provide valid filenames.";
            else if (e instanceof ParserConfigurationException) msg = "There was a configuration error within the parser.";
            else msg = "Couldn't parse playlist file.";

            trayIcon.displayMessage("An error occurred", msg, ERROR);

            freeSystemResources();
            System.exit(1);
        }
    }


    @Override
    public void pause() {
        try {
            app.pause();
        } catch (InterruptedException e) {
            trayIcon.displayMessage("An error occurred", "A thread was interrupted.", ERROR);
            System.exit(1);
        }
    }


    @Override
    public void resume() {

        try {

            app.resume();

        } catch (AWTException | InterruptedException | InvalidMidiDataException | IOException e) {

            String msg;

            if (e instanceof AWTException) msg = "Platform configuration does not allow low-level input control.";
            else if (e instanceof InterruptedException) msg = "A thread was interrupted.";
            else if (e instanceof InvalidMidiDataException) msg = "Invalid MIDI data was encountered. Please provide a valid MIDI file for playback.";
            else msg = "Couldn't find the given MIDI file. Please provide a valid filename.";

            trayIcon.displayMessage("An error occurred", msg, ERROR);

            freeSystemResources();
            System.exit(1);
        }
    }


    @Override
    public void stop() {

        try {

            app.stop();

        } catch (InterruptedException e) {

            trayIcon.displayMessage("An error occurred", "A thread was interrupted.", ERROR);

            freeSystemResources();
            System.exit(1);
        }
    }


    @Override
    public void prev() {

        try {

            app.prev();

        } catch (AWTException | InterruptedException | InvalidMidiDataException | IOException e) {

            String msg;

            if (e instanceof AWTException) msg = "Platform configuration does not allow low-level input control.";
            else if (e instanceof InterruptedException) msg = "A thread was interrupted.";
            else if (e instanceof InvalidMidiDataException) msg = "Invalid MIDI data was encountered. Please provide a valid MIDI file for playback.";
            else msg = "Couldn't find the given MIDI file. Please provide a valid filename.";

            trayIcon.displayMessage("An error occurred", msg, ERROR);

            freeSystemResources();
            System.exit(1);
        }
    }


    @Override
    public void next() {

        try {

            app.next();

        } catch (AWTException | InterruptedException | InvalidMidiDataException | IOException e) {

            String msg;

            if (e instanceof AWTException) msg = "Platform configuration does not allow low-level input control.";
            else if (e instanceof InterruptedException) msg = "A thread was interrupted.";
            else if (e instanceof InvalidMidiDataException) msg = "Invalid MIDI data was encountered. Please provide a valid MIDI file for playback.";
            else msg = "Couldn't find the given MIDI file. Please provide a valid filename.";

            trayIcon.displayMessage("An error occurred", msg, ERROR);

            freeSystemResources();
            System.exit(1);
        }
    }


    /** TODO for loop in case its playlist
     * Checks if the given midi file can be played by the given instrument.
     *
     * @param instrument Instrument to play given file with.
     * @param filename Name of midi file to play.
     * @param outside True if this was called from the outside (ie user), False otherwise.
     */
    private void canPlay(Instrument instrument, String filename, boolean outside) {

        try {

            ArrayList<Warning> warnings = app.canPlay(instrument, filename);

            if (warnings.size() == 0 && outside) {
                trayIcon.displayMessage("No problems found", "MIDI file is ready for playback.", NONE);
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

            trayIcon.displayMessage("Warning", strBuilder.substring(1), WARNING);

        } catch (InvalidMidiDataException | IOException e) {

            String msg;

            if (e instanceof InvalidMidiDataException) msg = "Invalid MIDI data was encountered. Please provide a valid MIDI file for playback.";
            else msg = "Couldn't find the given MIDI file. Please provide a valid filename.";

            trayIcon.displayMessage("An error occurred", msg, ERROR);

            freeSystemResources();
            System.exit(1);
        }
    }


    @Override
    public void canPlay(Instrument instrument, String filename) {
        canPlay(instrument, filename, true);
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

        active = false;

        try {
            app.stop();
        } catch (InterruptedException e) {
            trayIcon.displayMessage("An error occurred", "A thread was interrupted.", ERROR);
            freeSystemResources();
            System.exit(1);
        }

        freeSystemResources();
        System.exit(0);
    }
}
