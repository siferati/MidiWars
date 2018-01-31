package com.midiwars.logic.midi;

import javax.sound.midi.ShortMessage;

import static javax.sound.midi.ShortMessage.NOTE_OFF;
import static javax.sound.midi.ShortMessage.NOTE_ON;

/**
 * Collection of handlers called when dealing with MidiMessages of type ShortMessage.
 */
public abstract class ShortMessageHandler {

    /**
     * Called when a ShortMessage is received.
     *
     * @param midiTimeline midi timeline.
     * @param shortMessage Message received.
     * @param tick MidiEvent time-stamp (ticks).
     */
    public static void shortMessageHandler(MidiTimeline midiTimeline, ShortMessage shortMessage, long tick) {

        switch (shortMessage.getCommand()) {

            case NOTE_ON: {

                noteOn(midiTimeline, shortMessage, tick);
                break;
            }

            case ShortMessage.NOTE_OFF: {

                noteOff(midiTimeline, shortMessage, tick);
                break;
            }

            default: {

                System.out.println("debug: Unknown Short Message command: 0x" + Integer.toHexString(shortMessage.getCommand()).toUpperCase());
                break;
            }
        }
    }


    /**
     * Called when ShortMessage is of type NOTE_ON.
     *
     * @param midiTimeline midi timeline.
     * @param shortMessage Message received.
     * @param tick MidiEvent time-stamp (ticks).
     *
     * @see #shortMessageHandler(MidiTimeline, ShortMessage, long)
     */
    private static void noteOn(MidiTimeline midiTimeline, ShortMessage shortMessage, long tick) {

        // get velocity
        int velocity = shortMessage.getData2();

        // message should have been NOTE_OFF
        if (velocity == 0) {
            noteOff(midiTimeline, shortMessage, tick);
            return;
        }

        // get key number [0-127]
        int key = shortMessage.getData1();

        // add new note event to timeline
        midiTimeline.addNoteEvent(NOTE_ON, key, tick);
    }


    /**
     * Called when ShortMessage is of type NOTE_OFF.
     *
     * @param midiTimeline midi timeline.
     * @param shortMessage Message received.
     * @param tick MidiEvent time-stamp (ticks).
     *
     * @see #shortMessageHandler(MidiTimeline, ShortMessage, long)
     */
    private static void noteOff(MidiTimeline midiTimeline, ShortMessage shortMessage, long tick) {

        // get key number [0-127]
        int key = shortMessage.getData1();

        // add new note event to timeline
        midiTimeline.addNoteEvent(NOTE_OFF, key, tick);
    }
}
