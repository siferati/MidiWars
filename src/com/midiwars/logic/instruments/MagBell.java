package com.midiwars.logic.instruments;


public class MagBell extends Instrument {

    /* --- DEFINES --- */

    /** {@link Instrument#name Note}. */
    public final static String NAME = "Magnanimous Choir Bell";

    /** {@link Instrument#keybars Key bars}. */
    public final static int[][] KEYBARS = {
            {72, 74, 76, 77, 79, 81, 83, 84},
            {84, 86, 88, 89, 91, 93, 95, 96}
    };

    /** {@link Instrument#idleKeybarIndex Idle Key bar Index}. */
    public final static int IDLE_KEYBAR_INDEX = 0;


    /* --- METHODS --- */

    /**
     * Constructor.
     */
    public MagBell() {
        super(NAME, KEYBARS, IDLE_KEYBAR_INDEX);
    }
}
