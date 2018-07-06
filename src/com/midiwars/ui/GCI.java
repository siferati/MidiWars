package com.midiwars.ui;

import static com.sun.jna.platform.win32.WinUser.*;

import com.midiwars.jna.MyUser32;
import com.midiwars.logic.MidiWars;
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

    /** True when the user asks to quit the program, False otherwise. */
    private volatile boolean quit;

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

        // inits
        quit = false;

        // dll
        user32 = MyUser32.INSTANCE;

        // install hook and get its handle
        hHook = user32.SetWindowsHookEx(WH_KEYBOARD_LL, Chat.getInstance(), null, 0);

        // program exit TODO apagar isto e ver se funciona
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
    void pause() {
        app.pause();
    }

    @Override
    void resume() {
        try {
            app.resume();
        } catch (AWTException | InvalidMidiDataException | InterruptedException | IOException e) {
            // TODO
            e.printStackTrace();
        }
    }

    @Override
    void stop() {
        app.stop();
    }

    @Override
    void prev() {
        try {
            app.prev();
        } catch (InterruptedException | AWTException | IOException | InvalidMidiDataException e) {
            // TODO
            e.printStackTrace();
        }
    }

    @Override
    void next() {
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
        quit = true;
    }
}
