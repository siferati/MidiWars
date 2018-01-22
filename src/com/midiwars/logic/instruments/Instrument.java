package com.midiwars.logic.instruments;

import com.midiwars.logic.Keymap;
import com.midiwars.logic.midi.MidiTimeline;
import com.midiwars.logic.midi.Note;

import java.awt.*;
import java.util.ArrayList;

import static com.midiwars.logic.instruments.Instrument.OctaveOp.*;
import static com.midiwars.logic.midi.Note.Name.C;

/**
 * Represents a musical instrument.
 */
public abstract class Instrument {

    /* --- DEFINES --- */

    /** Octave operations. */
    public enum OctaveOp {UP, DOWN}

    /** Amount of time robot sleeps in-between operations (ms). */
    public static int ROBOT_SLEEP = 10;


    /* --- ATTRIBUTES --- */

    /** Name of the instrument. */
    private final String name;

    /** Lower limit of the instrument's range. */
    private final Note lowestNote;

    /** Upper limit of the instrument's range. */
    private final Note highestNote;

    /** Indicates this instrument's state prior and after the midi timeline is played. */
    private final Note idleNote;

    /** The most recently played note. */
    private Note previousNote;

    /** True if the instrument can hold notes (ie note duration matters). */
    private final boolean canHold;

    /** Note that appears twice in a single octave (it's usually C) */
    private final Note.Name repeatedNote;


    /* --- METHODS --- */

    /**
     * Constructor.
     *
     * @param name Name of the instrument.
     * @param lowestNote Lower limit of the instrument's range.
     * @param highestNote Upper limit of the instrument's range.
     * @param idleNote Instrument state prior and after timeline is played.
     * @param canHold If the instrument can hold notes.
     */
    public Instrument(String name, Note lowestNote, Note highestNote, Note idleNote, Note.Name repeatedNote, boolean canHold) {

        this.name = name;
        this.lowestNote = lowestNote;
        this.highestNote = highestNote;
        this.idleNote = idleNote;
        this.previousNote = idleNote;
        this.repeatedNote = repeatedNote;
        this.canHold = canHold;
    }


    /**
     * Plays the given midi timeline.
     *
     * @param midiTimeline Notes to play.
     *
     * @throws AWTException If the platform configuration does not allow low-level input control.
     */
    public void play(MidiTimeline midiTimeline) throws AWTException {

        ArrayList<Note> timeline = midiTimeline.getTimeline();

        Keymap keymap = new Keymap();
        Robot robot = new Robot();
        robot.setAutoDelay(ROBOT_SLEEP);

        for (int i = 0; i < timeline.size(); i++) {

            Note note = timeline.get(i);

            int deltaOctave = note.getOctave() - previousNote.getOctave();

            // prevents unneeded octave changes
            if (Math.abs(deltaOctave) == 1 && note.getName() == repeatedNote) {
                deltaOctave = 0;
            }

            // note not in range, skip
            if (note.compareTo(lowestNote) < 0 || note.compareTo(highestNote) > 0)
            {
                System.out.println("debug: Not in range: " + note);
                continue;
            }

            // amount to sleep until next note
            int delay = 0;

            // check if this isn't the last note
            if (i < timeline.size() - 1) {
                delay = timeline.get(i+1).getTimestamp() - note.getTimestamp();
            }

            int keybind;

            // play note
            if (note.isOn()) {

                // change octaves if needed
                if (note.getName() != repeatedNote) {

                    for (int j = 0; j < Math.abs(deltaOctave); j++) {

                        // up
                        if (deltaOctave > 0) {

                            keybind = keymap.octaveOpToKeybind(UP);
                            robot.keyPress(keybind);
                            robot.keyRelease(keybind);

                            // update delay
                            delay -= ROBOT_SLEEP * 2;

                            System.out.println("debug: Octave Up");
                        }
                        // down
                        else {

                            keybind = keymap.octaveOpToKeybind(DOWN);
                            robot.keyPress(keybind);
                            robot.keyRelease(keybind);

                            // update delay
                            delay -= ROBOT_SLEEP * 2;

                            System.out.println("debug: Octave Down");
                        }
                    }
                }

                // play note
                keybind = keymap.noteToKeybind(note.getName());
                robot.keyPress(keybind);

                System.out.println("debug: Played: " + note);

                // update delay
                delay -= ROBOT_SLEEP;

                // if can't hold notes, release key after auto delay
                if (!canHold) {

                    robot.keyRelease(keybind);

                    // update delay
                    delay -= ROBOT_SLEEP;
                }
            }

            // release note
            else if (canHold) {

                keybind = keymap.noteToKeybind(note.getName());
                robot.keyRelease(keybind);

                // update delay
                delay -= ROBOT_SLEEP;
            }

            if (delay > 0) {
                robot.delay(delay);
            }

            // prepare next ite
            previousNote = note;
        }

        // return to idle note's octave
        // code copied from above
        int deltaOctave = idleNote.getOctave() - previousNote.getOctave();
        int keybind;
        for (int j = 0; j < Math.abs(deltaOctave); j++) {
            if (deltaOctave < 0) {
                keybind = keymap.octaveOpToKeybind(UP);
                robot.keyPress(keybind);
                robot.keyRelease(keybind);
            } else {
                keybind = keymap.octaveOpToKeybind(UP);
                robot.keyPress(keybind);
                robot.keyRelease(keybind);
            }
        }
    }


    /**
     * Checks if the given midi timeline can be played by this instrument
     * (ie if every note is contained in this instrument's range).
     *
     * @param midiTimeline Timeline to assess.
     *
     * @return True if instrument can play it, False otherwise.
     */
    public boolean canPlay(MidiTimeline midiTimeline) {

        for (Note note: midiTimeline.getTimeline()) {

            if (note.compareTo(lowestNote) < 0 || note.compareTo(highestNote) > 0) {
                return false;
            }
        }

        return true;
    }
}
