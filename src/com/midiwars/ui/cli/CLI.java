package com.midiwars.ui.cli;

import com.midiwars.logic.MidiWars;
import com.midiwars.logic.Instrument.Warning;
import com.midiwars.ui.UserInterface;
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
            System.out.println("Error: Configurations file is missing.");
        }
        catch (NullPointerException e) {
            System.out.println("Error: Configurations file doesn't have required format.");
        }
        catch (MidiPathNotFoundException e) {
            System.out.println("Error: Default path listed in the configurations file is invalid.");
        }
        catch (SAXException e) {
            System.out.println("Error: Couldn't parse configurations file.");
        }
        catch (ParserConfigurationException e) {
            System.out.println("Error: There was a configuration error within the parser.");
        }
    }


    @Override
    public void displayUsage() {

        System.out.println("\nUsage: java -jar MidiWars.jar [command]");
        System.out.println("\nPossible commands:\n");
        System.out.println("\tplay    <FILENAME>\tPlays the given midi file or playlist.\n");
        System.out.println("\tcanplay <FILENAME>\tChecks if the given midi file can be properly played.");
    }


    @Override
    public void canPlay(String filename, boolean explicit) {

        try {

            if (!explicit) {
                filename = filename.replace(app.getMidiPath(), "");
            }

            ArrayList<Warning> warnings = app.canPlay(filename);

            for (Warning warning: warnings) {
                switch (warning) {
                    case NOT_IN_RANGE:
                        System.out.println("Warning: This midi file contains notes that this instrument can not play, therefore they will be skipped during playback.");
                        break;
                    case TEMPO_TOO_FAST:
                        System.out.println("Warning: This midi file's tempo is too fast - playback will probably be hindered. Lower the tempo for smoother playback.");
                        break;
                    case NOTES_TOO_LONG:
                        System.out.println("Warning: This midi file contains notes that are too long - they will probably be played twice. Lower their duration for smoother playback.");
                        break;
                    case PAUSES_TOO_LONG:
                        System.out.println("Warning: This midi file contains pauses that are too long - probably due to an error in the midi file.");
                        break;
                    default:
                        System.out.println("Warning: Unknown warning caused by this midi file.");
                        break;
                }
            }

            if (warnings.size() == 0 && explicit) {
                System.out.println("No problems found. midi file is ready for playback.");
            }
        }
        catch (InvalidMidiDataException e) {
            System.out.println("Invalid midi data was encountered. Please provide a valid midi file for playback.");
            displayUsage();
        }
        catch (IOException e) {
            System.out.println("Couldn't find the given midi file. Please provide a valid filename.");
            displayUsage();
        }
    }


    @Override
    public void play(String filename) {

        try {

            System.out.println("Playback will start shortly. Please switch focus to the target window.");
            System.out.println("Starting playback in...");
            for (int i = 0; i < 5; i++) {
                System.out.println("... " + (5 - i));
                Thread.sleep(1000);
            }

            app.play(filename);

        } catch (InterruptedException e) {
            System.out.println("Error: Thread was interrupted while sleeping.");
        } catch (AWTException e) {
            System.out.println("Error: Platform configuration does not allow low-level input control.");
        } catch (ParserConfigurationException e) {
            System.out.println("Error: There was a configuration error within the parser.");
        } catch (MidifilesNotFoundException e) {
            System.out.println("Couldn't find the midi files listed in the playlist. Please provide valid filenames.");
            displayUsage();
        } catch (SAXException e) {
            System.out.println("Error: Couldn't parse playlist file.");
        } catch (InvalidMidiDataException e) {
            System.out.println("Invalid midi data was encountered. Please provide a valid midi file for playback.");
            displayUsage();
        } catch (IOException e) {
            System.out.println("Couldn't find the given midi file. Please provide a valid filename.");
            displayUsage();
        }
    }

    @Override
    public void pause() {
        System.out.println("This command is invalid for the current application mode.");
        displayUsage();
    }

    @Override
    public void resume() {
        System.out.println("This command is invalid for the current application mode.");
        displayUsage();
    }

    @Override
    public void stop() {
        System.out.println("This command is invalid for the current application mode.");
        displayUsage();
    }

    @Override
    public void prev() {
        System.out.println("This command is invalid for the current application mode.");
        displayUsage();
    }

    @Override
    public void next() {
        System.out.println("This command is invalid for the current application mode.");
        displayUsage();
    }

    @Override
    public void quit() {
        System.out.println("Program exited.");
    }
}
