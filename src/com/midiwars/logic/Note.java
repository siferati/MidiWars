package com.midiwars.logic;

/**
 * 'Struct' representing a musical note
 */
public class Note {

    /* --- DEFINES --- */


    /* --- ATTRIBUTES --- */

    /** Key number (1-128) */
    public final int key;

    /** Moment in time this note was played (ms) */
    public final int time;

    /** How long the note was played (ms) */
    public final int duration;


    /* --- METHODS --- */

    public Note(int key, int ticks, int resolution, int bpm) {

        /* TODO SMPTE
        resolution = sequence.getResolution();

        ticksPerSecond = resolution * (currentTempoInBeatsPerMinute / 60.0);
        tickSize = 1.0 / ticksPerSecond;
        */

        this.key = key;
        time = 0;
        duration = 0;
    }
}
