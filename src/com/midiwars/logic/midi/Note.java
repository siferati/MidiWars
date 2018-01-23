package com.midiwars.logic.midi;

/**
 * Deprecated.
 * 'C++ Struct' representing a musical note (note name and octave number).
 */
public class Note implements Comparable<Note>{

    /* --- DEFINES --- */

    /** Possible note names. */
    public enum Name {C, Cs, D, Ds, E, F, Fs, G, Gs, A, As, B}

    /** Number of notes that exist in a single octave. */
    public static final int NOTES_PER_OCTAVE = Name.values().length;


    /* --- ATTRIBUTES --- */

    /** Name of this note. */
    public final Name name;

    /** Octave number of this note [-1, 9]. */
    public final int octave;

    /** How long the note was played for (ms). */
    public int duration;


    /* --- METHODS --- */

    /**
     * Creates a new Note object.
     *
     * @param name Name of this note.
     * @param octave Octave number of this note.
     */
    public Note(Name name, int octave) {
        this.name = name;
        this.octave = octave;
        duration = 0;
    }


    /**
     * Creates a new Note object.
     *
     * @param key Number of the note's key.
     */
    public Note(int key) {
        octave = (key / NOTES_PER_OCTAVE) - 1;
        name = Name.values()[key % NOTES_PER_OCTAVE];
        duration = 0;
    }


    @Override
    public String toString() {
        return "" + name + octave;
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
}
