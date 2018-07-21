package com.launcher;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Main {

    /**
     * Entry point for the application.
     *
     * @param args List of arguments.
     */
    public static void main(String[] args) {

        // redirect output to log file, since user won't be able to see the console
        // if they double click the .jar
        try {
            PrintStream out = new PrintStream(new FileOutputStream("log.txt"));
            System.setOut(out);
        }
        catch (FileNotFoundException e) {
            System.out.println("Critical Error: Couldn't create the log file.");
            return;
        }

        // this is a windows-only launcher
        if (!System.getProperty("os.name").startsWith("Windows")) {
            System.out.println("Critical Error: this can only be run on windows.");
            return;
        }

        try {

            // create configs folder
            File dir = new File("core");
            if (!dir.isDirectory()) {
                if (!dir.mkdirs()) {
                    throw new IOException();
                }
            }

            // extract needed files
            InputStream is = Main.class.getClassLoader().getResourceAsStream("core.jar");
            Files.copy(is, Paths.get("core/core.jar"), REPLACE_EXISTING);
            is.close();


            is = Main.class.getClassLoader().getResourceAsStream("elevate.cmd");
            Files.copy(is, Paths.get("core/elevate.cmd"), REPLACE_EXISTING);
            is.close();


            is = Main.class.getClassLoader().getResourceAsStream("elevate.vbs");
            Files.copy(is, Paths.get("core/elevate.vbs"), REPLACE_EXISTING);
            is.close();

            // execute app as admin
            Runtime.getRuntime().exec(("core/elevate.cmd javaw -jar core/core.jar -windows"));
        }
        catch (IOException e) {
            System.out.println("Critical Error: Couldn't launch the app.");
        }
    }
}
