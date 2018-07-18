package com.midiwars.logic.instruments;


/**
 * The in-game Magnanimous Choir Bell.
 * Tuned in C Major [C5, C7].
 */
public class MagBell extends Instrument {

    /* --- DEFINES --- */

    /** {@link Instrument#name Name}. */
    public final static String NAME = "magbell";

    /** {@link Instrument#canHold Can't hold notes}. */
    public final static boolean CAN_HOLD = false;

    /** {@link Instrument#keybars Key bars}. */
    public final static int[][] KEYBARS = {
            {72, 74, 76, 77, 79, 81, 83, 84},
            {84, 86, 88, 89, 91, 93, 95, 96}
    };

    /** {@link Instrument#idleKeybarIndex Idle key bar index}. */
    public final static int IDLE_KEYBAR_INDEX = 0;


    /* --- METHODS --- */

    /**
     * Creates a new MagBell object.
     */
    public MagBell() {
        super(NAME, CAN_HOLD, KEYBARS, IDLE_KEYBAR_INDEX);
    }
}
