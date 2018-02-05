package com.midiwars.logic;

import com.midiwars.logic.instruments.Instrument.*;
import com.midiwars.logic.instruments.MagBell;
import com.midiwars.logic.midi.MidiTimeline;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.sound.midi.InvalidMidiDataException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Represents the application itself.
 */
public class MidiWars {

    /* --- DEFINES --- */

    public static final String CONFIGPATH = "./config.xml";

    /* --- ATTRIBUTES --- */

    private String midiPath;
    private String apiKey;


    /* --- METHODS --- */

    /**
     * Default Constructor.
     */
    public MidiWars() throws IOException, SAXException, ParserConfigurationException, NullPointerException {

        loadConfigs();
    }


    /**
     * Plays the given midi file.
     *
     * @param filepath Path of midi file to play.
     *
     * @throws InvalidMidiDataException Midi file is invalid.
     * @throws IOException Can't open file.
     * @throws CantPlayMidiException If the midi file can't be properly played.
     * @throws InterruptedException If thread was interrupted while sleeping.
     * @throws AWTException If the platform configuration does not allow low-level input control.
     */
    public void play(String filepath) throws InvalidMidiDataException, IOException, CantPlayMidiException, InterruptedException, AWTException {

        // construct timeline from midi file
        MidiTimeline midiTimeline = new MidiTimeline("C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\Midi Files\\" + filepath);

        // TODO fetch instrument from api return true false in method
        MagBell magBell = new MagBell();

        // check for warnings
        try {
            magBell.canPlay(midiTimeline);
        } finally {
            Thread.sleep(5000);
            magBell.play(midiTimeline);
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
    private void loadConfigs() throws ParserConfigurationException, IOException, SAXException, NullPointerException {

        // setup doc
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(CONFIGPATH));

        // get first occurrence only
        midiPath = doc.getDocumentElement().getElementsByTagName("midipath").item(0).getTextContent();
        apiKey = doc.getDocumentElement().getElementsByTagName("apikey").item(0).getTextContent();
    }
}
