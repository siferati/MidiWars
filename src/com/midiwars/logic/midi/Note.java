package com.midiwars.logic.midi;

/**
 * Represents a musical note.
 */
public class Note implements Comparable<Note> {

    /* --- DEFINES --- */

    /** Possible note names. */
    public enum Name {C, Cs, D, Ds, E, F, Fs, G, Gs, A, As, B}

    /** Number of notes that exist in a single octave. */
    public static int NOTES_PER_OCTAVE = Name.values().length;


    /* --- ATTRIBUTES --- */

    /** True if NOTE_ON, False if NOTE_OFF. */
    private final boolean on;

    /** Key number [0-127]. */
    private final int key;

    /** Name of this note. */
    private final Name name;

    /** Octave number of this note [-1, 9]. */
    private final int octave;

    /** Moment in time this note was played (ms). */
    private final int timestamp;

    /** How long the note was played for (ms). */
    private int duration;


    /* --- METHODS --- */

    /**
     * Constructor.
     *
     * @param name Name of the note.
     * @param octave Octave of the note.
     */
    public Note(Name name, int octave) {

        on = true;
        key = (octave + 1) * NOTES_PER_OCTAVE + name.ordinal();
        this.name = name;
        this.octave = octave;
        duration = 0;
        timestamp = 0;
    }


    /**
     * Constructor.
     *
     * @param on True if NOTE_ON, False if NOTE_OFF.
     * @param key Key number [0-127].
     * @param tick NOTE_ON time-stamp (ticks).
     * @param resolution Number of ticks per quarter note (PPQ), or per SMPTE frame (SMPTE).
     * @param tempo Current tempo (BPM).
     */
    public Note(boolean on, int key, long tick, int resolution, int tempo) {

        this.on = on;
        this.key = key;
        octave = (key / NOTES_PER_OCTAVE) - 1;
        name = Name.values()[key % NOTES_PER_OCTAVE];

        // this is set when note is released
        duration = 0;

        timestamp = ticksToMilliseconds(tick, resolution, tempo);
    }


    /**
     * Converts ticks to milliseconds,
     * according to given tempo and resolution.
     *
     * @param tick Ticks.
     * @param resolution Number of ticks per quarter note (PPQ), or per SMPTE frame (SMPTE).
     * @param tempo Beats per minute.
     *
     * @return Seconds.
     */
    private int ticksToMilliseconds(long tick, int resolution, int tempo) {

        // TODO SMPTE
        double ticksPerSecond = resolution * (tempo / 60.0);
        return (int) ((tick / ticksPerSecond) * 1000);
    }


    /**
     * Sets the duration of this note based on the NOTE_OFF time-stamp.
     *
     * @param tick NOTE_OFF time-stamp (ticks).
     * @param resolution Number of ticks per quarter note (PPQ), or per SMPTE frame (SMPTE).
     * @param tempo Current tempo (BPM).
     */
    public void setDuration(long tick, int resolution, int tempo) {
        this.duration = ticksToMilliseconds(tick, resolution, tempo) - timestamp;
    }


    @Override
    public int compareTo(Note n) {

        if (octave == n.octave) {
            return Integer.compare(name.ordinal(), n.name.ordinal());
        } else {
            return Integer.compare(octave, n.octave);
        }
    }


    @Override
    public boolean equals(Object obj) {
        return name == name && octave == octave;
    }


    @Override
    public String toString() {
        if (on) {
            //return "NOTE_ON: " + Math.round(timestamp * 10) / 10.0 + ", NOTE: " + name + octave;
            return "NOTE_ON: " + timestamp + ", NOTE: " + name + octave;
        } else {
            //return "NOTE_OFF: " + Math.round(timestamp * 10) / 10.0 + ", NOTE: " + name + octave;
            return "NOTE_OFF: " + timestamp + ", NOTE: " + name + octave;
        }

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
    public int getDuration() {
        return duration;
    }


    /**
     * Returns {@link #timestamp}.
     *
     * @return {@link #timestamp Timestamp}.
     */
    public int getTimestamp() {
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


    /**
     * Returns true if {@link #on}, false otherwise.
     *
     * @return {@link #on On}.
     */
    public boolean isOn() {
        return on;
    }
}
