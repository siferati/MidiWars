package com.midiwars.ui;

import static com.sun.jna.platform.win32.WinUser.*;

import com.midiwars.jna.MyUser32;
import com.midiwars.logic.Chat;
import com.midiwars.logic.MidiWars;
import com.midiwars.logic.Player;
import com.midiwars.logic.instruments.Instrument;
import com.midiwars.logic.instruments.InstrumentFactory;
import com.sun.jna.platform.win32.WinUser.MSG;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import org.xml.sax.SAXException;

import javax.sound.midi.InvalidMidiDataException;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.IOException;

/**
 * Game Chat Interface.
 */
public class GCI extends UserInterface {

    /* --- ATTRIBUTES --- */

    /** Midi Wars app. */
    private MidiWars app;

    /** Handle to the hook procedure. */
    private HHOOK hHook;

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

        // install hook and get its handle
        hHook = user32.SetWindowsHookEx(WH_KEYBOARD_LL, Chat.getInstance(), null, 0);

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


    @Override
    public void displayUsage() {
        // TODO
    }


    @Override
    public void play(Instrument instrument, String filename) {
        try {
            app.play(instrument, filename);
        } catch (InvalidMidiDataException | IOException | AWTException | MidiWars.GameNotRunningException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    @Override
    public void playlist(Instrument instrument, String filename) {
        try {
            app.playlist(instrument, filename);
        } catch (InvalidMidiDataException | IOException | AWTException | MidiWars.GameNotRunningException | ParserConfigurationException | MidiWars.MidifilesNotFoundException | SAXException | InterruptedException e) {
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
            Player.getInstance().stop();
        } catch (InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
        finally {
            // free system resources
            user32.UnhookWindowsHookEx(hHook);
        }

        System.exit(0);
    }
}
