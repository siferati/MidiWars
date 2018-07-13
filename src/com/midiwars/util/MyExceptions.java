package com.midiwars.util;

/**
 * Holds multiple custom exceptions used throughout this application.
 */
public interface MyExceptions {

    /**
     * Thrown when the default instrument listed
     * in the configurations file is invalid.
     */
    class InvalidInstrumentException extends Exception {

    }

    /**
     * Thrown when the midi files listed
     * in the given playlist don't exist.
     */
    class MidifilesNotFoundException extends Exception {

    }

    /**
     * Thrown when the default midi path listed
     * in the configurations file doesn't exist.
     */
    class MidiPathNotFoundException extends Exception {

    }

    /**
     * Thrown when trying to create a new UserInterface object
     * and another UserInterface already exists.
     */
    class UIAlreadyExists extends RuntimeException {

    }

}
