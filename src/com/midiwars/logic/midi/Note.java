package com.midiwars.logic.midi;

/**
 * Represents a musical note.
 */
public class Note {

    /* --- DEFINES --- */

    /** Possible note names. */
    public enum Name {C, Cs, D, Ds, E, F, Fs, G, Gs, A, As, B}

    /** Number of notes that exist in a single octave. */
    public static int NOTES_PER_OCTAVE = Name.values().length;


    /* --- ATTRIBUTES --- */

    /** Key number [0-127]. */
    private final int key;

    /** Name of this note. */
    private final Name name;

    /** Octave number of this note [-1, 9]. */
    private final int octave;

    /** Moment in time this note was played (ms). */
    private final double timestamp;

    /** How long the note was played for (ms). */
    private double duration;


    /* --- METHODS --- */

    /**
     * Constructor.
     *
     * @param key Key number [0-127].
     * @param tick NOTE_ON time-stamp (ticks).
     * @param resolution Number of ticks per quarter note (PPQ), or per SMPTE frame (SMPTE).
     * @param tempo Current tempo (BPM).
     */
    public Note(int key, long tick, int resolution, int tempo) {

        this.key = key;
        octave = (key / NOTES_PER_OCTAVE) - 1;
        name = Name.values()[key % NOTES_PER_OCTAVE];

        // this is set when note is released
        duration = 0;

        timestamp = ticksToSeconds(tick, resolution, tempo);
    }


    /**
     * Converts ticks to seconds,
     * according to given tempo and resolution.
     *
     * @param tick Ticks.
     * @param resolution Number of ticks per quarter note (PPQ), or per SMPTE frame (SMPTE).
     * @param tempo Beats per minute.
     *
     * @return Seconds.
     */
    private double ticksToSeconds(long tick, int resolution, int tempo) {

        // TODO SMPTE
        double ticksPerSecond = resolution * (tempo / 60.0);
        return tick / ticksPerSecond;
    }


    /**
     * Sets the duration of this note based on the NOTE_OFF time-stamp.
     *
     * @param tick NOTE_OFF time-stamp (ticks).
     * @param resolution Number of ticks per quarter note (PPQ), or per SMPTE frame (SMPTE).
     * @param tempo Current tempo (BPM).
     */
    public void setDuration(long tick, int resolution, int tempo) {
        this.duration = ticksToSeconds(tick, resolution, tempo) - timestamp;
    }


    @Override
    public String toString() {
        return "NOTE_ON: " + Math.round(timestamp * 10) / 10.0 + ", NOTE: " + name + octave + ", NOTE_OFF: " +
                Math.round((timestamp + duration) * 10) / 10.0;
    }


    /**
     * Returns {@link #key} number.
     *
     * @return {@link #key Key} number.
     */
    public int getKey() {
        return key;
    }


    /**
     * Returns {@link #duration}.
     *
     * @return {@link #duration Duration}.
     */
    public double getDuration() {
        return duration;
    }


    /**
     * Returns {@link #timestamp}.
     *
     * @return {@link #timestamp Timestamp}.
     */
    public double getTimestamp() {
        return timestamp;
    }


    /**
     * Returns {@link #name}.
     *
     * @return {@link #name Name}.
     */
    public Name getName() {
        return name;
    }


    /**
     * Returns {@link #octave}.
     *
     * @return {@link #octave Octave}.
     */
    public int getOctave() {
        return octave;
    }
}
