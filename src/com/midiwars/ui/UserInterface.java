package com.midiwars.ui;

import com.midiwars.logic.instruments.Instrument;
import com.midiwars.logic.instruments.InstrumentFactory;

/** TODO picocli
 * Represents a user interface.
 */
public abstract class UserInterface {

    /* --- DEFINES --- */

    /** All commands coming from the in-game chat should start with this string. */
    public final static String CMD_GCI = "/mw";

    /** Command to exit the program. */
    public final static String CMD_QUIT = "quit";

    /** Command to play a midi file. */
    public static final String CMD_PLAY = "play";

    /** Command to pause the playback of the playlist. */
    public static final String CMD_PAUSE = "pause";

    /** Command to resume the playback of the playlist. */
    public static final String CMD_RESUME = "resume";

    /** Command to stop the playback of the playlist. */
    public static final String CMD_STOP = "stop";

    /** Command to play previous song. */
    public static final String CMD_PREV = "prev";

    /** Command to play next song. */
    public static final String CMD_NEXT = "next";

    /** Command to check playability of a midi file. */
    public final static String CMD_CANPLAY = "canplay";

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
     * Plays the songs listed in the given playlist.
     *
     * @param instrument Instrument to play given playlist with.
     * @param filename Playlist to play.
     */
    abstract void playlist(Instrument instrument, String filename);


    /**
     * Pauses the playback.
     */
    abstract void pause();


    /**
     * Resumes playback.
     */
    abstract void resume();


    /**
     * Stops playback.
     */
    abstract void stop();


    /**
     * Plays previous song.
     */
    abstract void prev();


    /**
     * Plays next song.
     */
    abstract void next();


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
        boolean pause = false;
        boolean resume = false;
        boolean prev = false;
        boolean next = false;
        boolean stop = false;
        boolean canPlay = false;
        boolean quit = false;
        int nOps = 0;

        String filename = "";
        Instrument instrument = null;

        boolean exit = false;
        for (int i = 0; !exit && i < args.length; i++) {

            String arg = args[i];

            switch (arg) {

                case CMD_QUIT: {

                    if (i == 0 && args.length == 1) {
                        quit = true;
                    }
                    else {
                        exit = true;
                    }
                    nOps++;
                    break;
                }

                case CMD_PLAY: {

                    if (i != args.length - 1) {
                        filename = args[i + 1];
                        play = true;
                        // skip next arg (since it's the filename)
                        i++;
                    } else {
                        exit = true;
                    }
                    nOps++;
                    break;
                }

                case CMD_PAUSE: {

                    if (i == 0 && args.length == 1) {
                        pause = true;
                    }
                    else {
                        exit = true;
                    }
                    nOps++;
                    break;
                }

                case CMD_RESUME: {

                    if (i == 0 && args.length == 1) {
                        resume = true;
                    }
                    else {
                        exit = true;
                    }
                    nOps++;
                    break;
                }

                case CMD_STOP: {

                    if (i == 0 && args.length == 1) {
                        stop = true;
                    }
                    else {
                        exit = true;
                    }
                    nOps++;
                    break;
                }

                case CMD_PREV: {

                    if (i == 0 && args.length == 1) {
                        prev = true;
                    }
                    else {
                        exit = true;
                    }
                    nOps++;
                    break;
                }

                case CMD_NEXT: {

                    if (i == 0 && args.length == 1) {
                        next = true;
                    }
                    else {
                        exit = true;
                    }
                    nOps++;
                    break;
                }

                case CMD_CANPLAY: {

                    if (i != args.length - 1) {
                        filename = args[i + 1];
                        canPlay = true;
                        // skip next arg (since it's the filename)
                        i++;
                    } else {
                        exit = true;
                    }
                    nOps++;
                    break;
                }

                case OPT_INST: {

                    if (i != args.length - 1) {
                        instrument = InstrumentFactory.newInstrument(args[i + 1]);
                        // skip next arg (since it's the instrument)
                        i++;
                    } else {
                        exit = true;
                    }
                    break;
                }

                default:
                    exit = true;
                    break;
            }
        }

        if (quit) {
            quit();
        }
        else if (!exit && nOps == 1) {

            if (play) {
                if (filename.endsWith(".xml")) {
                    playlist(instrument, filename);
                } else {
                    play(instrument, filename);
                }
            }

            if (canPlay) canPlay(instrument, filename);

            if (pause) pause();

            if (resume) resume();

            if (stop) stop();

            if (prev) prev();

            if (next) next();
        }
        else {
            displayUsage();
        }
    }
}
