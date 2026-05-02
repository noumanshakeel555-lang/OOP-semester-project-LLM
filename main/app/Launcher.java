package main.app;

import javax.swing.SwingUtilities;
import main.db.DatabaseManager;
import main.gui.LoginFrame;
import main.gui.UITheme;

public class Launcher {

    public static void main(String[] args) {

        // 1. Initialise SQLite database (creates quizpro.db if it doesn't exist)
        DatabaseManager.initialize();

        // 2. Close DB connection cleanly when JVM exits (any path)
        Runtime.getRuntime().addShutdownHook(new Thread(DatabaseManager::close));

        // 3. Apply global Swing theme tweaks
        UITheme.apply();

        // 4. Open login window on the Event Dispatch Thread
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}
