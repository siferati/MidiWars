package com.midiwars.ui;

import com.midiwars.logic.instruments.Instrument;

/**
 * Parses the commands inserted either through the CLI or the GCI.
 */
public class Parser {

    /* --- DEFINES --- */

    /** Command to exit the program. */
    public final static String CMD_QUIT = "/quit";

    /** Command to play a midi file. */
    public static final String CMD_PLAY = "/play";

    /** Command to check playability of a midi file. */
    public final static String CMD_CANPLAY = "/canplay";

    /**  Option to not use default instrument. */
    public final static String OPT_INST = "-inst";


    /* --- ATTRIBUTES --- */

    /**  User interface to call. */
    UserInterface ui;


    /* --- METHODS --- */

    /**
     * Creates a new Parser object.
     *
     * @param ui User interface to call.
     */
    public Parser(UserInterface ui) {

        this.ui = ui;
    }


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
                        instrument = Instrument.newInstrument(args[i + 1]);
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
            ui.quit();
        }
        else if (!stop && !filename.isEmpty() && !(play && canPlay)) {

            if (play) ui.play(instrument, filename);

            if (canPlay) ui.canPlay(instrument, filename);
        }
        else {
            ui.displayUsage();
        }
    }
}
