import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MainClassClient extends JPanel implements ActionListener {

    private JTextField nickInput, pwdInput;
    private JLabel answer;

    public static void main(String[] args) {
        JFrame window = new JFrame("Simple Calculator");
        MainClassClient content = new MainClassClient();
        window.setContentPane(content);
        window.pack();
        window.setLocation(100,100);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);

    }

    public MainClassClient(){
        setBackground(Color.GRAY);
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        nickInput = new JTextField("",10);
        nickInput.setBackground(Color.WHITE);
        pwdInput = new JTextField("",10);

        JPanel nickPanel = new JPanel();
        nickPanel.add(new JLabel("Nickname = "));
        nickPanel.add(nickInput);
        JPanel pwdPanel = new JPanel();
        pwdPanel.add(new JLabel("Password = "));
        pwdPanel.add(pwdInput);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1,4));

        JButton go = new JButton("GO");

        go.addActionListener(this);

        buttonPanel.add(go);

        answer = new JLabel("", JLabel.CENTER);
        answer.setForeground(Color.BLACK);
        answer.setBackground(Color.WHITE);
        answer.setOpaque(true);

        setLayout(new GridLayout(4,1,3,3));
        add(nickPanel);
        add(pwdPanel);
        add(buttonPanel);
        add(answer);


    }
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        String nick, pwd;
        nick = nickInput.getText();
        pwd = pwdInput.getText();

        try{

            Registry registry = LocateRegistry.getRegistry(RMIRegistrationInterface.PORT);
            RMIRegistrationInterface serverObject = (RMIRegistrationInterface) registry.lookup(RMIRegistrationInterface.REMOTE_OBJECT_NAME);

            answer.setText(""+serverObject.Registration(nick,pwd));
        } catch (Exception e) {
            answer.setText("Errore col server");
            e.printStackTrace();
        }
    }
}