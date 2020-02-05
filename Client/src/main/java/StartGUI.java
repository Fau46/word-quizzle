import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StartGUI extends JPanel implements ActionListener {

    private JFrame window;

    public StartGUI(JFrame window){
        this.window = window;

        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        JPanel buttonPanel = new JPanel();

        JButton register = new JButton("Iscriviti");
        JButton login = new JButton("Login");

        register.addActionListener(this);
        login.addActionListener(this);

        buttonPanel.add(register);
        buttonPanel.add(login);

        setLayout(new GridLayout(1,1));
        add(buttonPanel);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals("Iscriviti")){
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
