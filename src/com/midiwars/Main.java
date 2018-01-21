package com.midiwars;

import com.midiwars.logic.MidiWars;

import javax.sound.midi.InvalidMidiDataException;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {

        System.out.println("Starting");

        try {
            new MidiWars().readMidiFile();
        }

        catch (Exception e) {}
    }
}
