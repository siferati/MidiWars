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

/**
 * Command line interface.
 */
public class CLI {

    /* --- ATTRIBUTES --- */

    /** Midi Wars app. */
    private MidiWars app;


    /* --- METHODS --- */

    /**
     * Default Constructor.
     *
     * @param args List of arguments.
     */
    public CLI(String[] args) {

        try {
            app = new MidiWars();
            parse(args);
        }
        catch (IOException e) {
            System.out.println("ERROR: Configurations file is missing.");
        }
        catch (NullPointerException e) {
            System.out.println("ERROR: Configurations file doesn't have required format.");
        }
        catch (Instrument.InvalidInstrumentException e) {
            System.out.println("ERROR: Default instrument listed in the configurations file is invalid.");
        }
        catch (SAXException e) {
            System.out.println("ERROR: Couldn't parse configurations file.");
        }
        catch (ParserConfigurationException e) {
            System.out.println("ERROR: There was a configuration error within the parser.");
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
            String filepath = "";
            Instrument instrument = null;

            boolean stop = false;
            for (int i = 0; !stop && i < args.length; i++) {

                String arg = args[i];

                switch (arg) {

                    case "-play": {

                        if (i != args.length - 1) {
                            filepath = args[i + 1];
                            // skip next arg (since it's the filepath)
                            i++;
                        } else {
                            stop = true;
                        }
                        break;
                    }

                    case "-inst": {
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

            if (!stop && !filepath.isEmpty()) {
                play(instrument, filepath);
            } else {
                displayUsage();
            }
        }
        catch (InvalidMidiDataException e) {
            System.out.println("Invalid MIDI data was encountered. Please provide a valid MIDI file for playback.");
            displayUsage();
        }
        catch (IOException e) {
            System.out.println("Couldn't find the given MIDI file. Please provide a valid filepath.");
            displayUsage();
        }
    }


    /**
     * Prints to the screen the usage information.
     */
    private void displayUsage() {

        System.out.println("Usage: java -jar MidiWars.jar -play <FILEPATH> [OPTIONS]");
        System.out.println("\nPossible options:\n");
        System.out.println("\t-inst <INSTRUMENT>\tInstrument to play given midi file with. Resorts back to default instrument if option is missing or instrument is invalid.");
        System.out.println("\t                  \tInstruments: flute, harp, magbell.");
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
    private void play(Instrument instrument, String filepath) throws InvalidMidiDataException, IOException {

        try {
            System.out.println("Starting playback in...");
            for (int i = 0; i < 5; i++) {
                System.out.println("... " + (5 - i));
                Thread.sleep(1000);
            }
            app.play(instrument, filepath);
        }
        catch (Instrument.CantPlayMidiException e) {
            displayWarnings(e.warnings);
        }
        catch (InterruptedException e) {
            System.out.println("ERROR: Thread was interrupted while sleeping.");
        }
        catch (AWTException e) {
            System.out.println("ERROR: Platform configuration does not allow low-level input control.");
        }
    }

    /** TODO warnings are showing at the end and not beginning
     * Displays the given warnings to the user.
     *
     * @param warnings Warnings to display.
     */
    private void displayWarnings(ArrayList<Warning> warnings) {

        for (Warning warning: warnings) {
            switch (warning) {
                case NOT_IN_RANGE:
                    System.out.println("WARNING: This midi file contains notes that this instrument can not play, therefore they will be skipped during playback.");
                    break;
                case TEMPO_TOO_FAST:
                    System.out.println("WARNING: This midi file's tempo is too fast - playback will probably be hindered. Lower the tempo for smoother playback.");
                    break;
                case NOTES_TOO_LONG:
                    System.out.println("WARNING: This midi file contains notes that are too long - they will probably be played twice. Lower their duration for smoother playback.");
                    break;
                case PAUSES_TOO_LONG:
                    System.out.println("WARNING: This midi file contains pauses that are too long - probably due to an error in the midi file.");
                    break;
                default:
                    System.out.println("WARNING: Unknown warning caused by this midi file.");
                    break;
            }
        }
    }
}
