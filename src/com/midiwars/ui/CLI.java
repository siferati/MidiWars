package com.midiwars.ui;

import com.midiwars.logic.MidiWars;
import com.midiwars.logic.instruments.Instrument;
import com.midiwars.logic.instruments.Instrument.Warning;
import org.xml.sax.SAXException;

import javax.sound.midi.InvalidMidiDataException;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

/** TODO exception handling structure.
 * Command line interface.
 */
public class CLI {

    /* --- DEFINES --- */

    /** Command to play a midi file. */
    public final static String CMD_PLAY = "-play";

    /** Command to check playability of a midi file. */
    public final static String CMD_CANPLAY = "-canplay";

    /** Option to not use default instrument. */
    public final static String OPT_INST = "-inst";


    /* --- ATTRIBUTES --- */

    /** Midi Wars app. */
    private MidiWars app;


    /* --- METHODS --- */

    /**
     * Starts the app.
     *
     * @param args List of arguments.
     */
    public CLI(String[] args) {

        try {
            app = new MidiWars();
            parse(args);
        }
        catch (IOException e) {
            System.out.println("\nERROR: Configurations file is missing.");
        }
        catch (NullPointerException e) {
            System.out.println("\nERROR: Configurations file doesn't have required format.");
        }
        catch (Instrument.InvalidInstrumentException e) {
            System.out.println("\nERROR: Default instrument listed in the configurations file is invalid.");
        }
        catch (SAXException e) {
            System.out.println("\nERROR: Couldn't parse configurations file.");
        }
        catch (ParserConfigurationException e) {
            System.out.println("\nERROR: There was a configuration error within the parser.");
        }
    }


    /**
     * Parses the given command line arguments
     * and decides what to do.
     *
     * @param args List of arguments.
     */
    private void parse(String[] args) {

        try {

            if (args.length == 0) {
                displayUsage();
                return;
            }

            // what user wants to do
            boolean play = false;
            boolean canPlay = false;

            String filepath = "";
            Instrument instrument = null;

            boolean stop = false;
            for (int i = 0; !stop && i < args.length; i++) {

                String arg = args[i];

                switch (arg) {

                    case CMD_PLAY: {

                        if (i != args.length - 1) {
                            filepath = args[i + 1];
                            play = true;
                            // skip next arg (since it's the filepath)
                            i++;
                        } else {
                            stop = true;
                        }
                        break;
                    }

                    case CMD_CANPLAY: {

                        if (i != args.length - 1) {
                            filepath = args[i + 1];
                            canPlay = true;
                            // skip next arg (since it's the filepath)
                            i++;
                        } else {
                            stop = true;
                        }
                        break;
                    }

                    case OPT_INST: {
                        if (i != args.length - 1) {
                            instrument = Instrument.newInstrument(args[i + 1]);
                            // skip next arg (since it's the filepath)
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

            if (!stop && !filepath.isEmpty() && !(play && canPlay)) {

                if (play) play(instrument, filepath);

                if (canPlay) canPlay(instrument, filepath);
            }
            else {
                displayUsage();
            }
        }
        catch (InvalidMidiDataException e) {
            System.out.println("\nInvalid MIDI data was encountered. Please provide a valid MIDI file for playback.");
            displayUsage();
        }
        catch (IOException e) {
            System.out.println("\nCouldn't find the given MIDI file. Please provide a valid filepath.");
            displayUsage();
        }
        catch (AWTException e) {
            System.out.println("\nERROR: Platform configuration does not allow low-level input control.");
        }
        catch (InterruptedException e) {
            System.out.println("\nERROR: Thread was interrupted while sleeping.");
        }
        catch (MidiWars.GameNotRunningException e){
            System.out.println("\nERROR: Couldn't find the game window. Please make sure the game is running and try again.");
        }
    }


    /**
     * Prints to the screen the usage information.
     */
    private void displayUsage() {

        System.out.println("\nUsage: java -jar MidiWars.jar [COMMAND] [OPTIONS]");
        System.out.println("\nPossible commands:\n");
        System.out.println("\t-play <FILENAME>\tPlays the given MIDI file using the default instrument.");
        System.out.println("\t-canplay <FILENAME>\tChecks if the given MIDI file can be properly played using the default instrument.");
        System.out.println("\nPossible options:\n");
        System.out.println("\t-inst <INSTRUMENT>\tCommands will use the given instrument instead of the default one. Resorts back to default instrument if given instrument is invalid.");
        System.out.println("\t                  \tPossible values: flute, harp, magbell.");
    }


    /**
     * Checks if the given midi file can be played by the given instrument.
     *
     * @param instrument Instrument to play given file with.
     * @param filepath Path of midi file to play.
     *
     * @throws InvalidMidiDataException Midi file is invalid.
     * @throws IOException Can't open file.
     */
    private void canPlay(Instrument instrument, String filepath) throws InvalidMidiDataException, IOException {

        System.out.println("\nChecking playability...");

        ArrayList<Warning> warnings = app.canPlay(instrument, filepath);

        for (Warning warning: warnings) {
            switch (warning) {
                case NOT_IN_RANGE:
                    System.out.println("\nWARNING: This midi file contains notes that this instrument can not play, therefore they will be skipped during playback.");
                    break;
                case TEMPO_TOO_FAST:
                    System.out.println("\nWARNING: This midi file's tempo is too fast - playback will probably be hindered. Lower the tempo for smoother playback.");
                    break;
                case NOTES_TOO_LONG:
                    System.out.println("\nWARNING: This midi file contains notes that are too long - they will probably be played twice. Lower their duration for smoother playback.");
                    break;
                case PAUSES_TOO_LONG:
                    System.out.println("\nWARNING: This midi file contains pauses that are too long - probably due to an error in the midi file.");
                    break;
                default:
                    System.out.println("\nWARNING: Unknown warning caused by this midi file.");
                    break;
            }
        }

        if (warnings.size() == 0) {
            System.out.println("\nNo problems found. MIDI file is ready for playback.");
        }
    }


    /**
     * Plays the given file.
     *
     * @param instrument Instrument to play given file with.
     * @param filepath File to play.
     *
     * @throws InvalidMidiDataException Midi file is invalid.
     * @throws IOException Can't open file.
     */
    private void play(Instrument instrument, String filepath) throws InvalidMidiDataException, IOException, AWTException, InterruptedException, MidiWars.GameNotRunningException {

        // check playability
        canPlay(instrument, filepath);

        System.out.println("\nStarting playback in...");
        for (int i = 0; i < 5; i++) {
            System.out.println("... " + (5 - i));
            Thread.sleep(1000);
        }

        app.play(instrument, filepath);


    }
}
