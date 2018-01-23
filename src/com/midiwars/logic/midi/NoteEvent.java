package com.midiwars.logic.midi;

import javax.sound.midi.ShortMessage;

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

    /** Note played. */
    private final Note note;

    /** Moment in time this event was generated (ms). */
    private final int timestamp;


    /* --- METHODS --- */

    /**
     * Constructor.
     *
     * @param type NOTE_ON (0x90) or NOTE_OFF (0x80).
     * @param key Key number [0-127].
     * @param tick NOTE_ON time-stamp (ticks).
     * @param resolution Number of ticks per quarter note (PPQ), or per SMPTE frame (SMPTE).
     * @param tempo Current tempo (BPM).
     */
    public NoteEvent(int type, int key, long tick, int resolution, int tempo) {

        this.type = type;
        this.key = key;
        note = new Note(key);
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
     * Sets the duration of this event's note based on the NOTE_OFF time-stamp.
     *
     * @param tick NOTE_OFF time-stamp (ticks).
     * @param resolution Number of ticks per quarter note (PPQ), or per SMPTE frame (SMPTE).
     * @param tempo Current tempo (BPM).
     */
    public void setNoteDuration(long tick, int resolution, int tempo) {
        note.duration = ticksToMilliseconds(tick, resolution, tempo) - timestamp;
    }


    @Override
    public int compareTo(NoteEvent e) {
        if (timestamp == e.timestamp) {
            return note.compareTo(e.note);
        } else {
            return Integer.compare(timestamp, e.timestamp);
        }
    }


    @Override
    public String toString() {
        if (type == ShortMessage.NOTE_ON) {
            return "NOTE_ON: " + timestamp + ", NOTE: " + note;
        } else {
            return "NOTE_OFF: " + timestamp + ", NOTE: " + note;
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
     * Returns {@link #note}.
     *
     * @return {@link #note Note}.
     */
    public Note getNote() {
        return note;
    }


    /**
     * Returns {@link #type}.
     *
     * @return {@link #type Type}.
     */
    public int getType() {
        return type;
    }
}
