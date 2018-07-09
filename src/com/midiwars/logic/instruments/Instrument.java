package com.midiwars.logic.instruments;

import com.midiwars.logic.Keymap;
import com.midiwars.logic.Player;
import com.midiwars.logic.midi.MidiTimeline;
import com.midiwars.logic.midi.NoteEvent;
import com.midiwars.util.MyRobot;

import java.awt.*;
import java.util.ArrayList;

import static com.midiwars.logic.Player.State.PAUSED;
import static com.midiwars.logic.Player.State.STOPPED;
import static com.midiwars.logic.instruments.Instrument.Warning.*;
import static javax.sound.midi.ShortMessage.NOTE_ON;

/**
 * Represents a musical instrument.
 */
public abstract class Instrument {

    /* --- DEFINES --- */

    /** Possible warnings when trying to play a midi timeline. */
    public enum Warning {

        /** Some notes can't be played by this instrument. */
        NOT_IN_RANGE,

        /** Tempo is too fast, playback will probably be hindered. */
        TEMPO_TOO_FAST,

        /** Some notes last for too long, so they'll probably be played twice. */
        NOTES_TOO_LONG,

        /** Some pauses last for too long, probably an error in the midi file. */
        PAUSES_TOO_LONG
    }


    /** Amount of time robot sleeps after a key bar change (ms). */
    public static final int ROBOT_SLEEP = 50;

    /** Minimum amount of time needed in-between key bar changes (ms). */
    public static final int KEYBAR_COOLDOWN = 200;

    /** Upper limit to a note's duration (ms) - note will be played twice if duration is higher than this value. */
    public static final int NOTE_DURATION_LIMIT = 2250;

    /** Upper limit to a pause's duration (ms) - longer pauses are probably due to errors in the midi file. */
    public static final int PAUSE_DURATION_LIMIT = 5000;

    /** Maximum amount of time (ms) Robot.delay() can sleep for. */
    public static final int ROBOT_MAX_SLEEP = 60000;


    /* --- ATTRIBUTES --- */

    /** Name of the instrument. */
    private final String name;

    /** True if the instrument can hold notes (ie note duration matters). */
    private final boolean canHold;

    /** Each line represents a key bar (in-game skill bar - usually an octave) and its slots. */
    private final int[][] keybars;

    /** The active key bar before and after the midi timeline is played. */
    private final int idleKeybarIndex;

    /** The currently active key bar. */
    private int activeKeybarIndex;

    /** Robot used to simulate system inputs. */
    private MyRobot robot;

    /** System time of the previous key bar change (ms). */
    private long previousKeybarChange;

    /** The keyboard key that is currently being held down. */
    private int heldKeybind;


    /* --- METHODS --- */

    /**
     * Constructor.
     *
     * @param name Name of the instrument.
     * @param canHold If the instrument can hold notes.
     * @param keybars Each line represents a skill bar (usually an octave).
     * @param idleKeybarIndex The active key bar before and after the midi timeline is played.
     */
    public Instrument(String name, boolean canHold, int [][] keybars, int idleKeybarIndex) {

        this.name = name;
        this.canHold = canHold;
        this.keybars = keybars;
        this.idleKeybarIndex = idleKeybarIndex;
        activeKeybarIndex = idleKeybarIndex;
        robot = null;
        previousKeybarChange = -1;
        heldKeybind = -1;
    }


    /**
     * Calculates the time left to complete delta.
     *
     * @param startTime Time countdown started.
     * @param delta Total amount of time to complete.
     *
     * @return The amount of time left to complete delta.
     */
    private int timeLeft(long startTime, int delta) {
        return delta - (int) (System.currentTimeMillis() - startTime);
    }


    /**
     * Plays the given midi timeline.
     *
     * @param midiTimeline Midi Timeline.
     * @param startNote Note (index) to start playback from.
     *
     * @return -1 If playback finished, next note (index) to play otherwise (ie playback was paused or stopped).
     */
    public int play(MidiTimeline midiTimeline, int startNote) throws AWTException {

        robot = new MyRobot();
        Player player = Player.getInstance();

        ArrayList<NoteEvent> timeline = midiTimeline.getTimeline();

        int i = startNote;
        for (; i < timeline.size(); i++) {

            // stop / pause playback
            if (player.getState() == PAUSED || player.getState() == STOPPED) {

                // release key
                if (heldKeybind > -1) {
                    robot.keyRelease(heldKeybind);
                    heldKeybind = -1;
                }

                // return to idle keybar
                returnToIdleKeybar();

                if (player.getState() == PAUSED) return i;
                if (player.getState() == STOPPED) return 0;
            }

            // store start time for this event's processing
            long startTime = System.currentTimeMillis();

            NoteEvent noteEvent = timeline.get(i);
            int keybarIndex = getKeybarIndex(noteEvent.getKey());

            // ignore if note can't be played
            if (keybarIndex < 0) {
                continue;
            }

            // amount to sleep until next noteEvent
            final int deltaTime;
            if (i < timeline.size() - 1) {
                deltaTime = timeline.get(i+1).getTimestamp() - noteEvent.getTimestamp();
            } else {
                deltaTime = 0;
            }

            int keybind;

            // case NOTE_ON
            if (noteEvent.getType() == NOTE_ON) {

                // if there's a key being held down
                if (heldKeybind > -1) {
                    robot.keyRelease(heldKeybind);
                }

                // change keybars if needed
                changeKeybars(keybarIndex);

                // note keybind
                keybind = Keymap.KEYBINDS[getKeyIndex(noteEvent.getKey())];

                // play note
                robot.keyPress(keybind);
                heldKeybind = keybind;

                // if there's time, look into the future and preemptively change key bars if needed
                if (timeLeft(startTime, deltaTime) > 0 && !canHold) {
                    preemptivelyChangeKeybars(i, timeline);
                }
            }

            // case NOTE_OFF
            else {
                keybind = Keymap.KEYBINDS[getKeyIndex(noteEvent.getKey())];
                if (keybind == heldKeybind) {
                    robot.keyRelease(keybind);
                    heldKeybind = -1;

                    // canHold instruments can only preemptively change key bars if there's no held key atm
                    // (ie if a key was just released)
                    if (timeLeft(startTime, deltaTime) > 0 && canHold) {
                        preemptivelyChangeKeybars(i, timeline);
                    }
                }
            }

            // sleep until next event
            int delay;
            if ((delay = timeLeft(startTime, deltaTime)) > 0) {

                // robot.delay() doesn't handle higher values
                // and I don't want to Thread.sleep()
                if (delay > ROBOT_MAX_SLEEP) {
                    delay = ROBOT_MAX_SLEEP;
                }

                robot.delay(delay);
            }
        }

        // return to idle keybar
        returnToIdleKeybar();

        return -1;
    }


    /**
     * Returns the instrument to the idle keybar.
     */
    public void returnToIdleKeybar() {
        changeKeybars(idleKeybarIndex);
    }


    /**
     * Looks into the future (starting from given position) to preemptively change keybars if needed.
     *
     * @param i Index of starting position in the timeline.
     * @param timeline Midi Timeline.
     */
    public void preemptivelyChangeKeybars(int i, ArrayList<NoteEvent> timeline) {

        for (int j = i; j < timeline.size() - 1; j++) {

            // get next note event
            NoteEvent nextNoteEvent = timeline.get(j+1);

            // only interested in NOTE_ON events
            if (nextNoteEvent.getType() == NOTE_ON) {

                int nextKeybarIndex = getKeybarIndex(nextNoteEvent.getKey());

                // make sure key to play is within instrument's range
                if (nextKeybarIndex >= 0) {

                    // change keybars if needed
                    changeKeybars(nextKeybarIndex);
                    break;
                }
            }
        }
    }


    /**
     * Changes the active keybar to the given one.
     *
     * @param keybarIndex New active keybar.
     */
    private void changeKeybars(int keybarIndex) {

        // how many key bars are necessary to change
        int deltaKeybarIndex = keybarIndex - activeKeybarIndex;

        int keybind;
        for (int j = 0; j < Math.abs(deltaKeybarIndex); j++) {

            // amount of time needed to wait before changing key bar (ms)
            int wait = 0;

            // check if it's not the first time
            if (previousKeybarChange > -1) {

                // how much time passed since the previous key bar change (ms)
                int deltaKeybarChange = (int) (System.currentTimeMillis() - previousKeybarChange);

                // check if it needs to wait before changing key bar
                if (deltaKeybarChange < KEYBAR_COOLDOWN) {
                    wait = KEYBAR_COOLDOWN - deltaKeybarChange;
                }

                robot.delay(wait);
            }

            // decide key bar change direction
            if (deltaKeybarIndex > 0) {
                keybind = Keymap.OCTAVEUP_KEYBIND;
            } else {
                keybind = Keymap.OCTAVEDOWN_KEYBIND;
            }

            // if there's a key being held down
            if (heldKeybind > -1) {
                robot.keyRelease(heldKeybind);
                // key bar changes immediately release key
                heldKeybind = -1;
            }

            // change keybar
            robot.keyPress(keybind);

            // update previous key bar change time
            previousKeybarChange = System.currentTimeMillis();

            // needed for key bar change to take effect
            robot.delay(ROBOT_SLEEP);

            // called after delay() to provide small visual feedback in-game
            robot.keyRelease(keybind);
        }

        // update active keybar index
        activeKeybarIndex = keybarIndex;
    }


    /**
     * Returns the index of the keybar given key (note) belongs to,
     * taking into account active key bar, in order to avoid
     * unnecessary keybar (octave) changes.
     *
     * @param key Key to assess.
     *
     * @return Key bar index. -1 if note can't be played.
     */
    private int getKeybarIndex(int key) {

        // check active key bar first,
        // in order to avoid unnecessary changes
        for (int k : keybars[activeKeybarIndex]) {
            if (k == key) {
                return activeKeybarIndex;
            }
        }

        // other key bars
        for (int i = 0; i < keybars.length; i++) {
            // this was already tested above
            if (i == activeKeybarIndex) {
                continue;
            }

            for (int k : keybars[i]) {
                if (k == key) {
                    return i;
                }
            }
        }

        // can't play this note
        return -1;
    }


    /**
     * Returns the index of given key (note) in a keybar.
     *
     * @param key Key to assess.
     *
     * @return Key index. -1 if note can't be played.
     */
    private int getKeyIndex(int key) {

        // check active key bar first,
        // since its the most common occurrence
        for (int i = 0; i < keybars[activeKeybarIndex].length; i++) {
            if (key == keybars[activeKeybarIndex][i]) {
                return i;
            }
        }

        // other key bars
        for (int i = 0; i < keybars.length; i++) {
            // this was already tested above
            if (i == activeKeybarIndex) {
                continue;
            }

            for (int j = 0; j < keybars[i].length; j++) {
                if (keybars[i][j] == key) {
                    return j;
                }
            }
        }

        // can't play this note
        return -1;
    }


    /**
     * Checks if the given midi timeline can be properly played by this instrument
     * (ie no warnings pop up).
     *
     * @param midiTimeline Timeline to assess.
     *
     * @return List of warnings caused by given timeline.
     */
    public ArrayList<Warning> canPlay(MidiTimeline midiTimeline) {

        ArrayList<Warning> warnings = new ArrayList<>();

        ArrayList<NoteEvent> timeline = midiTimeline.getTimeline();

        // control
        boolean isInRange = true;
        boolean isTooFast = false;
        boolean areNotesTooLong = false;
        boolean arePausesTooLong = false;

        // reset
        previousKeybarChange = -1;
        activeKeybarIndex = idleKeybarIndex;

        NoteEvent previousNoteEvent = null;
        NoteEvent previousNoteOnEvent = null;

        for (NoteEvent noteEvent: timeline) {

            if (isInRange && !isInRange(noteEvent)) {
                warnings.add(NOT_IN_RANGE);
                isInRange = false;
            }

            if (!isTooFast && isTooFast(previousNoteOnEvent, noteEvent)) {
                warnings.add(TEMPO_TOO_FAST);
                isTooFast = true;
            }

            if (!areNotesTooLong && areNotesTooLong(noteEvent)) {
                warnings.add(NOTES_TOO_LONG);
                areNotesTooLong = true;
            }

            if (!arePausesTooLong && arePausesTooLong(previousNoteEvent, noteEvent)) {
                warnings.add(PAUSES_TOO_LONG);
                arePausesTooLong = true;
            }

            // prepare nex ite
            previousNoteEvent = noteEvent;
            if (noteEvent.getType() == NOTE_ON) {
                previousNoteOnEvent = noteEvent;
            }

        }

        // reset
        previousKeybarChange = -1;
        activeKeybarIndex = idleKeybarIndex;

        return warnings;
    }


    /**
     * Checks if the given note doesn't require key bar changes that are too fast,
     * thus hindering playback.
     *
     * @param previousNoteOnEvent Previous NOTE_ON event assessed.
     * @param noteEvent Current note event being assessed.
     *
     * @return True if note will hinder playback, False otherwise.
     */
    private boolean isTooFast(NoteEvent previousNoteOnEvent, NoteEvent noteEvent) {

        int keybarIndex = getKeybarIndex(noteEvent.getKey());

        // ignore if note can't be played
        if (keybarIndex < 0 || noteEvent.getType() != NOTE_ON) {
            return false;
        }

        // how many key bars are necessary to change
        int deltaKeybarIndex = keybarIndex - activeKeybarIndex;

        // if there's a need to change key bar
        if (deltaKeybarIndex != 0) {

            // if it's not the first time
            if (previousKeybarChange > -1) {

                // how much time passed since the previous key bar change (ms)
                int deltaKeybarChange = (int) (noteEvent.getTimestamp() - previousKeybarChange);

                // ---------------------------------------------------------------
                // these IFs could be under a single statement using OR operator,
                // but it's already hard enough to read as it is, so...
                // ---------------------------------------------------------------

                boolean exit = false;

                // change is too fast, key bar change cooldown hasn't passed yet
                if (deltaKeybarChange < Math.abs(deltaKeybarIndex) * KEYBAR_COOLDOWN) {
                    exit = true;
                }

                // change is too fast, robot.delay() after octave change will affect note playtime
                else if (previousNoteOnEvent != null && noteEvent.getTimestamp() != previousNoteOnEvent.getTimestamp()) {

                    // instrument can't hold notes
                    if ((!canHold &&
                            (noteEvent.getTimestamp() < previousNoteOnEvent.getTimestamp() + ROBOT_SLEEP))) {
                        exit = true;
                    }

                    // instrument can hold notes
                    else if ((canHold &&
                            (noteEvent.getTimestamp() < previousNoteOnEvent.getTimestamp() + previousNoteOnEvent.getDuration() + ROBOT_SLEEP))) {
                        exit = true;
                    }
                }

                // can't properly play this midi timeline
                if (exit) {
                    return true;
                }
            }

            if (!canHold && previousNoteOnEvent != null) {
                previousKeybarChange = previousNoteOnEvent.getTimestamp();
            }
            if (canHold && previousNoteOnEvent != null) {
                previousKeybarChange = previousNoteOnEvent.getTimestamp() + previousNoteOnEvent.getDuration();
            }

            activeKeybarIndex = keybarIndex;
        }

        // can play this midi timeline
        return false;
    }


    /**
     * Checks if given note can be played by this instrument.
     *
     * @param noteEvent Note event to assess.
     *
     * @return True if note is in range, False otherwise.
     */
    private boolean isInRange(NoteEvent noteEvent) {

        for (int[] keybar : keybars) {
            for (int key : keybar) {
                if (key == noteEvent.getKey()) {
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * Checks if the given note goes over the duration limit.
     *
     * @param noteEvent Note event to assess.
     *
     * @return True if note is above the limit, False otherwise.
     */
    private boolean areNotesTooLong(NoteEvent noteEvent) {

        return (noteEvent.getDuration() > NOTE_DURATION_LIMIT);
    }


    /**
     * Checks if there are pauses (time between events) that go over the duration limit.
     *
     * @param previousNoteEvent Previous note event assessed.
     * @param noteEvent Current note event being assessed.
     *
     * @return True if pause is above the limit, False otherwise.
     */
    private boolean arePausesTooLong(NoteEvent previousNoteEvent, NoteEvent noteEvent) {

        return (previousNoteEvent != null && noteEvent.getTimestamp() - previousNoteEvent.getTimestamp() > PAUSE_DURATION_LIMIT);
    }
}
