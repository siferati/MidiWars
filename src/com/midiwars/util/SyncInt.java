package com.midiwars.util;

/**
 * Represents a synchronized int,
 * allowing for atomic increments and decrements.
 */
public class SyncInt {

    /* --- ATTRIBUTES --- */

    /** Primitive int value. */
    private int value;


    /* --- METHODS --- */

    /**
     * Creates a new SyncInt object.
     *
     * @param value Initial value.
     */
    public SyncInt(int value) {
        this.value = value;
    }


    /**
     * Returns the current value.
     *
     * @return Current value.
     */
    public synchronized int get() {
        return value;
    }


    /**
     * Sets the current value to a new one.
     *
     * @param newValue New value.
     */
    public synchronized void set(int newValue) {
        value = newValue;
    }


    /**
     * Increments the current value by 1.
     */
    public synchronized void increment() {
        value++;
    }


    /**
     * Decrements the current value by 1.
     */
    public synchronized void decrement() {
        value--;
    }
}
