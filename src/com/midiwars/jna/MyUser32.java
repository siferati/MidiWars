package com.midiwars.jna;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.win32.W32APIOptions;

/** TODO MapVirtualKeyEx
 * Extension of JNA's user32.dll mapping.
 */
public interface MyUser32 extends User32 {

    MyUser32 INSTANCE = Native.loadLibrary("user32", MyUser32.class, W32APIOptions.DEFAULT_OPTIONS);

    /**
     * Translates (maps) a virtual-key code into a scan code or character value,
     * or translates a scan code into a virtual-key code.
     * To specify a handle to the keyboard layout to use for translating the specified code,
     * use the MapVirtualKeyEx function.
     *
     * @param uCode      The virtual key code or scan code for a key.
     *                  How this value is interpreted depends on the value of the uMapType parameter.
     * @param uMapType  The translation to be performed.
     *                  The value of this parameter depends on the value of the uCode parameter.
     *
     * @return          The return value is either a scan code, a virtual-key code, or a character value,
     *                  depending on the value of uCode and uMapType.
     *                  If there is no translation, the return value is zero.
     */
    UINT MapVirtualKey(UINT uCode, UINT uMapType);
}
