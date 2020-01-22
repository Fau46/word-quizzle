import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class HomePage extends JPanel implements ActionListener{
    private JFrame window;

    public HomePage(String nick, JFrame window) {
        this.window = window;

        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        JPanel welcomePanel = new JPanel();
        welcomePanel.add(new JLabel("BENVENUTO IN WORD QUIZZLE"));

        JPanel nickPanel = new JPanel();
        nickPanel.add(new JLabel(nick));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1,4));

        JButton logout = new JButton("LOGOUT");

        logout.addActionListener(this);

        buttonPanel.add(logout);

        setLayout(new GridLayout(4,1,3,3));
        add(welcomePanel);
        add(nickPanel);
        add(buttonPanel);


    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {

    }
}
