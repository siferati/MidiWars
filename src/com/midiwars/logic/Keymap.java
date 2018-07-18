package com.midiwars.logic;

import static java.awt.event.KeyEvent.*;

/**
 * Maps key bar slots to keybinds.
 */
public abstract class Keymap {

    /* --- DEFINES --- */

    /** Keybind for moving an octave up. */
    public static int OCTAVEUP_KEYBIND = VK_NUMPAD9;

    /** Keybind for moving an octave down. */
    public static int OCTAVEDOWN_KEYBIND = VK_NUMPAD0;

    /** Map key bar slots to keybinds (except octave up and down). */
    public static final int[] KEYBINDS = {
            VK_NUMPAD1,
            VK_NUMPAD2,
            VK_NUMPAD3,
            VK_NUMPAD4,
            VK_NUMPAD5,
            VK_NUMPAD6,
            VK_NUMPAD7,
            VK_NUMPAD8
    };
}
