package com.midiwars.logic.instruments;

/**
 * The in-game Flute.
 * Tuned in E Major [E4, E6].
 */
public class Flute extends Instrument {

    /* --- DEFINES --- */

    /** {@link Instrument#name Name}. */
    public final static String NAME = "flute";

    /** {@link Instrument#canHold Can hold notes}. */
    public final static boolean CAN_HOLD = true;

    /** {@link Instrument#keybars Key bars}. */
    public final static int[][] KEYBARS = {
            {64, 66, 68, 69, 71, 73, 75, 76},
            {76, 78, 80, 81, 83, 85, 87, 88}
    };

    /** {@link Instrument#idleKeybarIndex Idle key bar index}. */
    public final static int IDLE_KEYBAR_INDEX = 0;


    /* --- METHODS --- */

    /**
     * Creates a new Flute object.
     */
    public Flute() {
        super(NAME, CAN_HOLD, KEYBARS, IDLE_KEYBAR_INDEX);
    }
}
