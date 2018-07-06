package com.midiwars.logic;

import com.midiwars.logic.instruments.Instrument;
import com.midiwars.logic.midi.MidiTimeline;
import com.midiwars.util.SyncInt;

import javax.sound.midi.InvalidMidiDataException;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import static com.midiwars.logic.Player.State.PAUSED;
import static com.midiwars.logic.Player.State.PLAYING;
import static com.midiwars.logic.Player.State.STOPPED;

/**
 * Represents a music player for midi files.
 */
public class Player {

    /**
     * The current state of the player.
     */
    public enum State {

        /** Playback is active. */
        PLAYING,

        /** Playback is paused. */
        PAUSED,

        /** Playback is stopped. */
        STOPPED

    } // State


    /* --- Defines --- */

    /** Amount of time (ms) to sleep in-between songs. */
    public static int BREAK_DURATION = 3000;


    /* --- Attrs --- */

    /** True when playback is active. */
    private volatile State state = STOPPED;

    /** True if next song to play is random. */
    private boolean shuffle;

    /** True if playlist should repeat upon ending. */
    private boolean repeat;

    /** List of midi files of this playlist. */
    private String[] playlist;

    /** The previous song that was played (used when shuffling). */
    private String prevSong;

    /** Instrument to play midi files with. */
    private Instrument instrument;

    /** The file that's currently playing. */
    private volatile SyncInt currentSong;

    /** The instance. */
    private static final Player instance = new Player();


    /* --- Methods --- */

    public static Player getInstance() {
        return instance;
    }

    private Player() {

        instrument = null;
        shuffle = false;
        repeat = false;
        playlist = null;
        prevSong = "none";
        currentSong = new SyncInt(0);
    }


    /**
     * Getter.
     *
     * @return The current state of the app.
     */
    public State getState() {
        return state;
    }


    /**
     * Resumes playback.
     */
    public void resume() throws AWTException, InvalidMidiDataException, IOException, InterruptedException {

        // unless it was stopped, changing state to playing is enough
        if (state != STOPPED) {
            state = PLAYING;
            return;
        }

        state = PLAYING;

        do {

            // shuffle playlist
            if (shuffle && !prevSong.equals("")) shuffle();

            for (; currentSong.get() < playlist.length; currentSong.increment()) {

                System.out.println(playlist[currentSong.get()]);

                // construct timeline from midi file
                MidiTimeline midiTimeline = new MidiTimeline(playlist[currentSong.get()]);

                // play
                instrument.play(midiTimeline);

                // stop playback
                if (state == STOPPED) {
                    return;
                }

                // update
                prevSong = playlist[currentSong.get()];

                // small break in-between songs
                if (repeat || currentSong.get() < playlist.length - 1) {
                    Thread.sleep(BREAK_DURATION);
                }
            }

            // prepare repetition
            currentSong.set(0);

        } while (repeat);

        state = STOPPED;
    }


    /**
     * Starts playback.
     */
    public void play(String[] playlist, boolean shuffle, boolean repeat, Instrument instrument) throws AWTException, InvalidMidiDataException, IOException, InterruptedException {

        // inits
        this.instrument = instrument;
        this.shuffle = shuffle;
        this.repeat = repeat;
        this.playlist = playlist;
        prevSong = "";
        currentSong = new SyncInt(0);

        // start playback
        resume();
    }


    public void stop() {
        state = STOPPED;
        prevSong = "none";
    }

    public void pause() {
        state = PAUSED;
    }


    /**
     * Shuffles the playlist,
     * following the Fisher–Yates shuffle algorithm.
     * Also sets currentSong to 0.
     */
    private void shuffle() {

        System.out.println("---------------");

        for (int i = 0; i < playlist.length - 1; i++) {

            int j = ThreadLocalRandom.current().nextInt(i, playlist.length);
            String temp = playlist[j];

            // prevent repeating previous song when starting new shuffle
            if (i == 0 && temp.equals(prevSong)) {
                i--;
                continue;
            }

            playlist[j] = playlist[i];
            playlist[i] = temp;
        }

        // reset currentSong
        currentSong.set(0);
    }


    /**
     * Plays the next midi file.
     */
    public void next() throws InterruptedException, AWTException, InvalidMidiDataException, IOException {

        if (playlist.length <= 1) {
            return;
        }

        State oldState = state;
        state = STOPPED;

        // small break in-between songs
        Thread.sleep(500);

        if (currentSong.get() >= playlist.length - 1) {
            currentSong.set(0);
        }
        else {
            currentSong.increment();
        }

        prevSong = "";
        if (oldState != STOPPED) {
            resume();
        }
    }


    /**
     * Plays previous midi file.
     */
    public void prev() throws InterruptedException, AWTException, InvalidMidiDataException, IOException {

        if (playlist.length <= 1) {
            return;
        }

        State oldState = state;
        state = STOPPED;

        // small break in-between songs
        Thread.sleep(500);

        if (currentSong.get() <= 0) {
            currentSong.set(playlist.length - 1);
        }
        else {
            currentSong.decrement();
        }

        prevSong = "";
        if (oldState != STOPPED) {
            resume();
        }
    }
}
