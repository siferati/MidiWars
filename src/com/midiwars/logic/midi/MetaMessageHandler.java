package com.midiwars.logic.midi;

import javax.sound.midi.MetaMessage;

/**
 * Collection of handlers called when dealing with MidiMessages of type MetaMessage.
 */
public class MetaMessageHandler {

    /* --- DEFINES --- */

    /** This event indicates the end of a track. */
    public static final int END_OF_TRACk = 0x2F;

    /** This event indicates a tempo change. */
    public static final int SET_TEMPO = 0x51;


    /* --- METHODS --- */

    /**
     * Called when a MetaMessage is received.
     *
     * @param midiTimeline midi timeline.
     * @param metaMessage Message received.
     * @param tick MidiEvent time-stamp (ticks).
     */
    public static void metaMessageHandler(MidiTimeline midiTimeline, MetaMessage metaMessage, long tick) {

        switch (metaMessage.getType()) {

            case END_OF_TRACk: {

                System.out.println("debug: END_OF_TRACK");
                break;
            }

            case SET_TEMPO: {

                setTempo(midiTimeline, metaMessage, tick);
                break;
            }

            default: {

                System.out.println("debug: Unknown Meta Message type: 0x" + Integer.toHexString(metaMessage.getType()).toUpperCase());
                break;
            }
        }
    }


    /** TODO this assumes time signature is x/4
     * Called when MetaMessage is of type SET_TEMPO.
     *
     * @param midiTimeline midi timeline.
     * @param metaMessage Message received.
     * @param tick MidiEvent time-stamp (ticks).
     *
     * @see #metaMessageHandler(MidiTimeline, MetaMessage, long)
     */
    private static void setTempo(MidiTimeline midiTimeline, MetaMessage metaMessage, long tick) {

        // read message data
        byte[] data = metaMessage.getData();

        // microseconds per quarter-note
        int mspq = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[2] & 0xff);

        // 1 min = 60 s = 60 000 000 ms
        midiTimeline.addTempo(tick, (60.0 * 1000000) / mspq);

        System.out.println("debug: SET_TEMPO: " + ((60.0 * 1000000) / mspq) + " BPM.");
    }
}
