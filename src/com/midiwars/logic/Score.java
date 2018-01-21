package com.midiwars.logic;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.midiwars.logic.MessageHandlers.MetaMessageHandler.metaMessageHandler;
import static com.midiwars.logic.MessageHandlers.ShortMessageHandler.shortMessageHandler;

/**
 * Represents the timeline of a given midi file.
 * Holds information about all the notes played, their start time and duration.
 */
public class Score {

    /* --- DEFINES --- */


    /* --- ATTRIBUTES --- */

    /** Sequence present in midi file. */
    private Sequence sequence;

    /** Notes played. */
    private ArrayList<Note> timeline;

    /** Current tempo of the piece (bpm) (changes upon receiving SET_TEMPO). */
    private int tempo;


    /* --- METHODS --- */

    /**
     * Constructor.
     *
     * @param filepath Path to midi file.
     *
     * @throws InvalidMidiDataException Midi file is invalid.
     * @throws IOException Can't open file.
     */
    public Score(String filepath) throws InvalidMidiDataException, IOException {

        sequence = null;
        timeline = new ArrayList<>();
        tempo = 0;

        // read the midi file and construct its timeline
        constructTimeline(filepath);

        for (Note note: timeline) {
            System.out.println("debug: " + note);
        }
    }


    /**
     * Sorts the timeline in ascendant order of timestamps.
     */
    private void sort() {

        Collections.sort(timeline, new Comparator<>() {
            @Override
            public int compare(Note n1, Note n2) {

                // timestamp -> octave -> note name
                if (n1.getTimestamp() < n2.getTimestamp()) return -1;
                else if (n1.getTimestamp() > n2.getTimestamp()) return 1;
                else if (n1.getOctave() < n2.getOctave()) return -1;
                else if (n1.getOctave() > n2.getOctave()) return 1;
                else if (n1.getName().ordinal() < n2.getName().ordinal()) return -1;
                else if (n1.getName().ordinal() > n2.getName().ordinal()) return 1;
                else return 0;
            }
        });
    }


    /**
     * Reads the contents of the given midi file
     * and constructs its timeline.
     *
     * @param filepath Path to midi file.
     *
     * @throws InvalidMidiDataException Midi file is invalid.
     * @throws IOException Can't open file.
     */
    public void constructTimeline(String filepath) throws InvalidMidiDataException, IOException {

        // read sequence from midi file
        sequence = MidiSystem.getSequence(new File(filepath));

        // iterate through every Track
        for (Track track : sequence.getTracks()) {

            // iterate through every MidiEvent
            for (int i = 0; i < track.size(); i++) {

                // get current MidiEvent
                MidiEvent event = track.get(i);

                // get event time-stamp (ticks)
                long tick = event.getTick();

                // get current MidiMessage
                MidiMessage message = event.getMessage();

                // case ShortMessage
                if (message instanceof ShortMessage) {
                    shortMessageHandler(this, (ShortMessage) message, tick);
                }

                // case MetaMessage
                else if (message instanceof MetaMessage) {

                   metaMessageHandler(this, (MetaMessage) message);
                }

                // case SysexMessage
                else if (message instanceof SysexMessage) {
                    // TODO ...
                }
            }
        }

        // in case there are multiple tracks in the sequence,
        // timeline wouldn't be sorted without this call
        sort();
    }


    /**
     * Returns {@link #sequence}.
     *
     * @return {@link #sequence Sequence}.
     */
    public Sequence getSequence() {
        return sequence;
    }


    /**
     * Returns {@link #timeline}.
     *
     * @return {@link #timeline Timeline}.
     */
    public ArrayList<Note> getTimeline() {
        return timeline;
    }


    /**
     * Returns current {@link #tempo}.
     *
     * @return {@link #tempo Tempo}.
     */
    public int getTempo() {
        return tempo;
    }


    /**
     * Sets current {@link #tempo}.
     *
     * @param tempo {@link #tempo Tempo}.
     */
    public void setTempo(int tempo) {
        this.tempo = tempo;
    }
}
