package com.midiwars.logic.midi;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.midiwars.logic.midi.MetaMessageHandler.metaMessageHandler;
import static com.midiwars.logic.midi.ShortMessageHandler.shortMessageHandler;
import static javax.sound.midi.Sequence.*;
import static javax.sound.midi.ShortMessage.NOTE_OFF;
import static javax.sound.midi.ShortMessage.NOTE_ON;

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

    /** Maps tempo (QPM) changes (SET_TEMPO midi message) to the instant (tick) it happens.
     * Note that tempo is mapped as QPM (quarter-note per minute) and not the usual BPM (beat per minute).
     * This makes it so that all time signatures are handled the same way. */
    private TreeMap<Long, Double> tempo;


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
        tempo = new TreeMap<>();

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
     * Adds an entry to the tempo map.
     *
     * @param tempo {@link #tempo Tempo}.
     */
    public void addTempo(long tick, double tempo) {
        this.tempo.put(tick, tempo);
    }


    /**
     * Adds note event to timeline.
     *
     * @param type NOTE_ON (0x90) or NOTE_OFF (0x80).
     * @param key Key number [0-127].
     * @param tick MidiEvent time-stamp (ticks).
     */
    public void addNoteEvent(int type, int key, long tick) {

        int timestamp = ticksToMilliseconds(tick);

        // add note event
        timeline.add(new NoteEvent(
                type,
                key,
                timestamp
        ));

        // key released
        if (type == NOTE_OFF) {

            // search respective NOTE_ON event
            for (int i = timeline.size() - 1; i >= 0; i--) {

                NoteEvent noteEvent = timeline.get(i);

                // set duration of respective NOTE_ON event
                if (noteEvent.getKey() == key && noteEvent.getType() == NOTE_ON && noteEvent.getDuration() == 0) {
                    noteEvent.setDuration(timestamp - noteEvent.getTimestamp());
                }
            }
        }
    }


    /**
     * Returns an ordered list of tick-tempos between the given time frame.
     *
     * @return  List of tempos.
     */
    public ArrayList<Map.Entry<Long, Double>> getTempos(long ticki, long tickf) {

        return new ArrayList<>(
                tempo.subMap(
                        tempo.floorKey(ticki),
                        true,
                        tickf,
                        true
                ).entrySet()
        );
    }


    /**
     * Converts ticks to milliseconds,
     * taking into account division type
     * and tempo changes (if applicable).
     *
     * @param tick Ticks.
     *
     * @return Milliseconds.
     */
    private int ticksToMilliseconds(long tick) {

        int ms = 0;

        float division = sequence.getDivisionType();
        int resolution = sequence.getResolution();

        if (division == PPQ) {

            ArrayList<Map.Entry<Long, Double>> tempos = getTempos(0, tick);

            for (int i = 0; i < tempos.size(); i++) {

                Map.Entry<Long, Double> entry = tempos.get(i);

                // how many ticks passed
                long deltaTick;

                if (tempos.size() == 1) {
                    deltaTick = tick;
                } else if (i == tempos.size() - 1) {
                    deltaTick = tick - entry.getKey();
                } else {
                    Map.Entry<Long, Double> nextEntry = tempos.get(i + 1);
                    deltaTick = nextEntry.getKey() - entry.getKey();
                }

                double ticksPerSecond = resolution * (entry.getValue() / 60.0);

                // duration of each tick (ms)
                double tickSize = (1.0 / ticksPerSecond) * 1000;

                ms += (int) (deltaTick * tickSize);
            }
        }
        else {

            float framesPerSecond;

            if (division == SMPTE_24) {
                framesPerSecond = 24;
            } else if (division == SMPTE_25) {
                framesPerSecond = 25;
            } else if (division == SMPTE_30) {
                framesPerSecond = 30;
            } else {
                framesPerSecond = (float) 29.97;
            }

            double ticksPerSecond = resolution * framesPerSecond;

            // duration of each tick (ms)
            double tickSize = (1.0 / ticksPerSecond) * 1000;

            ms += (int) (tick * tickSize);
        }

        return ms;
    }


    /**
     * Returns {@link #timeline}.
     *
     * @return {@link #timeline Timeline}.
     */
    public ArrayList<NoteEvent> getTimeline() {
        return timeline;
    }
}
