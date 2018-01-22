package com.midiwars.logic;

import com.midiwars.logic.instruments.Instrument;
import com.midiwars.logic.midi.Note;

import java.util.EnumMap;

import static com.midiwars.logic.midi.Note.Name.*;
import static java.awt.event.KeyEvent.*;

/**
 * This class maps Notes to keybinds
 */
public class Keymap {

    /* --- DEFINES --- */

    public static int C_KEYBIND = VK_NUMPAD1;
    public static int D_KEYBIND = VK_NUMPAD2;
    public static int E_KEYBIND = VK_NUMPAD3;
    public static int F_KEYBIND = VK_NUMPAD4;
    public static int G_KEYBIND = VK_NUMPAD5;
    public static int A_KEYBIND = VK_NUMPAD6;
    public static int B_KEYBIND = VK_NUMPAD7;
    public static int OCTAVEUP_KEYBIND = VK_NUMPAD9;
    public static int OCTAVEDOWN_KEYBIND = VK_NUMPAD0;

    /* --- ATTRIBUTES --- */

    /** Maps note names to keybinds */
    private EnumMap<Note.Name, Integer> notemap;

    /** Maps octave operations to keybinds */
    private EnumMap<Instrument.OctaveOp, Integer> octaveopmap;


    /* --- METHODS --- */

    /**
     * Default Constructor.
     */
    public Keymap() {

        notemap = new EnumMap<>(Note.Name.class);
        octaveopmap = new EnumMap<>(Instrument.OctaveOp.class);

        // game doesn't allow for sharps nor flats
        notemap.put(C, C_KEYBIND);
        notemap.put(D, D_KEYBIND);
        notemap.put(E, E_KEYBIND);
        notemap.put(F, F_KEYBIND);
        notemap.put(G, G_KEYBIND);
        notemap.put(A, A_KEYBIND);
        notemap.put(B, B_KEYBIND);

        octaveopmap.put(Instrument.OctaveOp.UP, OCTAVEUP_KEYBIND);
        octaveopmap.put(Instrument.OctaveOp.DOWN, OCTAVEDOWN_KEYBIND);
    }


    /**
     * Returns a note's keybind.
     *
     * @param name Name of the note.
     *
     * @return Keybind.
     */
    public int noteToKeybind(Note.Name name) {

        return notemap.get(name);
    }


    /**
     * Returns an octave operation's keybind.
     *
     * @param octaveOp Operation.
     *
     * @return Keybind.
     */
    public int octaveOpToKeybind(Instrument.OctaveOp octaveOp) {

        return octaveopmap.get(octaveOp);
    }
}
