package com.midiwars.ui.cli;

import com.midiwars.logic.MidiWars;
import com.midiwars.logic.instruments.Instrument;
import com.midiwars.logic.instruments.Instrument.Warning;
import com.midiwars.ui.UserInterface;
import com.midiwars.util.MyExceptions.InvalidInstrumentException;
import com.midiwars.util.MyExceptions.MidifilesNotFoundException;
import com.midiwars.util.MyExceptions.MidiPathNotFoundException;
import org.xml.sax.SAXException;

import javax.sound.midi.InvalidMidiDataException;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Command line interface.
 */
public class CLI extends UserInterface {

    /* --- ATTRIBUTES --- */

    /** Midi Wars app. */
    private MidiWars app;


    /* --- METHODS --- */

    /**
     * Creates a new CLI object.
     */
    public CLI() {

        try {
            app = new MidiWars();
        }
        catch (IOException e) {
            System.out.println("\nERROR: Configurations file is missing.");
        }
        catch (NullPointerException e) {
            System.out.println("\nERROR: Configurations file doesn't have required format.");
        }
        catch (InvalidInstrumentException e) {
            System.out.println("\nERROR: Default instrument listed in the configurations file is invalid.");
        }
        catch (MidiPathNotFoundException e) {
            System.out.println("\nERROR: Default path listed in the configurations file is invalid.");
        }
        catch (SAXException e) {
            System.out.println("\nERROR: Couldn't parse configurations file.");
        }
        catch (ParserConfigurationException e) {
            System.out.println("\nERROR: There was a configuration error within the parser.");
        }
    }


    @Override
    public void displayUsage() {

        System.out.println("\nUsage: java -jar MidiWars.jar [COMMAND] [OPTIONS]");
        System.out.println("\nPossible commands:\n");
        System.out.println("\tplay <FILENAME>\tPlays the given MIDI file using the default instrument.");
        System.out.println("\tcanplay <FILENAME>\tChecks if the given MIDI file can be properly played using the default instrument.");
        System.out.println("\nPossible options:\n");
        System.out.println("\t-inst <INSTRUMENT>\tCommands will use the given instrument instead of the default one. Resorts back to default instrument if given instrument is invalid.");
        System.out.println("\t                  \tPossible values: flute, harp, magbell.");
    }


    @Override
    public void canPlay(Instrument instrument, String filename, boolean explicit) {

        try {

            System.out.println("\nChecking playability...");

            if (!explicit) {
                filename = filename.replace(app.getMidiPath(), "");
            }

            ArrayList<Warning> warnings = app.canPlay(instrument, filename);

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

            if (warnings.size() == 0 && explicit) {
                System.out.println("\nNo problems found. MIDI file is ready for playback.");
            }
        }
        catch (InvalidMidiDataException e) {
            System.out.println("\nInvalid MIDI data was encountered. Please provide a valid MIDI file for playback.");
            displayUsage();
        }
        catch (IOException e) {
            System.out.println("\nCouldn't find the given MIDI file. Please provide a valid filename.");
            displayUsage();
        }
    }


    @Override
    public void play(Instrument instrument, String filename) {

        try {

            System.out.println("Playback will start shortly. Please open");

            app.play(instrument, filename);

        } catch (InterruptedException e) {
            System.out.println("\nERROR: Thread was interrupted while sleeping.");
        } catch (AWTException e) {
            System.out.println("\nERROR: Platform configuration does not allow low-level input control.");
        } catch (ParserConfigurationException e) {
            System.out.println("\nERROR: There was a configuration error within the parser.");
        } catch (MidifilesNotFoundException e) {
            System.out.println("\nCouldn't find the MIDI files listed in the playlist. Please provide valid filenames.");
            displayUsage();
        } catch (SAXException e) {
            System.out.println("\nERROR: Couldn't parse playlist file.");
        } catch (InvalidMidiDataException e) {
            System.out.println("\nInvalid MIDI data was encountered. Please provide a valid MIDI file for playback.");
            displayUsage();
        } catch (IOException e) {
            System.out.println("\nCouldn't find the given MIDI file. Please provide a valid filename.");
            displayUsage();
        }
    }

    @Override
    public void pause() {
        System.out.println("\nThis command is invalid for the current application mode.");
        displayUsage();
    }

    @Override
    public void resume() {
        System.out.println("\nThis command is invalid for the current application mode.");
        displayUsage();
    }

    @Override
    public void stop() {
        System.out.println("\nThis command is invalid for the current application mode.");
        displayUsage();
    }

    @Override
    public void prev() {
        System.out.println("\nThis command is invalid for the current application mode.");
        displayUsage();
    }

    @Override
    public void next() {
        System.out.println("\nThis command is invalid for the current application mode.");
        displayUsage();
    }

    @Override
    public void quit() {
        System.out.println("Program exited.");
    }
}
