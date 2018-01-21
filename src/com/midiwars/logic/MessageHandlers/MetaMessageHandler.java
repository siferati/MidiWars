package com.midiwars.logic.MessageHandlers;

import com.midiwars.logic.Score;
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
     * @param score Midi timeline.
     * @param metaMessage Message received.
     */
    public static void metaMessageHandler(Score score, MetaMessage metaMessage) {

        switch (metaMessage.getType()) {

            case END_OF_TRACk: {

                System.out.println("debug: END OF TRACK");
                break;
            }

            case SET_TEMPO: {

                setTempo(score, metaMessage);
                break;
            }

            default:
                break;
        }
    }


    /** TODO this assumes time signature is x/4
     * Called when MetaMessage is of type SET_TEMPO.
     *
     * @param score Midi timeline.
     * @param metaMessage Message received.
     *
     * @see #metaMessageHandler(Score, MetaMessage)
     */
    private static void setTempo(Score score, MetaMessage metaMessage) {

        // read message data
        byte[] data = metaMessage.getData();

        // microseconds per quarter-note
        int mspq = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[2] & 0xff);

        // 1 min = 60 s = 60 000 000 ms
        score.setTempo((60 * 1000000) / mspq);
    }
}
