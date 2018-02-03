package com.midiwars.logic;

import com.midiwars.logic.instruments.Instrument.Warning;
import com.midiwars.logic.instruments.MagBell;
import com.midiwars.logic.midi.MidiTimeline;

import java.util.ArrayList;

/**
 * Represents the application itself
 */
public class MidiWars {

    /* --- DEFINES --- */

    //public static String FILEPATH = "C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\midi Files\\test.mid";
    //public static String FILEPATH = "C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\midi Files\\Shigatsu wa Kimi no Uso.mid";
    //public static String FILEPATH = "C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\midi Files\\HesaPirate.mid";
    //public static String FILEPATH = "C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\midi Files\\New Empire - A Little Braver (mid).mid";
    //public static String FILEPATH = "C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\midi Files\\Memory - Undertale (c major).mid";
    public static String FILEPATH = "C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\midi Files\\Light your heart up - Kill la Kill.mid";


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

            ArrayList<Warning> warnings = magBell.canPlay(midiTimeline);
            for (Warning warning: warnings) {
                switch (warning) {
                    case NOT_IN_RANGE:
                        System.out.println("This midi file contains notes that this instrument can not play, therefore they will be skipped during playback.");
                        break;
                    case TEMPO_TOO_FAST:
                        System.out.println("This midi file's tempo is too fast - playback will probably be hindered. Lower the tempo for smoother playback.");
                        break;
                    case NOTES_TOO_LONG:
                        System.out.println("This midi file contains notes that are too long - they will probably be played twice. Lower their duration for smoother playback.");
                        break;
                    case PAUSES_TOO_LONG:
                        System.out.println("This midi file contains pauses that are too long - probably due to an error in the midi file.");
                        break;
                    default:
                        System.out.println("Unknown warning caused by this midi file.");
                        break;
                }
            }

            //Thread.sleep(5000);
            //magBell.play(midiTimeline);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
