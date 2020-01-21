import javax.swing.*;
import java.awt.*;

public class MainClassClient extends JPanel {

    public static void main(String[] args) {
        JFrame window = new JFrame("Simple Calculator");
//        Register registerGUI = new Register();
        StartGUI startGUI = new StartGUI(window);
        window.setContentPane(startGUI);
        window.pack();
        window.setLocation(100,100);
        window.setPreferredSize(new Dimension(400, 300));
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);
    }
}