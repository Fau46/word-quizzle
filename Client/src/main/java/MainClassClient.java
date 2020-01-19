import javax.swing.*;

public class MainClassClient extends JPanel {

    public static void main(String[] args) {
        JFrame window = new JFrame("Simple Calculator");
        Register registerGUI = new Register();
        window.setContentPane(registerGUI);
        window.pack();
        window.setLocation(100,100);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);
    }
}