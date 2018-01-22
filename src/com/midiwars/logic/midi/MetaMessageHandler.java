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
     */
    public static void metaMessageHandler(MidiTimeline midiTimeline, MetaMessage metaMessage) {

        switch (metaMessage.getType()) {

            case END_OF_TRACk: {

                System.out.println("debug: END OF TRACK");
                break;
            }

            case SET_TEMPO: {

                setTempo(midiTimeline, metaMessage);
                break;
            }

            default:
                break;
        }
    }


    /** TODO this assumes time signature is x/4
     * Called when MetaMessage is of type SET_TEMPO.
     *
     * @param midiTimeline midi timeline.
     * @param metaMessage Message received.
     *
     * @see #metaMessageHandler(MidiTimeline, MetaMessage)
     */
    private static void setTempo(MidiTimeline midiTimeline, MetaMessage metaMessage) {

        // read message data
        byte[] data = metaMessage.getData();

        // microseconds per quarter-note
        int mspq = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[2] & 0xff);

        // 1 min = 60 s = 60 000 000 ms
        midiTimeline.setTempo((60 * 1000000) / mspq);
    }
}
