package com.midiwars.logic;

import com.midiwars.logic.instruments.Instrument;
import com.midiwars.logic.midi.MidiTimeline;
import com.midiwars.ui.UserInterface;

import javax.sound.midi.InvalidMidiDataException;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static com.midiwars.logic.Player.State.PAUSED;
import static com.midiwars.logic.Player.State.PLAYING;
import static com.midiwars.logic.Player.State.STOPPED;

/**
 * A music player for midi files. (Singleton)
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


    /* --- DEFINES --- */

    /** Amount of time (ms) to sleep in-between songs. */
    public static final int BREAK_DURATION = 3000;

    /** Amount of time (ms) to sleep when moving to next or previous song. */
    public static final int SMALL_BREAK_DURATION = 500;


    /* --- ATTRIBUTES --- */

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
    private final AtomicInteger currentSong;

    /** Note (index) to resume playback from. */
    private int resumeNote;

    /** The thread that is currently playing. */
    private Thread currentPlayingThread;

    /** The previous contents of the clipboard prior to playing. */
    private Transferable prevClipboardContents;

    /** The instance. */
    private static final Player instance = new Player();


    /* --- METHODS --- */

    /**
     * Getter.
     *
     * @return The {@link #instance}.
     */
    public static Player getInstance() {
        return instance;
    }


    /**
     * Creates a new Player object.
     */
    private Player() {

        instrument = null;
        shuffle = false;
        repeat = false;
        playlist = new String[0];
        prevSong = "";
        currentSong = new AtomicInteger(0);
        resumeNote = 0;
        currentPlayingThread = null;
        prevClipboardContents = null;
    }


    /**
     * Getter.
     *
     * @return The current {@link #state} of the app.
     */
    public State getState() {
        return state;
    }


    /**
     * Resumes playback.
     *
     * @throws AWTException If the platform configuration does not allow low-level input control.
     * @throws InterruptedException If a thread was interrupted.
     * @throws InvalidMidiDataException If midi file is invalid.
     * @throws IOException If can't open file.
     */
    public void resume() throws AWTException, InterruptedException, InvalidMidiDataException, IOException {

        if (state == PLAYING || currentPlayingThread != null) {
            return;
        }

        currentPlayingThread = Thread.currentThread();
        prevClipboardContents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

        state = PLAYING;

        // true if this is loop's first iteration, false otherwise.
        boolean firstIte = true;

        do {

            // shuffle playlist
            if (shuffle && resumeNote == 0) shuffle(firstIte);

            for (; currentSong.get() < playlist.length; currentSong.incrementAndGet()) {

                // make sure it's a valid index
                if (resumeNote < 0){
                    resumeNote = 0;
                }

                // update
                prevSong = playlist[currentSong.get()];

                // check for warnings
                if (resumeNote == 0) {
                    UserInterface.getInstance().canPlay(instrument, prevSong, false);
                }

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
        if (prevClipboardContents != null) Toolkit.getDefaultToolkit().getSystemClipboard().setContents(prevClipboardContents, null);
    }


    /**
     * Starts playback.
     *
     * @param playlist Playlist to play.
     * @param shuffle True if player should switch to shuffle mode.
     * @param repeat True if playlist should repeat upon ending.
     * @param instrument Instrument to play given playlist with.
     *
     * @throws AWTException If the platform configuration does not allow low-level input control.
     * @throws InterruptedException If a thread was interrupted.
     * @throws InvalidMidiDataException If midi file is invalid.
     * @throws IOException If can't open file.
     */
    public void play(String[] playlist, boolean shuffle, boolean repeat, Instrument instrument) throws AWTException, InterruptedException, InvalidMidiDataException, IOException {

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
        currentSong.set(0);
        resumeNote = 0;

        // start playback
        resume();
    }


    /**
     * Stops playback.
     *
     * @throws InterruptedException If a thread was interrupted.
     */
    public void stop() throws InterruptedException {

        state = STOPPED;

        // wait for playing thread to terminate
        if (currentPlayingThread != null) {
            currentPlayingThread.join();
            currentPlayingThread = null;
        }

        if (prevClipboardContents != null) Toolkit.getDefaultToolkit().getSystemClipboard().setContents(prevClipboardContents, null);

        // needed in case player was paused
        resumeNote = 0;
    }


    /**
     * Pauses playback.
     *
     * @throws InterruptedException If a thread was interrupted.
     */
    public void pause() throws InterruptedException {

        state = PAUSED;

        // wait for playing thread to terminate
        if (currentPlayingThread != null) {
            currentPlayingThread.join();
            currentPlayingThread = null;
        }

        if (prevClipboardContents != null) Toolkit.getDefaultToolkit().getSystemClipboard().setContents(prevClipboardContents, null);
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
     * @throws AWTException If the platform configuration does not allow low-level input control.
     * @throws InterruptedException If a thread was interrupted.
     * @throws InvalidMidiDataException If midi file is invalid.
     * @throws IOException If can't open file.
     *
     * @param next True to play next midi file, False to play previous midi file.
     */
    private void nextOrPrev(boolean next) throws AWTException, InterruptedException, InvalidMidiDataException, IOException {

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
                currentSong.incrementAndGet();
            }
        }
        // prev song
        else {
            if (currentSong.get() <= 0) {
                currentSong.set(playlist.length - 1);
            }
            else {
                currentSong.decrementAndGet();
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
     * Plays the next song.
     *
     * @throws AWTException If the platform configuration does not allow low-level input control.
     * @throws InterruptedException If a thread was interrupted.
     * @throws InvalidMidiDataException If midi file is invalid.
     * @throws IOException If can't open file.
     */
    public void next() throws AWTException, InterruptedException, InvalidMidiDataException, IOException {
        nextOrPrev(true);
    }


    /**
     * Plays previous song.
     *
     * @throws AWTException If the platform configuration does not allow low-level input control.
     * @throws InterruptedException If a thread was interrupted.
     * @throws InvalidMidiDataException If midi file is invalid.
     * @throws IOException If can't open file.
     */
    public void prev() throws AWTException, InterruptedException, InvalidMidiDataException, IOException {
        nextOrPrev(false);
    }
}
