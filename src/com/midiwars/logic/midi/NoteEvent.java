package com.midiwars.logic.midi;

import static javax.sound.midi.ShortMessage.NOTE_ON;

/**
 * Represents a midi event about a keypress (NOTE_ON) or keyrelease (NOTE_OFF).
 */
public class NoteEvent implements Comparable<NoteEvent> {

    /* --- DEFINES --- */

    /* --- ATTRIBUTES --- */

    /** NOTE_ON (0x90) or NOTE_OFF (0x80). */
    private final int type;

    /** Number of the key that originated the event [0-127]. */
    private final int key;

    /** Moment in time this event was generated (ms). */
    private final int timestamp;

    /** How long the note was played for (ms). */
    private int duration;


    /* --- METHODS --- */

    /**
     * Constructor.
     *
     * @param type NOTE_ON (0x90) or NOTE_OFF (0x80).
     * @param key Key number [0-127].
     * @param timestamp Moment in time this event was generated (ms).
     */
    public NoteEvent(int type, int key, int timestamp) {

        this.type = type;
        this.key = key;
        this.timestamp = timestamp;
        duration = 0;
    }


    @Override
    public int compareTo(NoteEvent e) {
        if (timestamp == e.timestamp) {
            return Integer.compare(key, e.key);
        } else {
            return Integer.compare(timestamp, e.timestamp);
        }
    }


    @Override
    public String toString() {
        if (type == NOTE_ON) {
            return "NOTE_ON: " + timestamp + ", KEY: " + key + ", DURATION: " + duration;
        } else {
            return "NOTE_OFF: " + timestamp + ", KEY: " + key + ", DURATION: " + duration;
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
     * Returns {@link #timestamp}.
     *
     * @return {@link #timestamp Timestamp}.
     */
    public int getTimestamp() {
        return timestamp;
    }


    /**
     * Returns {@link #type}.
     *
     * @return {@link #type Type}.
     */
    public int getType() {
        return type;
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
     * Sets {@link #duration}.
     *
     * @param duration New duration.
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

}
