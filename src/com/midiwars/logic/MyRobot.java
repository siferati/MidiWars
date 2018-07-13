package com.midiwars.logic;

import com.midiwars.jna.MyUser32;
import com.midiwars.ui.gci.Chat;
import com.midiwars.ui.UserInterface;
import com.midiwars.util.Pair;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

import static java.awt.event.KeyEvent.*;
import static java.awt.event.KeyEvent.VK_ALT;
import static java.awt.event.KeyEvent.VK_CONTROL;

/**
 * Synthesizes keyboard input events.
 */
public class MyRobot extends Robot {

    /** The in-game chat. */
    private Chat chat;

    /** DLL. */
    private final MyUser32 user32;


    /**
     * Creates a new MyRobot object.
     *
     * @throws AWTException If the platform configuration does not allow low-level input control.
     */
    public MyRobot() throws AWTException {
        super();
        user32 = MyUser32.INSTANCE;
        this.chat = Chat.getInstance();
    }


    /**
     * If the in-game chat is currently open,
     * closes it, presses the given key,
     * and re-opens it afterwards,
     * restoring its previous state.
     * Otherwise, just presses the given key.
     */
    @Override
    public synchronized void keyPress(int keycode) {
        if (!UserInterface.getInstance().isActive()) {
            return;
        }

        if (!chat.isOpen()) {
            super.keyPress(keycode);
            return;
        }

        // build string from kbd events
        Pair<String, Integer> built = chat.buildString();
        String str = built.first;
        int cursor = built.second;

        // block input and release modifiers
        user32.BlockInput(true);
        releaseModifiers();

        // close chat
        super.keyPress(VK_ESCAPE);
        super.keyRelease(VK_ESCAPE);

        // press key
        super.keyPress(keycode);

        // re-open chat
        super.keyPress(VK_ENTER);
        keyRelease(VK_ENTER);

        // otherwise key presses aren't detected
        delay(10);

        if (!str.equals("")) {

            // copy string to type to clipboard
            StringSelection selection = new StringSelection(str);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

            // prevent getting keyboard state while Ctrl and V are pressed.
            chat.setHoldGetKeyboardState(true);

            // paste string to chat
            super.keyPress(VK_CONTROL);
            super.keyPress(VK_V);

            // otherwise key presses aren't detected
            delay(10);

            // release pressed keys
            keyRelease(VK_V);
            keyRelease(VK_CONTROL);

            // allow getting keyboard state.
            chat.setHoldGetKeyboardState(false);

            // adjust cursor position
            for (int i = cursor; i < str.length(); i++) {
                super.keyPress(VK_LEFT);
                super.keyRelease(VK_LEFT);
            }
        }
        
        user32.BlockInput(false);
    }


    /**
     * Releases modifier keys.
     */
    private synchronized void releaseModifiers() {

        if(user32.GetAsyncKeyState(VK_SHIFT) < 0) {
            super.keyRelease(VK_SHIFT);
        }
        if(user32.GetAsyncKeyState(VK_CONTROL) < 0 && user32.GetAsyncKeyState(VK_ALT) < 0) {
            super.keyRelease(VK_ALT_GRAPH);
        }
        if(user32.GetAsyncKeyState(VK_CONTROL) < 0) {
            super.keyRelease(VK_CONTROL);
        }
        if(user32.GetAsyncKeyState(VK_ALT) < 0) {
            super.keyRelease(VK_ALT);
        }
    }
}
