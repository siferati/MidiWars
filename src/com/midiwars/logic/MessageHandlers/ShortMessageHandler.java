package com.midiwars.logic.MessageHandlers;

import com.midiwars.logic.Note;
import com.midiwars.logic.Score;

import javax.sound.midi.ShortMessage;

/**
 * Collection of handlers called when dealing with MidiMessages of type ShortMessage.
 */
public abstract class ShortMessageHandler {

    /**
     * Called when a ShortMessage is received.
     *
     * @param score Midi timeline.
     * @param shortMessage Message received.
     * @param tick MidiEvent time-stamp (ticks).
     */
    public static void shortMessageHandler(Score score, ShortMessage shortMessage, long tick) {

        switch (shortMessage.getCommand()) {

            case ShortMessage.NOTE_ON: {

                noteOn(score, shortMessage, tick);
                break;
            }

            case ShortMessage.NOTE_OFF: {

                noteOff(score, shortMessage, tick);
                break;
            }

            default: {

                System.out.println("debug: Command:" + shortMessage.getCommand());
                break;
            }
        }
    }


    /**
     * Called when ShortMessage is of type NOTE_ON.
     *
     * @param score Midi timeline.
     * @param shortMessage Message received.
     * @param tick MidiEvent time-stamp (ticks).
     *
     * @see #shortMessageHandler(Score, ShortMessage, long)
     */
    private static void noteOn(Score score, ShortMessage shortMessage, long tick) {

        // get velocity
        int velocity = shortMessage.getData2();

        // message should have been NOTE_OFF
        if (velocity == 0) {
            noteOff(score, shortMessage, tick);
            return;
        }

        // get key number [0-127]
        int key = shortMessage.getData1();

        // add note to timeline
        score.getTimeline().add(new Note(key, tick, score.getSequence().getResolution(), score.getTempo()));
    }


    /**
     * Called when ShortMessage is of type NOTE_OFF.
     *
     * @param score Midi timeline.
     * @param shortMessage Message received.
     * @param tick MidiEvent time-stamp (ticks).
     *
     * @see #shortMessageHandler(Score, ShortMessage, long)
     */
    private static void noteOff(Score score, ShortMessage shortMessage, long tick) {

        // get key number [0-127]
        int key = shortMessage.getData1();

        // set duration of released note
        for (Note note : score.getTimeline()) {
            if (note.getKey() == key && note.getDuration() == 0) {
                note.setDuration(tick, score.getSequence().getResolution(), score.getTempo());
                return;
            }
        }
    }
}
