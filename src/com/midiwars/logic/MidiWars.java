package com.midiwars.logic;

import com.midiwars.logic.instruments.Instrument;
import com.midiwars.logic.instruments.Instrument.*;
import com.midiwars.logic.instruments.InstrumentFactory;
import com.midiwars.logic.instruments.InstrumentFactory.*;
import com.midiwars.logic.midi.MidiTimeline;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.sound.midi.InvalidMidiDataException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;

/**
 * Represents the application itself.
 */
public class MidiWars {

    /* --- DEFINES --- */

    public static class GameNotRunningException extends Exception {

    }

    public static class MidiPathNotFoundException extends Exception {

    }

    public static class MidifilesNotFoundException extends Exception {

    }

    /** Name of the window of the game. */
    public final static String GAME_WINDOW = "Guild Wars 2";

    /** Path to configurations file. */
    public static final String CONFIGPATH = "./config.xml";


    /* --- ATTRIBUTES --- */

    /** JNA mapping of User32.dll functions. */
    private User32 user32;

    /** Path to where midi files are stored. */
    private String midiPath;

    /** Default instrument. */
    private Instrument defaultInstrument;

    /** The music player. */
    private Player player;


    /* --- METHODS --- */

    /**
     * Default Constructor.
     */
    public MidiWars() throws IOException, SAXException, ParserConfigurationException, NullPointerException, InvalidInstrumentException, MidiPathNotFoundException {

        user32 = User32.INSTANCE;

        loadConfigs();

        player = Player.getInstance();
    }


    /**
     * Plays the given midi file.
     *
     * @param instrument Instrument to play given file with.
     * @param filepath Path of midi file to play.
     *
     * @throws InvalidMidiDataException Midi file is invalid.
     * @throws IOException Can't open file.
     * @throws AWTException If the platform configuration does not allow low-level input control.
     */
    public void play(Instrument instrument, String filepath) throws InvalidMidiDataException, IOException, AWTException, GameNotRunningException, InterruptedException {

        // TODO work only when guildwars is the active window - install alt tab hook
        // find guild wars window
        WinDef.HWND gameWindow = user32.FindWindow(null, GAME_WINDOW);

        // bring window to the front
        if (gameWindow != null) {
            // TODO uncomment user32.SetForegroundWindow(gameWindow);
        }
        else {
            // TODO uncomment throw new GameNotRunningException();
        }

        // default instrument
        if (instrument == null) {
            instrument = defaultInstrument;
        }

        // play
        player.play(new String[] {midiPath + filepath}, false, false, instrument);
    }


    /**
     * Plays the given playlist.
     *
     * @param instrument Instrument to play given playlist with.
     * @param filepath Path of playlist to play.
     *
     * @throws ParserConfigurationException If there was a configuration error within the parser.
     * @throws IOException If playlist file is missing.
     * @throws SAXException If couldn't parse playlist file.
     * @throws NullPointerException If playlist file doesn't have required format.
     */
    public void playlist(Instrument instrument, String filepath) throws ParserConfigurationException, IOException, SAXException, NullPointerException, MidifilesNotFoundException, AWTException, InvalidMidiDataException, GameNotRunningException, InterruptedException {

        // setup doc
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(midiPath + filepath));

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


    /**
     * Pauses playback.
     */
    public void pause() throws InterruptedException {
        player.pause();
    }


    /**
     * Resumes playback.
     */
    public void resume() throws AWTException, InvalidMidiDataException, InterruptedException, IOException {
        player.resume();
    }


    /**
     * Stops playback.
     */
    public void stop() throws InterruptedException {
        player.stop();
    }


    /**
     * Plays previous song.
     */
    public void prev() throws InterruptedException, AWTException, InvalidMidiDataException, IOException {
        player.prev();
    }


    /**
     * Plays the next song.
     */
    public void next() throws InterruptedException, AWTException, InvalidMidiDataException, IOException {
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
     * @throws InvalidMidiDataException Midi file is invalid.
     * @throws IOException Can't open file.
     *
     * @see import com.midiwars.logic.instruments.Instrument.Warning
     */
    public ArrayList<Warning> canPlay(Instrument instrument, String filepath) throws InvalidMidiDataException, IOException {

        // construct timeline from midi file
        MidiTimeline midiTimeline = new MidiTimeline("C:\\Users\\Tirafesi\\Documents\\Guild Wars 2\\Midi Files\\" + filepath);

        if (instrument == null) {
            instrument = defaultInstrument;
        }

        return instrument.canPlay(midiTimeline);
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
    private void loadConfigs() throws ParserConfigurationException, IOException, SAXException, NullPointerException, InvalidInstrumentException, MidiPathNotFoundException {

        // setup doc
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(CONFIGPATH));

        // get first occurrence only
        midiPath = doc.getDocumentElement().getElementsByTagName("midipath").item(0).getTextContent();
        defaultInstrument = InstrumentFactory.newInstrument(doc.getDocumentElement().getElementsByTagName("instrument").item(0).getTextContent());

        // make sure path has a trailing slash
        if (!midiPath.endsWith("/") && !midiPath.endsWith("\\")) {
            midiPath += "/";
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
}
