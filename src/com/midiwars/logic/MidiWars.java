package com.midiwars.logic;

import com.midiwars.logic.instruments.MagBell;
import com.midiwars.logic.midi.MidiTimeline;
import com.midiwars.logic.midi.NoteEvent;

import javax.sound.midi.*;
import java.awt.*;
import java.io.IOException;

import static java.awt.event.KeyEvent.*;

/**
 * Represents the application itself
 */
public class MidiWars {

    /* --- DEFINES --- */

    //public static String FILEPATH = "C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\midi Files\\HesaPirate.mid";
    public static String FILEPATH = "C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\midi Files\\New Empire - A Little Braver.mid";
    //public static String FILEPATH = "C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\midi Files\\Memory - Undertale (c major).mid";
    //public static String FILEPATH = "C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\midi Files\\Light your heart up - Kill la Kill (c major).mid";


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
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
