package com.midiwars.util;

/**
 * Represents a synchronized boolean,
 * allowing for atomic get(), set() and swap().
 */
public class SyncBoolean {

    /* --- ATTRIBUTES --- */

    /** Primitive boolean value. */
    private boolean value;


    /* --- METHODS --- */

    /**
     * Creates a new SyncBoolean object.
     *
     * @param value Initial value.
     */
    public SyncBoolean(boolean value) {
        this.value = value;
    }


    /**
     * Returns the current value.
     *
     * @return Current value.
     */
    public synchronized boolean get() {
        return value;
    }


    /**
     * Sets the current value to a new one.
     *
     * @param newValue New value.
     */
    public synchronized void set(boolean newValue) {
        value = newValue;
    }


    /**
     * Swaps the current value.
     */
    public synchronized void swap() {
        value = !value;
    }

}
