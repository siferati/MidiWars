package com.midiwars.logic;

import com.midiwars.logic.instruments.MagBell;
import com.midiwars.logic.midi.MidiTimeline;

import javax.sound.midi.*;
import java.awt.*;
import java.io.IOException;

/**
 * Represents the application itself
 */
public class MidiWars {

    /* --- DEFINES --- */

    public static String FILEPATH = "C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\midi Files\\test.mid";
    //public static String FILEPATH = "C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\midi Files\\Memory - Undertale (c major).mid";
    //public static String FILEPATH = "C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\midi Files\\Light your heart up - Kill la Kill.mid";


    /* --- ATTRIBUTES --- */


    /* --- METHODS --- */

    /**
     * Default Constructor.
     */
    public MidiWars() {

        try {

            // construct timeline from midi file
            MidiTimeline midiTimeline = new MidiTimeline(FILEPATH);

            MagBell magBell = new MagBell();

            if (!magBell.canPlay(midiTimeline)) {
                System.out.println("This midi file contains notes that this instrument can not play, therefore they will be skipped during playback");
            }

            Thread.sleep(5000);

            magBell.play(midiTimeline);
        }
        catch (InvalidMidiDataException | IOException | AWTException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
