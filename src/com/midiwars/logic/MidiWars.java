package com.midiwars.logic;

import javax.sound.midi.*;
import java.io.IOException;

/**
 * Represents the application itself
 */
public class MidiWars {

    /* --- DEFINES --- */

    // TODO filepaths
    public static String FILEPATH = "C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\Midi Files\\Memory - Undertale.mid";
    //public static String FILEPATH = "C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\Midi Files\\Light your heart up - Kill la Kill.mid";


    /* --- ATTRIBUTES --- */


    /* --- METHODS --- */

    /**
     * Default Constructor.
     */
    public MidiWars() {

        try {
            Score score = new Score(FILEPATH);
        }
        catch (InvalidMidiDataException | IOException e) {
            e.printStackTrace();
        }
    }
}
