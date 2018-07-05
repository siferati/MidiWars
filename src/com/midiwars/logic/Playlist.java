package com.midiwars.logic;

import com.midiwars.logic.instruments.Instrument;
import com.midiwars.ui.Chat;
import com.midiwars.util.SyncInt;

import javax.sound.midi.InvalidMidiDataException;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static com.midiwars.logic.MidiWars.State.STOPPED;

/**
 * Represents a playlist of midi files to play.
 */
public class Playlist {

    private class PlayedMidifile {
        public SyncInt index;
        public String midifile;
        public PlayedMidifile(SyncInt index, String midifile) {
            this.index = index;
            this.midifile = midifile;
        }
    }

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

    /** List of midi files left to play on shuffle. */
    private volatile ArrayList<String> leftMidifiles;

    /** List of midifiles played so far on shuffle. */
    private volatile ArrayList<PlayedMidifile> playedMidifiles;

    /** Instrument to play midi files with. */
    private Instrument instrument;

    /** The in-game chat. */
    private Chat chat;

    /** The file that's currently playing. */
    private volatile SyncInt iMidifile;


    /* --- Methods --- */

    public Playlist(ArrayList<String> midifiles, boolean shuffle, boolean repeat, Instrument instrument, Chat chat) {

        this.midifiles = midifiles;
        this.shuffle = shuffle;
        this.repeat = repeat;
        this.instrument = instrument;
        this.chat = chat;
        iMidifile = new SyncInt(0);
        leftMidifiles = new ArrayList<>();
        playedMidifiles = new ArrayList<>();
    }


    /**
     * Starts playback.
     */
    public void play(MidiWars app) throws AWTException, InvalidMidiDataException, MidiWars.GameNotRunningException, IOException, InterruptedException {

        // prevent playing same file twice in a row when repeating shuffle
        String lastMidifilePlayed = "";

        do {

            // play midifiles in order
            if (!shuffle) {

                for (iMidifile.set(0); iMidifile.get() < midifiles.size(); iMidifile.increment()) {

                    app.play(instrument, midifiles.get(iMidifile.get()), chat, true);

                    // check playback status
                    boolean stopped = false;
                    while (MidiWars.getState() == STOPPED) {
                        Thread.onSpinWait();
                        stopped = true;
                    }

                    // repeat song if stopped
                    if (stopped) {
                        iMidifile.decrement();
                    }

                    // small break in-between songs
                    if (!stopped && (repeat || iMidifile.get() < midifiles.size() - 1)) {
                        Thread.sleep(BREAK_DURATION);
                    }
                }
            }
            // play random midifiles
            else {

                // resets
                leftMidifiles = new ArrayList<>(midifiles);
                playedMidifiles.clear();

                // true if playback stopped
                boolean stopped = false;

                // play a random midifile
                while (!leftMidifiles.isEmpty()) {

                    // only swap song if didnt stop
                    if (!stopped) {

                        stopped = false;

                        // prevent playing same file twice in a row when repeating shuffle
                        do {
                            iMidifile.set(ThreadLocalRandom.current().nextInt(leftMidifiles.size()));
                        } while (leftMidifiles.get(iMidifile.get()).equals(lastMidifilePlayed) && midifiles.size() > 1);
                    }

                    // play midifile
                    lastMidifilePlayed = leftMidifiles.get(iMidifile.get());
                    app.play(instrument, lastMidifilePlayed, chat, true);

                    // check playback status
                    while (MidiWars.getState() == STOPPED) {
                        Thread.onSpinWait();
                        stopped = true;
                    }

                    if (!stopped) {
                        // remove song from waiting list
                        leftMidifiles.remove(iMidifile.get());

                        // add it to played list
                        playedMidifiles.add(new PlayedMidifile(iMidifile, lastMidifilePlayed));

                        // small break in-between songs
                        if (repeat || !leftMidifiles.isEmpty()) {
                            Thread.sleep(BREAK_DURATION);
                        }
                    }
                }
            }
        } while (repeat);
    }


    /**
     * Plays the next midi file.
     */
    public void next() throws InterruptedException {

        // small break in-between songs
        Thread.sleep(BREAK_DURATION);

        if (!shuffle) {
            if (iMidifile.get() >= midifiles.size() - 1) {
                if (repeat) {
                    iMidifile.set(0);
                }
            }
            else {
                iMidifile.increment();
            }
        }
        else if (leftMidifiles.size() > 1) {

            // remove song from waiting list and add it to played list
            playedMidifiles.add(new PlayedMidifile(iMidifile, leftMidifiles.remove(iMidifile.get())));

            // get next song
            iMidifile.set(ThreadLocalRandom.current().nextInt(leftMidifiles.size()));
        }
    }


    /**
     * Plays previous midi file.
     */
    public void prev() throws InterruptedException {

        // small break in-between songs
        Thread.sleep(BREAK_DURATION);

        if (!shuffle) {
            if (iMidifile.get() <= 0) {
                if (repeat) {
                    iMidifile.set(midifiles.size() - 1);
                }
            }
            else {
                iMidifile.decrement();
            }
        }
        else if (!playedMidifiles.isEmpty()) {
            int iLast = playedMidifiles.size() - 1;
            PlayedMidifile last = playedMidifiles.remove(iLast);
            leftMidifiles.add(last.index.get(), last.midifile);
            iMidifile.set(last.index.get());
        }
    }
}
