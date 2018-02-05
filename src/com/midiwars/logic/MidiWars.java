package com.midiwars.logic;

import com.midiwars.logic.instruments.Instrument;
import com.midiwars.logic.instruments.Instrument.*;
import com.midiwars.logic.midi.MidiTimeline;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.sound.midi.InvalidMidiDataException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.*;

/**
 * Represents the application itself.
 */
public class MidiWars {

    /* --- DEFINES --- */

    public static final String CONFIGPATH = "./config.xml";

    /* --- ATTRIBUTES --- */

    /** Path to where midi files are stored. */
    private String midiPath;

    /** Default instrument. */
    private Instrument defaultInstrument;


    /* --- METHODS --- */

    /**
     * Default Constructor.
     */
    public MidiWars() throws IOException, SAXException, ParserConfigurationException, NullPointerException, InvalidInstrumentException {

        loadConfigs();
    }


    /**
     * Plays the given midi file.
     *
     * @param instrument Instrument to play given file with.
     * @param filepath Path of midi file to play.
     *
     * @throws InvalidMidiDataException Midi file is invalid.
     * @throws IOException Can't open file.
     * @throws CantPlayMidiException If the midi file can't be properly played.
     * @throws AWTException If the platform configuration does not allow low-level input control.
     */
    public void play(Instrument instrument, String filepath) throws InvalidMidiDataException, IOException, CantPlayMidiException, AWTException {

        // construct timeline from midi file
        MidiTimeline midiTimeline = new MidiTimeline("C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\Midi Files\\" + filepath);

        // check for warnings
        try {
            if (instrument == null) {
                instrument = defaultInstrument;
            }
            instrument.canPlay(midiTimeline);
        } finally {
            instrument.play(midiTimeline);
        }
    }


    /**
     * Parses configuration file
     * and loads necessary info.
     *
     * @throws ParserConfigurationException If there was a configuration error within the parser.
     * @throws IOException If configurations file is missing.
     * @throws SAXException If couldn't parse configurations file.
     * @throws NullPointerException If configurations file doesn't have required format.
     */
    private void loadConfigs() throws ParserConfigurationException, IOException, SAXException, NullPointerException, InvalidInstrumentException {

        // setup doc
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(CONFIGPATH));

        // get first occurrence only
        midiPath = doc.getDocumentElement().getElementsByTagName("midipath").item(0).getTextContent();
        defaultInstrument = Instrument.newInstrument(doc.getDocumentElement().getElementsByTagName("instrument").item(0).getTextContent());

        /* TODO
        File path = new File(midiPath);
        if (!path.exists() || !path.isDirectory()) {
            throw new
        }*/

        if (defaultInstrument == null) {
            throw new InvalidInstrumentException();
        }
    }
}
