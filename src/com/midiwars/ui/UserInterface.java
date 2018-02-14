package com.midiwars.ui;

import com.midiwars.logic.instruments.Instrument;

/**
 * Represents a user interface.
 */
public interface UserInterface {

    /**
     * Displays to the user the usage information of the app.
     */
    void displayUsage();


    /**
     * Plays the given file.
     *
     * @param instrument Instrument to play given file with.
     * @param filename File to play.
     */
    void play(Instrument instrument, String filename);


    /**
     * Checks if the given midi file can be played by the given instrument.
     *
     * @param instrument Instrument to play given file with.
     * @param filename Name of midi file to play.
     */
    void canPlay(Instrument instrument, String filename);


    /**
     * Exits the program.
     */
    void quit();
}
