package com.midiwars.logic;

import com.midiwars.logic.instruments.Instrument;
import com.midiwars.logic.instruments.Instrument.*;
import com.midiwars.logic.instruments.InstrumentFactory;
import com.midiwars.util.MyExceptions.InvalidInstrumentException;
import com.midiwars.util.MyExceptions.MidiPathNotFoundException;
import com.midiwars.util.MyExceptions.MidifilesNotFoundException;
import com.midiwars.logic.midi.MidiTimeline;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.sound.midi.InvalidMidiDataException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * The application itself.
 */
public class MidiWars {

    /* --- DEFINES --- */

    /** Path to configurations file. */
    public static final String CONFIGPATH = "config.xml";


    /* --- ATTRIBUTES --- */

    /** Path to where midi files are stored. */
    private String midiPath;

    /** Default instrument. */
    private Instrument defaultInstrument;

    /** The music player. */
    private Player player;


    /* --- METHODS --- */

    /**
     * Creates a new MidiWars object.
     *
     * @throws InvalidInstrumentException If default instrument listed in the configurations file is invalid.
     * @throws IOException If couldn't extract configs from resources.
     * @throws MidiPathNotFoundException If default path listed in the configurations file is invalid.
     * @throws NullPointerException If configurations file doesn't have required format.
     * @throws ParserConfigurationException If there was a configuration error within the parser.
     * @throws SAXException If couldn't parse configurations file.
     */
    public MidiWars() throws InvalidInstrumentException, IOException, MidiPathNotFoundException, NullPointerException, ParserConfigurationException, SAXException {

        loadConfigs();

        player = Player.getInstance();
    }


    /**
     * Plays the given midi file or playlist.
     *
     * @param instrument Instrument to play with.
     * @param filename Name of midi file / playlist to play.
     *
     * @throws AWTException If the platform configuration does not allow low-level input control.
     * @throws InterruptedException If a thread was interrupted.
     * @throws InvalidMidiDataException If midi file is invalid.
     * @throws IOException If can't open file.
     * @throws MidifilesNotFoundException If couldn't find the midi files listed in the playlist.
     * @throws ParserConfigurationException If there was a configuration error within the parser.
     * @throws SAXException If couldn't parse playlist file.
     */
    public void play(Instrument instrument, String filename) throws AWTException, InterruptedException, InvalidMidiDataException, IOException, MidifilesNotFoundException, ParserConfigurationException, SAXException {

        if (filename.endsWith(".xml")) {

            // setup doc
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(midiPath + filename));

            // get midifiles
            NodeList nodeList = doc.getDocumentElement().getElementsByTagName("midifile");
            String[] midifiles = new String[nodeList.getLength()];
            for (int i = 0; i < nodeList.getLength(); i++) {
                midifiles[i] = midiPath + nodeList.item(i).getTextContent();
            }

            // get options
            boolean shuffle = doc.getDocumentElement().getElementsByTagName("shuffle").getLength() > 0;
            boolean repeat = doc.getDocumentElement().getElementsByTagName("repeat").getLength() > 0;

            // instrument priority: command > playlist > default
            if (instrument == null) {
                NodeList instruments = doc.getDocumentElement().getElementsByTagName("instrument");
                if (instruments.getLength() > 0) {
                    instrument = InstrumentFactory.newInstrument(instruments.item(0).getTextContent());
                }
            }
            if (instrument == null) {
                instrument = defaultInstrument;
            }

            // check if midifiles are valid
            for (String midifile : midifiles) {
                File file = new File(midifile);
                if (!file.exists() || file.isDirectory()) {
                    throw new MidifilesNotFoundException();
                }
            }

            // play list
            player.play(midifiles, shuffle, repeat, instrument);
        }
        else {

            // default instrument
            if (instrument == null) {
                instrument = defaultInstrument;
            }

            // play
            player.play(new String[] {midiPath + filename}, false, false, instrument);
        }
    }


    /**
     * Pauses playback.
     *
     * @throws InterruptedException If a thread was interrupted.
     */
    public void pause() throws InterruptedException {
        player.pause();
    }


    /**
     * Resumes playback.
     *
     * @throws AWTException If the platform configuration does not allow low-level input control.
     * @throws InterruptedException If a thread was interrupted.
     * @throws InvalidMidiDataException If midi file is invalid.
     * @throws IOException If can't open file.
     */
    public void resume() throws AWTException, InterruptedException, InvalidMidiDataException, IOException {
        player.resume();
    }


    /**
     * Stops playback.
     *
     * @throws InterruptedException If a thread was interrupted.
     */
    public void stop() throws InterruptedException {
        player.stop();
    }


    /**
     * Plays previous song.
     *
     * @throws AWTException If the platform configuration does not allow low-level input control.
     * @throws InterruptedException If a thread was interrupted.
     * @throws InvalidMidiDataException If midi file is invalid.
     * @throws IOException If can't open file.
     */
    public void prev() throws AWTException, InterruptedException, InvalidMidiDataException, IOException {
        player.prev();
    }


    /**
     * Plays the next song.
     *
     * @throws AWTException If the platform configuration does not allow low-level input control.
     * @throws InterruptedException If a thread was interrupted.
     * @throws InvalidMidiDataException If midi file is invalid.
     * @throws IOException If can't open file.
     */
    public void next() throws AWTException, InterruptedException, InvalidMidiDataException, IOException {
        player.next();
    }


    /**
     * Checks if the given midi file can be played by the given instrument.
     *
     * @param instrument Instrument to play given file with.
     * @param filepath Path of midi file to play.
     *
     * @return List of warnings.
     *
     * @throws InvalidMidiDataException If midi file is invalid.
     * @throws IOException If can't open file.
     *
     * @see com.midiwars.logic.instruments.Instrument.Warning
     */
    public ArrayList<Warning> canPlay(Instrument instrument, String filepath) throws InvalidMidiDataException, IOException {

        // construct timeline from midi file
        MidiTimeline midiTimeline = new MidiTimeline(midiPath + filepath);

        if (instrument == null) {
            instrument = defaultInstrument;
        }

        return instrument.canPlay(midiTimeline);
    }


    /**
     * Parses configuration file
     * and loads necessary info.
     *
     * @throws InvalidInstrumentException If default instrument listed in the configurations file is invalid.
     * @throws IOException If couldn't extract configs from resources.
     * @throws MidiPathNotFoundException If default path listed in the configurations file is invalid.
     * @throws NullPointerException If configurations file doesn't have required format.
     * @throws ParserConfigurationException If there was a configuration error within the parser.
     * @throws SAXException If couldn't parse configurations file.
     */
    private void loadConfigs() throws InvalidInstrumentException, IOException, MidiPathNotFoundException, NullPointerException, ParserConfigurationException, SAXException {

        // extract configs in case it doesn't already exist
        File configs = new File(CONFIGPATH);
        if (!configs.exists()) {
            InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIGPATH);
            Files.copy(is, Paths.get(CONFIGPATH));
            is.close();
        }

        // setup doc
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(configs);

        // get first occurrence only
        midiPath = doc.getDocumentElement().getElementsByTagName("midipath").item(0).getTextContent();
        defaultInstrument = InstrumentFactory.newInstrument(doc.getDocumentElement().getElementsByTagName("instrument").item(0).getTextContent());

        // make sure path has a trailing slash
        if (!midiPath.endsWith("/") && !midiPath.endsWith("\\")) {
            midiPath += File.separator;
        }

        // check if path is valid
        File path = new File(midiPath);
        if (!path.exists() || !path.isDirectory()) {
            throw new MidiPathNotFoundException();
        }

        if (defaultInstrument == null) {
            throw new InvalidInstrumentException();
        }
    }


    /**
     * Getter.
     *
     * @return {@link #midiPath Midi path}.
     */
    public String getMidiPath() {
        return midiPath;
    }
}
