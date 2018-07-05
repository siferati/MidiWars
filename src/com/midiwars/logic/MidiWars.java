package com.midiwars.logic;

import com.midiwars.logic.instruments.Instrument;
import com.midiwars.logic.instruments.Instrument.*;
import com.midiwars.logic.instruments.InstrumentFactory;
import com.midiwars.logic.instruments.InstrumentFactory.*;
import com.midiwars.logic.midi.MidiTimeline;
import com.midiwars.ui.Chat;
import com.midiwars.util.MyRobot;
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

import static com.midiwars.logic.MidiWars.State.PAUSED;
import static com.midiwars.logic.MidiWars.State.PLAYING;
import static com.midiwars.logic.MidiWars.State.STOPPED;

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

    public enum State {

        /** Playback is active. */
        PLAYING,

        /** Playback is paused. */
        PAUSED,

        /** Playback is stopped. */
        STOPPED

    }

    /** Name of the window of the game. */
    public final static String GAME_WINDOW = "Guild Wars 2";

    /** Path to configurations file. */
    public static final String CONFIGPATH = "./config.xml";


    /* --- ATTRIBUTES --- */

    /** True when the app is playing a midi file. */
    private static volatile State state = STOPPED;

    /** JNA mapping of User32.dll functions. */
    private User32 user32;

    /** Path to where midi files are stored. */
    private String midiPath;

    /** Default instrument. */
    private Instrument defaultInstrument;

    /** The playlist that's currently playing. */
    private Playlist playlist;


    /* --- METHODS --- */

    /**
     * Default Constructor.
     */
    public MidiWars() throws IOException, SAXException, ParserConfigurationException, NullPointerException, InvalidInstrumentException, MidiPathNotFoundException {

        user32 = User32.INSTANCE;

        loadConfigs();
    }


    /** TODO quando playlist ja esta a tocar
     * TODO restaurar contents da clipboard
     * Plays the given midi file.
     *
     * @param instrument Instrument to play given file with.
     * @param filepath Path of midi file to play.
     * @param chat The in-game chat.
     *
     * @throws InvalidMidiDataException Midi file is invalid.
     * @throws IOException Can't open file.
     * @throws AWTException If the platform configuration does not allow low-level input control.
     */
    public void play(Instrument instrument, String filepath, Chat chat, boolean playlist) throws InvalidMidiDataException, IOException, AWTException, GameNotRunningException {

        if (!playlist) state = PLAYING;

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

        // construct timeline from midi file
        MidiTimeline midiTimeline = new MidiTimeline(midiPath + filepath);

        // default instrument
        if (instrument == null) {
            instrument = defaultInstrument;
        }

        // play
        MyRobot robot = new MyRobot(chat);
        instrument.play(midiTimeline, robot);

        if (!playlist) state = STOPPED;
    }


    /** TODO quando playlist ja esta a tocar
     * Plays the given playlist.
     *
     * @param instrument Instrument to play given playlist with.
     * @param filepath Path of playlist to play.
     * @param chat The in-game chat.
     *
     * @throws ParserConfigurationException If there was a configuration error within the parser.
     * @throws IOException If playlist file is missing.
     * @throws SAXException If couldn't parse playlist file.
     * @throws NullPointerException If playlist file doesn't have required format.
     */
    public void playlist(Instrument instrument, String filepath, Chat chat) throws ParserConfigurationException, IOException, SAXException, NullPointerException, MidifilesNotFoundException, AWTException, InvalidMidiDataException, GameNotRunningException, InterruptedException {

        state = PLAYING;

        // setup doc
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(midiPath + filepath));

        // get midifiles
        ArrayList<String> midifiles = new ArrayList<>();
        NodeList nodeList = doc.getDocumentElement().getElementsByTagName("midifile");
        for (int i = 0; i < nodeList.getLength(); i++) {
            midifiles.add(nodeList.item(i).getTextContent());
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
            File file = new File(midiPath + midifile);
            if (!file.exists() || file.isDirectory()) {
                throw new MidifilesNotFoundException();
            }
        }

        // play list
        playlist = new Playlist(midifiles, shuffle, repeat, instrument, chat);
        playlist.play(this);

        state = STOPPED;
    }


    /**
     * Getter.
     *
     * @return The current state of the app.
     */
    public static State getState() {
        return state;
    }


    /** TODO keys are still pressed down during pause - need to release them all
     * Pauses playback.
     */
    public void pause() {
        state = PAUSED;
    }


    /**
     * Resumes playback.
     */
    public void resume() {
        state = PLAYING;
    }


    /** TODO make this fully stop (ie making sure threads die) instead of current function
     * Stops playback.
     */
    public void stop() {
        state = STOPPED;
    }


    /**
     * Plays previous song.
     */
    public void prev() throws InterruptedException {
        state = STOPPED;
        if (playlist != null) {
            playlist.prev();
        }
        state = PLAYING;
    }


    /**
     * Plays the next song.
     */
    public void next() throws InterruptedException {
        state = STOPPED;
        if (playlist != null) {
            playlist.next();
        }
        state = PLAYING;
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
