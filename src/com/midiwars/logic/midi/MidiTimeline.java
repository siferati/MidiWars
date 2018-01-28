package com.midiwars.logic.midi;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.midiwars.logic.midi.MetaMessageHandler.metaMessageHandler;
import static com.midiwars.logic.midi.ShortMessageHandler.shortMessageHandler;

/**
 * Represents the timeline of a given midi file.
 * Holds information about all the notes played, their start time and duration.
 */
public class MidiTimeline {

    /* --- DEFINES --- */


    /* --- ATTRIBUTES --- */

    /** Sequence present in midi file. */
    private Sequence sequence;

    /** Notes played. */
    private ArrayList<NoteEvent> timeline;

    /** Maps tempo (bpm) changes (SET_TEMPO midi message) to the instant (tick) it happens. */
    private Map<Long, Double> tempo;




    /* --- METHODS --- */

    /**
     * Constructor.
     *
     * @param filepath Path to midi file.
     *
     * @throws InvalidMidiDataException midi file is invalid.
     * @throws IOException Can't open file.
     */
    public MidiTimeline(String filepath) throws InvalidMidiDataException, IOException {

        sequence = null;
        timeline = new ArrayList<>();
        tempo = new HashMap<>();

        // read the midi file and construct its timeline
        constructTimeline(filepath);
    }


    /**
     * Sorts the timeline in ascendant order of timestamps.
     */
    private void sort() {

        Collections.sort(timeline, new Comparator<>() {
            @Override
            public int compare(NoteEvent n1, NoteEvent n2) {

                if (n1.getTimestamp() == n2.getTimestamp()) return n1.compareTo(n2);
                else return Double.compare(n1.getTimestamp(), n2.getTimestamp());
            }
        });
    }


    /**
     * Reads the contents of the given midi file
     * and constructs its timeline.
     *
     * @param filepath Path to midi file.
     *
     * @throws InvalidMidiDataException midi file is invalid.
     * @throws IOException Can't open file.
     */
    public void constructTimeline(String filepath) throws InvalidMidiDataException, IOException {

        // read Sequence from midi file
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
                   metaMessageHandler(this, (MetaMessage) message, tick);
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
    public ArrayList<NoteEvent> getTimeline() {
        return timeline;
    }


    /**
     * Returns tempo at current tick.
     *
     * @return {@link #tempo Tempo}.
     */
    public double getTempo(long tick) {

        // key of return value
        long latestTempo = 0;

        for (Map.Entry<Long, Double> entry : tempo.entrySet())
        {
            // find current tempo
            if (entry.getKey() <= tick && entry.getKey() >= latestTempo) {
                latestTempo = entry.getKey();
            }
        }

        return tempo.get(latestTempo);
    }


    /**
     * Adds an entry to the tempo map.
     *
     * @param tempo {@link #tempo Tempo}.
     */
    public void addTempo(long tick, double tempo) {

        this.tempo.put(tick, tempo);
    }
}
