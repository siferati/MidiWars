package com.midiwars.logic.instruments;

/**
 * An instrument factory.
 */
public class InstrumentFactory {

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
