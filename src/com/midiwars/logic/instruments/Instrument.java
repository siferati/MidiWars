package com.midiwars.logic.instruments;

import com.midiwars.logic.Keymap;
import com.midiwars.logic.midi.MidiTimeline;
import com.midiwars.logic.midi.Note;
import com.midiwars.logic.midi.NoteEvent;

import javax.sound.midi.ShortMessage;
import java.awt.*;
import java.util.ArrayList;

/**
 * Represents a musical instrument. TODO refactor previous note into current skill bar (enum low, medium, high)
 */
public abstract class Instrument {

    /* --- DEFINES --- */

    /** Amount of time robot sleeps after an octave change (ms). */
    public static int ROBOT_SLEEP = 50;

    // TODO cooldown between octave changes

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
    private Robot robot; // TODO robot.delay() = Thread.sleep(), so each holding note needs its own thread


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

                // change keybars if needed
                delay -= changeKeybars(keybarIndex);

                // play note
                keybind = Keymap.KEYBINDS[getKeyIndex(noteEvent.getKey())];
                robot.keyPress(keybind);

                System.out.println("debug: Played: " + noteEvent);

                // if can't hold notes, release key
                if (!canHold) {
                    robot.keyRelease(keybind);
                }
            }

            // case NOTE_OFF
            else if (canHold) {
                // assuming note positions are the same between keybars
                keybind = Keymap.KEYBINDS[getKeyIndex(noteEvent.getKey())];
                robot.keyRelease(keybind);
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

        // how many keybars (octaves) are necessary to change
        int deltaKeybarIndex = keybarIndex - activeKeybarIndex;

        int keybind;
        for (int j = 0; j < Math.abs(deltaKeybarIndex); j++) {

            // up
            if (deltaKeybarIndex > 0) {

                keybind = Keymap.OCTAVEUP_KEYBIND;
                robot.keyPress(keybind);
                robot.keyRelease(keybind);

                System.out.println("debug: Octave Up");
            }
            // down
            else {

                keybind = Keymap.OCTAVEDOWN_KEYBIND;
                robot.keyPress(keybind);
                robot.keyRelease(keybind);

                System.out.println("debug: Octave Down");
            }

            // needed for octave change to take effect
            robot.delay(ROBOT_SLEEP);

            // update delay
            sleptAmount += ROBOT_SLEEP;
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

        for (int k : keybars[activeKeybarIndex]) {
            if (k == key) {
                return activeKeybarIndex;
            }
        }

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
     * Returns the index of given key (note) in the active keybar.
     *
     * @param key Key to assess.
     *
     * @return Key index. -1 if key doesn't belong to active key bar.
     */
    private int getKeyIndex(int key) {

        for (int i = 0; i < keybars[activeKeybarIndex].length; i++) {
            if (key == keybars[activeKeybarIndex][i]) {
                return i;
            }
        }

        return -1;
    }
}
