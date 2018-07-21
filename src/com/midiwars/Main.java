package com.midiwars;

import static javax.swing.JOptionPane.PLAIN_MESSAGE;

import com.midiwars.jna.MyUser32;
import com.midiwars.ui.cli.CLI;
import com.midiwars.ui.gci.GCI;
import org.json.JSONObject;

import javax.swing.*;
import java.io.*;
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

        // figure out what UI to use
        boolean gci = (args.length == 1 && args[0].equals("-windows") && System.getProperty("os.name").startsWith("Windows"));

        if (gci) {

            // isLatestVersion() takes a while to execute,
            // so there's a need to block input - preventing the user from swapping windows
            // during that time. SetForegroundWindow() wouldn't work otherwise.
            MyUser32.INSTANCE.BlockInput(true);

            // redirect output to log file, since user won't be able to see the console
            // due to this being called through elevate.cmd
            try {
                PrintStream out = new PrintStream(new FileOutputStream("log.txt"));
                System.setOut(out);
            }
            catch (FileNotFoundException e) {
                System.out.println("Critical Error: Couldn't create the log file.");
                return;
            }
        }

        boolean isLatestVersion = isLatestVersion();

        // unblock input
        if (gci) MyUser32.INSTANCE.BlockInput(false);

        if(!isLatestVersion) {

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

        // launch UI
        if (gci) new GCI();
        else new CLI().parse(args);
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
