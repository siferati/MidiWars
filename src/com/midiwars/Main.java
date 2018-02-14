package com.midiwars;

import com.midiwars.ui.CLI;
import com.midiwars.ui.GCI;
import com.midiwars.ui.Parser;

/**
 * Entry point for the application.
 */
public class Main {

    /**
     * Entry point for the application.
     *
     * @param args List of arguments.
     */
    public static void main(String[] args) {

        // figure out what UI to use
        if (args.length == 0) {
            System.out.println("debug: Launching In-Game Chat Interface!");
            new GCI();
        }
        else {
            System.out.println("debug: Launching Command Line Interface!");
            Parser parser = new Parser(new CLI());
            parser.parse(args);
        }
    }
}
