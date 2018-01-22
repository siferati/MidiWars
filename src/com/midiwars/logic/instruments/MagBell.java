package com.midiwars.logic.instruments;

import com.midiwars.logic.midi.Note;

import static com.midiwars.logic.midi.Note.Name.C;

public class MagBell extends Instrument {

    /* --- DEFINES --- */

    /** {@link Instrument#name Name}. */
    public final static String NAME = "Magnanimous Choir Bell";

    /** {@link Instrument#lowestNote Lowest Note}. */
    public final static Note LOWEST_NOTE = new Note(C, 5);

    /** {@link Instrument#highestNote Highest Note}. */
    public final static Note HIGHEST_NOTE = new Note(C, 7);

    /** {@link Instrument#idleNote Idle Note}. */
    public final static Note IDLE_NOTE = new Note(C, 5);

    /** {@link Instrument#repeatedNote Repeated Note}. */
    public final static Note.Name REPEATED_NOTE = C;

    /** {@link Instrument#canHold Can Hold}. */
    public final static boolean CAN_HOLD = false;


    /* --- METHODS --- */

    /**
     * Constructor.
     */
    public MagBell() {
        super(NAME, LOWEST_NOTE, HIGHEST_NOTE, IDLE_NOTE, REPEATED_NOTE, CAN_HOLD);
    }
}
