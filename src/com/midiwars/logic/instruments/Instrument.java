package com.midiwars.logic.instruments;

import com.midiwars.logic.Keymap;
import com.midiwars.logic.midi.MidiTimeline;
import com.midiwars.logic.midi.NoteEvent;

import javax.sound.midi.ShortMessage;
import java.awt.*;
import java.util.ArrayList;

/**
 * Represents a musical instrument.
 */
public abstract class Instrument {

    /* --- DEFINES --- */

    /** Amount of time robot sleeps after a key bar change (ms). */
    public static int ROBOT_SLEEP = 50;

    /** Minimum amount of time needed in-between key bar changes (ms). */
    public static int KEYBAR_COOLDOWN = 200;


    /* --- ATTRIBUTES --- */

    /** Note of the instrument. */
    private final String name;

    /** True if the instrument can hold notes (ie note duration matters). */
    private final boolean canHold;

    /** Each line represents a key bar (in-game skill bar - usually an octave) and its slots. */
    private final int[][] keybars;

    /** The active key bar before and after the midi timeline is played. */
    private final int idleKeybarIndex;

    /** The currently active key bar. */
    private int activeKeybarIndex;

    /** Robot used to simulate system inputs. */
    private Robot robot;

    /** System time of the previous key bar change (ms). */
    private long previousKeybarChange;

    /** The keyboard key that is currently being held down. */
    private int heldKeybind;


    /* --- METHODS --- */

    /**
     * Constructor.
     *
     * @param name Note of the instrument.
     * @param canHold If the instrument can hold notes.
     * @param keybars Each line represents a skill bar (usually an octave).
     * @param idleKeybarIndex The active key bar before and after the midi timeline is played.
     */
    public Instrument(String name, boolean canHold, int [][] keybars, int idleKeybarIndex) {

        this.name = name;
        this.canHold = canHold;
        this.keybars = keybars;
        this.idleKeybarIndex = idleKeybarIndex;
        activeKeybarIndex = idleKeybarIndex;
        robot = null;
        previousKeybarChange = -1;
        heldKeybind = -1;
    }


    /**
     * Plays the given midi timeline.
     *
     * @param midiTimeline Midi Timeline.
     *
     * @throws AWTException If the platform configuration does not allow low-level input control.
     */
    public void play(MidiTimeline midiTimeline) throws AWTException {

        ArrayList<NoteEvent> timeline = midiTimeline.getTimeline();

        robot = new Robot();

        for (int i = 0; i < timeline.size(); i++) {

            NoteEvent noteEvent = timeline.get(i);
            int keybarIndex = getKeybarIndex(noteEvent.getKey());

            // ignore if note can't be played
            if (keybarIndex < 0) {
                continue;
            }

            // amount to sleep until next noteEvent
            int delay = 0;

            // check if this isn't the last noteEvent
            if (i < timeline.size() - 1) {
                delay = timeline.get(i+1).getTimestamp() - noteEvent.getTimestamp();
            }

            int keybind;

            // case NOTE_ON
            if (noteEvent.getType() == ShortMessage.NOTE_ON) {

                // if there's a key being held down
                if (heldKeybind > -1) {
                    robot.keyRelease(heldKeybind);
                }

                // change keybars if needed
                delay -= changeKeybars(keybarIndex);

                // play note
                keybind = Keymap.KEYBINDS[getKeyIndex(noteEvent.getKey())];
                robot.keyPress(keybind);
                heldKeybind = keybind;

                System.out.println("debug: Played: " + noteEvent);

                // if there's time, look into the future and preemptively change key bars if needed
                if (delay > 0 && !canHold) {
                    delay -= preemptivelyChangeKeybars(i, timeline);
                }
            }

            // case NOTE_OFF
            else {
                System.out.println("debug: Releasing: " + noteEvent);
                keybind = Keymap.KEYBINDS[getKeyIndex(noteEvent.getKey())];
                if (keybind == heldKeybind) {
                    robot.keyRelease(keybind);
                    heldKeybind = -1;

                    // canHold instruments can only preemptively change key bars if there's no held key atm
                    // (ie if a key was just released)
                    if (delay > 0 && canHold) {
                        delay -= preemptivelyChangeKeybars(i, timeline);
                    }
                }
            }

            // sleep until next event
            if (delay > 0) {
                robot.delay(delay);
            }
        }

        // return to idle keybar
        changeKeybars(idleKeybarIndex);
    }


    /**
     * Looks into the future (starting from given position) to preemptively change keybars if needed.
     *
     * @param i Index of starting position in the timeline.
     * @param timeline Midi Timeline.
     *
     * @return Returns the amount of time (ms) robot slept.
     */
    public int preemptivelyChangeKeybars(int i, ArrayList<NoteEvent> timeline) {

        for (int j = i; j < timeline.size() - 1; j++) {

            // get next note event
            NoteEvent nextNoteEvent = timeline.get(j+1);

            // only interested in NOTE_ON events
            if (nextNoteEvent.getType() == ShortMessage.NOTE_ON) {

                int nextKeybarIndex = getKeybarIndex(nextNoteEvent.getKey());

                // make sure key to play is within instrument's range
                if (nextKeybarIndex >= 0) {

                    // change keybars if needed
                    return changeKeybars(nextKeybarIndex);
                }
            }
        }

        return 0;
    }


    /**
     * Checks if the given midi timeline can be played by this instrument
     * (ie if every note is contained in this instrument's key bars).
     *
     * @param midiTimeline Timeline to assess.
     *
     * @return True if instrument can play it, False otherwise.
     */
    public boolean canPlay(MidiTimeline midiTimeline) {

        for (NoteEvent noteEvent : midiTimeline.getTimeline()) {

            boolean found = false;

            search:
            for (int[] keybar : keybars) {
                for (int key : keybar) {
                    if (key == noteEvent.getKey()) {
                        found = true;
                        break search;
                    }
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }


    /**
     * Changes the active keybar to the given one.
     *
     * @param keybarIndex New active keybar.
     *
     * @return Returns the amount of time (ms) robot slept.
     */
    private int changeKeybars(int keybarIndex) {

        int sleptAmount = 0;

        // how many key bars are necessary to change
        int deltaKeybarIndex = keybarIndex - activeKeybarIndex;

        int keybind;
        for (int j = 0; j < Math.abs(deltaKeybarIndex); j++) {

            // amount of time needed to wait before changing key bar (ms)
            int wait = 0;

            // check if it's not the first time
            if (previousKeybarChange > -1) {

                // how much time passed since the previous key bar change (ms)
                int deltaKeybarChange = (int) (System.currentTimeMillis() - previousKeybarChange);

                // check if it needs to wait before changing key bar
                if (deltaKeybarChange < KEYBAR_COOLDOWN) {
                    wait = KEYBAR_COOLDOWN - deltaKeybarChange;
                }

                robot.delay(wait);
            }

            // decide key bar change direction
            if (deltaKeybarIndex > 0) {
                keybind = Keymap.OCTAVEUP_KEYBIND;
                System.out.println("debug: KEYBAR_UP");
            } else {
                keybind = Keymap.OCTAVEDOWN_KEYBIND;
                System.out.println("debug: KEYBAR_DOWN");
            }

            // if there's a key being held down
            if (heldKeybind > -1) {
                robot.keyRelease(heldKeybind);
                // key bar changes immediately release key
                heldKeybind = -1;
            }

            // change key bar
            robot.keyPress(keybind);

            // update previous key bar change time
            previousKeybarChange = System.currentTimeMillis();

            // needed for key bar change to take effect
            robot.delay(ROBOT_SLEEP);

            // called after delay() to provide small visual feedback in-game
            robot.keyRelease(keybind);

            // update delay
            sleptAmount += ROBOT_SLEEP + wait;
        }

        // update active keybar index
        activeKeybarIndex = keybarIndex;

        return sleptAmount;
    }


    /**
     * Returns the index of the keybar given key (note) belongs to,
     * taking into account active key bar, in order to avoid
     * unnecessary keybar (octave) changes.
     *
     * @param key Key to assess.
     *
     * @return Key bar index. -1 if note can't be played.
     */
    private int getKeybarIndex(int key) {

        // check active key bar first,
        // in order to avoid unnecessary changes
        for (int k : keybars[activeKeybarIndex]) {
            if (k == key) {
                return activeKeybarIndex;
            }
        }

        // other key bars
        for (int i = 0; i < keybars.length; i++) {
            // this was already tested above
            if (i == activeKeybarIndex) {
                continue;
            }

            for (int k : keybars[i]) {
                if (k == key) {
                    return i;
                }
            }
        }

        // can't play this note
        return -1;
    }


    /**
     * Returns the index of given key (note) in a keybar.
     *
     * @param key Key to assess.
     *
     * @return Key index. -1 if note can't be played.
     */
    private int getKeyIndex(int key) {

        // check active key bar first,
        // since its the most common occurrence
        for (int i = 0; i < keybars[activeKeybarIndex].length; i++) {
            if (key == keybars[activeKeybarIndex][i]) {
                return i;
            }
        }

        // other key bars
        for (int i = 0; i < keybars.length; i++) {
            // this was already tested above
            if (i == activeKeybarIndex) {
                continue;
            }

            for (int j = 0; j < keybars[i].length; j++) {
                if (keybars[i][j] == key) {
                    return j;
                }
            }
        }

        // can't play this note
        return -1;
    }
}
