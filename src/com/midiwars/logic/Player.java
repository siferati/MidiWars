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
    public static final int BREAK_DURATION = 3000;

    /** Amount of time (ms) to sleep when moving to next or previous song. */
    public static final int SMALL_BREAK_DURATION = 500;


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

    /** Note (index) to resume playback from. */
    private int resumeNote;

    /** The thread that is currently playing. */
    private Thread currentPlayingThread;

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
        prevSong = "";
        currentSong = new SyncInt(0);
        resumeNote = 0;
        currentPlayingThread = null;
    }


    /**
     * Getter.
     *
     * @return The current state of the app.
     */
    public State getState() {
        return state;
    }


    /** TODO restore clipboard contents
     * Resumes playback.
     */
    public void resume() throws AWTException, InvalidMidiDataException, IOException, InterruptedException {

        if (state == PLAYING || currentPlayingThread != null) {
            return;
        }

        currentPlayingThread = Thread.currentThread();

        state = PLAYING;

        // true if this is loop's first iteration, false otherwise.
        boolean firstIte = true;

        do {

            // shuffle playlist
            if (shuffle && resumeNote == 0) shuffle(firstIte);

            for (; currentSong.get() < playlist.length; currentSong.increment()) {

                // make sure it's a valid index
                if (resumeNote < 0){
                    resumeNote = 0;
                }

                // update
                prevSong = playlist[currentSong.get()];

                // construct timeline from midi file
                MidiTimeline midiTimeline = new MidiTimeline(playlist[currentSong.get()]);

                // play
                resumeNote = instrument.play(midiTimeline, resumeNote);

                // playback was stopped
                if (resumeNote >= 0) {
                    return;
                }

                // prepare next song
                resumeNote = 0;

                // small break in-between songs
                if (repeat || currentSong.get() < playlist.length - 1) {
                    Thread.sleep(BREAK_DURATION);
                }
            }

            // prepare repetition
            currentSong.set(0);
            firstIte = false;

        } while (repeat);

        state = STOPPED;

        currentPlayingThread = null;
    }


    /**
     * Starts playback.
     */
    public void play(String[] playlist, boolean shuffle, boolean repeat, Instrument instrument) throws AWTException, InvalidMidiDataException, IOException, InterruptedException {

        // stop playback
        if (state == PLAYING) {
            stop();
        }

        // inits
        this.instrument = instrument;
        this.shuffle = shuffle;
        this.repeat = repeat;
        this.playlist = playlist;
        prevSong = "";
        currentSong = new SyncInt(0);
        resumeNote = 0;

        // start playback
        resume();
    }


    /**
     * Stops playback.
     */
    public void stop() throws InterruptedException {

        state = STOPPED;

        // wait for playing thread to terminate
        if (currentPlayingThread != null) {
            currentPlayingThread.join();
            currentPlayingThread = null;
        }

        // needed in case player was paused
        resumeNote = 0;
    }


    /**
     * Pauses playback.
     */
    public void pause() throws InterruptedException {

        state = PAUSED;

        // wait for playing thread to terminate
        if (currentPlayingThread != null) {
            currentPlayingThread.join();
            currentPlayingThread = null;
        }
    }


    /**
     * Shuffles the playlist,
     * following the Fisher–Yates shuffle algorithm.
     * Also sets currentSong to 0.
     *
     * @param repeat True if the previous song should be the first song in the playlist,
     *               False if it should NOT be the first song in the playlist.
     */
    private void shuffle(boolean repeat) {

        for (int i = 0; i < playlist.length - 1; i++) {

            int j = ThreadLocalRandom.current().nextInt(i, playlist.length);
            String temp = playlist[j];

            // make sure first song of playlist is as wanted
            if (i == 0 && !prevSong.isEmpty() && (!repeat && temp.equals(prevSong) || repeat && !temp.equals(prevSong))) {
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
     * Plays either the next or the previous midi file.
     *
     * @param next True to play next midi file, False to play previous midi file.
     */
    private void nextOrPrev(boolean next) throws InterruptedException, AWTException, InvalidMidiDataException, IOException {

        if (playlist.length <= 1) {
            return;
        }

        long startTime = System.currentTimeMillis();

        State oldState = state;

        // stop playback
        stop();

        // next song
        if (next) {
            if (currentSong.get() >= playlist.length - 1) {
                currentSong.set(0);
            }
            else {
                currentSong.increment();
            }
        }
        // prev song
        else {
            if (currentSong.get() <= 0) {
                currentSong.set(playlist.length - 1);
            }
            else {
                currentSong.decrement();
            }
        }

        // prevent shuffling
        if (oldState != STOPPED) {
            resumeNote = -1;
        }
        else {
            // update for shuffle
            prevSong = playlist[currentSong.get()];
        }

        // resume playback
        if (oldState == PLAYING) {

            // small break in-between songs
            long sleep = SMALL_BREAK_DURATION - (System.currentTimeMillis() - startTime);
            if (sleep > 0) {
                Thread.sleep(sleep);
            }

            resume();
        }
    }


    /**
     * Plays the next midi file.
     */
    public void next() throws InterruptedException, AWTException, InvalidMidiDataException, IOException {
        nextOrPrev(true);
    }


    /**
     * Plays previous midi file.
     */
    public void prev() throws InterruptedException, AWTException, InvalidMidiDataException, IOException {
        nextOrPrev(false);
    }
}
