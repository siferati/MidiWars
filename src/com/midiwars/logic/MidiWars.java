package com.midiwars.logic;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;

public class MidiWars {

    /* --- DEFINES --- */

    public static String FILEPATH = "C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\Midi Files\\Memory - Undertale.mid";
    //public static String FILEPATH = "C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\Midi Files\\Light your heart up - Kill la Kill.mid";

    public static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    public static final int END_OF_TRACk = 0x2F;
    public static final int SET_TEMPO = 0x51;


    /* --- ATTRIBUTES --- */


    /* --- METHODS --- */

    /**
     * Default Constructor
     */
    public MidiWars() {

    }


    /**
     * Reads midi file
     *
     * @throws InvalidMidiDataException
     * @throws IOException
     */
    public void readMidiFile() throws InvalidMidiDataException, IOException {

        // read sequence from midi file
        Sequence sequence = MidiSystem.getSequence(new File(FILEPATH));

        // iterate through every Track
        for (Track track : sequence.getTracks()) {

            // iterate through every MidiEvent
            for (int i = 0; i < track.size(); i++) {

                // get current MidiEvent
                MidiEvent event = track.get(i);

                // debug
                String output = "time: " + event.getTick();

                // get current MidiMessage
                MidiMessage message = event.getMessage();

                // case ShortMessage
                if (message instanceof ShortMessage) {

                    ShortMessage shortMessage = (ShortMessage) message;

                    switch (shortMessage.getCommand()) {

                        // key press
                        case ShortMessage.NOTE_ON: {

                            // get key (1-128)
                            int key = shortMessage.getData1();

                            // get octave
                            int octave = (key / 12) - 1;

                            // get musical note
                            int note = key % 12;
                            String noteName = NOTE_NAMES[note];

                            // get velocity
                            int velocity = shortMessage.getData2();

                            output += " Note on, " + noteName + octave + " key: " + key + " velocity: " + velocity;

                            break;
                        }

                        default:
                            output += " Command:" + shortMessage.getCommand();

                            break;
                    }
                }

                // case MetaMessage
                else if (message instanceof MetaMessage) {
                    // TODO bpm

                    MetaMessage metaMessage = (MetaMessage) message;

                    switch (metaMessage.getType()) {

                        case END_OF_TRACk: {

                            output += " END OF TRACK";
                            break;
                        }

                        case SET_TEMPO: {

                            output += " SET TEMPO";
                            break;
                        }

                        default:
                            break;
                    }
                }

                // case SysexMessage
                else if (message instanceof SysexMessage) {
                    // TODO ...
                }

                System.out.println(output);
            }
        }
    }
}
