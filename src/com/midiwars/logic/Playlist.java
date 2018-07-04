package com.midiwars.logic;

import com.midiwars.logic.instruments.Instrument;
import com.midiwars.ui.Chat;

import javax.sound.midi.InvalidMidiDataException;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a playlist of midi files to play.
 */
public class Playlist {

    /* --- Defines --- */

    /** Amount of time (ms) to sleep in-between songs. */
    public static int BREAK_DURATION = 3000;

    /* --- Attrs --- */

    /** True if next song to play is random. */
    private boolean shuffle;

    /** True if playlist should repeat upon ending. */
    private boolean repeat;

    /** List of midi files of this playlist. */
    private ArrayList<String> midifiles;

    /** Instrument to play midi files with. */
    private Instrument instrument;

    /** The in-game chat. */
    private Chat chat;

    /** The file that's currently playing. */
    private int iMidifile;

    /** True if playlist is in play mode. */
    private boolean playing = false;


    /* --- Methods --- */

    public Playlist(ArrayList<String> midifiles, boolean shuffle, boolean repeat, Instrument instrument, Chat chat) {

        this.midifiles = midifiles;
        this.shuffle = shuffle;
        this.repeat = repeat;
        this.instrument = instrument;
        this.chat = chat;
        iMidifile = 0;
        playing = false;
    }


    /**
     * Starts playback.
     */
    public void play(MidiWars app) throws AWTException, InvalidMidiDataException, MidiWars.GameNotRunningException, IOException, InterruptedException {

        playing = true;

        do {
            if (!shuffle) {
                for (iMidifile = 0; iMidifile < midifiles.size(); iMidifile++) {
                    app.play(instrument, midifiles.get(iMidifile), chat);
                    // small break in-between songs
                    if (repeat || iMidifile < midifiles.size() - 1) {
                        Thread.sleep(BREAK_DURATION);
                    }
                }
            } else {
                // TODO shuffle
            }
        } while (repeat);

        playing = false;
    }


    /**
     * Resumes playback.
     */
    public void resume() {

    }

    public void pause() {

    }

    public void stop() {

    }

    public void next() {

    }

    public void prev() {

    }
}
