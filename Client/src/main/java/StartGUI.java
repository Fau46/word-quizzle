import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import Costanti.*;

public class StartGUI extends JPanel implements ActionListener,Costanti {

    private JFrame window;

    public StartGUI(JFrame window){
        this.window = window;

        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(Color.WHITE);

        JLabel title = new JLabel();

        ImageIcon imageIcon = new ImageIcon(new ImageIcon(IMAGEPATH+"logo.png").getImage().getScaledInstance(350, 170, Image.SCALE_DEFAULT));

        title.setIcon(imageIcon);
        title.setBackground(Color.WHITE);

        titlePanel.add(title);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.WHITE);

        JButton register = new JButton("ISCRIVITI");
        JButton login = new JButton("LOGIN");

        register.addActionListener(this);
        login.addActionListener(this);

        register.setPreferredSize(new Dimension(BUTTONWIDTH,BUTTONHEIGHT));
        login.setPreferredSize(new Dimension(BUTTONWIDTH,BUTTONHEIGHT));

        buttonPanel.add(register);
        buttonPanel.add(login);

        setLayout(new GridLayout(2,1));
        add(titlePanel);
        add(buttonPanel);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals("ISCRIVITI")){
            Register registerGUI = new Register(window);
            window.setContentPane(registerGUI);
            window.validate();
        }
        else{
            Login loginGUI = new Login(window);
            window.setContentPane(loginGUI);
            window.validate();
        }
    }
}
