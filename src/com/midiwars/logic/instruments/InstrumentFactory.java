package com.midiwars.logic.instruments;

/**
 * Represents an instrument factory.
 */
public class InstrumentFactory {

    /**
     * Thrown when the default instrument in the configurations file is invalid.
     */
    public static class InvalidInstrumentException extends Exception {

    }

    /**
     * Returns a new instrument of given name.
     *
     * @param name Name of the instrument.
     *
     * @return New instrument.
     */
    public static Instrument newInstrument(String name){

        switch (name) {

            case MagBell.NAME: return new MagBell();

            case Flute.NAME: return new Flute();

            case Harp.NAME: return new Harp();

            default: return null;
        }
    }
}
