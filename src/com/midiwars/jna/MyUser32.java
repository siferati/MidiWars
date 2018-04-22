package com.midiwars.jna;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.win32.W32APIOptions;

/** TODO ToAsciiEx
 * Extension of JNA's user32.dll mapping.
 */
public interface MyUser32 extends User32 {

    MyUser32 INSTANCE = Native.loadLibrary("user32", MyUser32.class, W32APIOptions.DEFAULT_OPTIONS);

    /**
     * Translates the specified virtual-key code and keyboard state to the corresponding character or characters.
     * The function translates the code using the input language
     * and physical keyboard layout identified by the keyboard layout handle.
     * To specify a handle to the keyboard layout to use to translate the specified code,
     * use the ToAsciiEx function.
     *
     * @param uVirtKey      The virtual-key code to be translated.
     * @param uScanCode     The hardware scan code of the key to be translated.
     *                      The high-order bit of this value is set if the key is up (not pressed).
     * @param lpKeyState    A pointer to a 256-byte array that contains the current keyboard state.
     *                      Each element (byte) in the array contains the state of one key.
     *                      If the high-order bit of a byte is set, the key is down (pressed).
     *                      The low bit, if set, indicates that the key is toggled on.
     *                      In this function, only the toggle bit of the CAPS LOCK key is relevant.
     *                      The toggle state of the NUM LOCK and SCROLL LOCK keys is ignored.
     * @param lpChar        The buffer that receives the translated character or characters.
     * @param uFlags        This parameter must be 1 if a menu is active, or 0 otherwise.
     *
     * @return              If the specified key is a dead key, the return value is negative.
     *                      Otherwise, it is one of the following values:
     *                      0 - The specified virtual key has no translation for the current state of the keyboard.
     *                      1 - One character was copied to the buffer.
     *                      2 - Two characters were copied to the buffer. This usually happens when a dead-key character
     *                      (accent or diacritic) stored in the keyboard layout cannot be composed with the specified
     *                      virtual key to form a single character.
     */
    int ToAscii(UINT uVirtKey, UINT uScanCode, byte[] lpKeyState, DWORDByReference lpChar, UINT uFlags);
}
