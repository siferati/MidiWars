package com.midiwars.ui;

import com.midiwars.logic.instruments.Instrument;
import com.midiwars.logic.instruments.InstrumentFactory;

/**
 * Represents a user interface.
 */
public abstract class UserInterface {

    /* --- DEFINES --- */

    /** Command to exit the program. */
    public final static String CMD_QUIT = "/quit";

    /** Command to play a midi file. */
    public static final String CMD_PLAY = "/play";

    /** Command to check playability of a midi file. */
    public final static String CMD_CANPLAY = "/canplay";

    /**  Option to not use default instrument. */
    public final static String OPT_INST = "-inst";


    /* --- METHODS --- */

    /**
     * Displays to the user the usage information of the app.
     */
    abstract void displayUsage();


    /**
     * Plays the given file.
     *
     * @param instrument Instrument to play given file with.
     * @param filename File to play.
     */
    abstract void play(Instrument instrument, String filename);


    /**
     * Checks if the given midi file can be played by the given instrument.
     *
     * @param instrument Instrument to play given file with.
     * @param filename Name of midi file to play.
     */
    abstract void canPlay(Instrument instrument, String filename);


    /**
     * Exits the program.
     */
    abstract void quit();


    /**
     * Parses the given command line arguments
     * and decides what to do.
     *
     * @param args List of arguments to parse.
     */
    public void parse(String[] args) {

        // what user wants to do
        boolean play = false;
        boolean canPlay = false;
        boolean quit = false;

        String filename = "";
        Instrument instrument = null;

        boolean stop = false;
        for (int i = 0; !stop && i < args.length; i++) {

            String arg = args[i];

            switch (arg) {

                case CMD_QUIT: {

                    if (i == 0 && args.length == 1) {
                        quit = true;
                    }
                    else {
                        stop = true;
                    }
                    break;
                }

                case CMD_PLAY: {

                    if (i != args.length - 1) {
                        filename = args[i + 1];
                        play = true;
                        // skip next arg (since it's the filename)
                        i++;
                    } else {
                        stop = true;
                    }
                    break;
                }

                case CMD_CANPLAY: {

                    if (i != args.length - 1) {
                        filename = args[i + 1];
                        canPlay = true;
                        // skip next arg (since it's the filename)
                        i++;
                    } else {
                        stop = true;
                    }
                    break;
                }

                case OPT_INST: {

                    if (i != args.length - 1) {
                        instrument = InstrumentFactory.newInstrument(args[i + 1]);
                        // skip next arg (since it's the instrument)
                        i++;
                    } else {
                        stop = true;
                    }
                    break;
                }

                default:
                    stop = true;
                    break;
            }
        }

        if (quit) {
            quit();
        }
        else if (!stop && !filename.isEmpty() && !(play && canPlay)) {

            if (play) play(instrument, filename);

            if (canPlay) canPlay(instrument, filename);
        }
        else {
            displayUsage();
        }
    }
}
