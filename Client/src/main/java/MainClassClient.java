import javax.swing.*;
import java.awt.*;

public class MainClassClient extends JPanel {

    public static void main(String[] args) {
        JFrame window = new JFrame("Word Quizzle");
        StartGUI startGUI = new StartGUI(window);

        window.setContentPane(startGUI);
        window.pack();
        window.setLocation(550,200);
        window.setSize(600,600);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);
    }
}