package com.midiwars;

import static javax.swing.JOptionPane.PLAIN_MESSAGE;

import com.midiwars.ui.cli.CLI;
import com.midiwars.ui.gci.GCI;
import org.json.JSONObject;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Entry point for the application.
 */
public class Main {

    /** The url used to request the latest version of the app. */
    public static final String LATEST_VERSION_URL = "https://api.github.com/repos/tirafesi/MidiWars/releases/latest";


    /**
     * Entry point for the application.
     *
     * @param args List of arguments.
     */
    public static void main(String[] args) {

        if(!isLatestVersion()) {

            // make the dialog box prettier
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                System.out.println("Error: Couldn't change the style of the dialog box.");
            }

            String html =
                    "<html>" +
                            "<body>" +
                            "<div style='text-align: center;'>" +
                            "<p>Please download the latest version from</p>" +
                            "<p>https://github.com/tirafesi/MidiWars/releases</p>" +
                            "</div>" +
                            "</body>" +
                            "</html>";

            // show message
            JOptionPane.showMessageDialog(null, html, "Midi Wars Update", PLAIN_MESSAGE);
        }


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


    /**
     * Checks if this is the latest version of the app.
     *
     * @return True if this is the latest version of the app,
     *         False otherwise.
     */
    private static boolean isLatestVersion() {

        // inits
        HttpURLConnection conn = null;

        try {

            // set the connection
            URL url = new URL(LATEST_VERSION_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            // read status code
            if (conn.getResponseCode() != 200) {
                throw new IOException();
            }

            // read response
            BufferedReader reader = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            StringBuilder strBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                strBuilder.append(line);
            }

            // get latest version
            JSONObject json = new JSONObject(strBuilder.toString());
            String latestVersion = json.getString("tag_name");
            latestVersion = latestVersion.substring(1, latestVersion.length());

            // get local version
            String localVersion = Main.class.getPackage().getSpecificationVersion();
            if (localVersion == null) localVersion = "";

            return localVersion.equals(latestVersion);

        } catch (IOException e) {
            System.out.println("Error: Couldn't fetch the latest release information.");
        } finally {
            if (conn != null) conn.disconnect();
        }

        return true;
    }
}
