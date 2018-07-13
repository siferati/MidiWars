package com.midiwars;

import com.midiwars.ui.cli.CLI;
import com.midiwars.ui.gci.GCI;

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
        if (args.length == 0 && System.getProperty("os.name").startsWith("Windows")) {
            System.out.println("debug: Launching In-Game Chat Interface!");
            new GCI();
        }
        else {
            System.out.println("debug: Launching Command Line Interface!");
            new CLI().parse(args);
        }
    }
}
