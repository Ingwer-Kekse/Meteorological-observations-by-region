import app.ConsoleApp;
import app.WeatherFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && "--gui".equals(args[0])) {
            SwingUtilities.invokeLater(() -> new WeatherFrame().setVisible(true));
        } else {
            ConsoleApp.run();
        }
    }
}