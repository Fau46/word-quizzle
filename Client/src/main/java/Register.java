import Costanti.Costanti;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Register extends JPanel implements ActionListener,Costanti {
    private JLabel answer;
    private JFrame window;
    private JTextField nickInput, pwdInput;

    public Register(JFrame window){
        this.window = window;

        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        JPanel headPanel = new JPanel();
        headPanel.setBackground(Color.WHITE);
        headPanel.setLayout(new BoxLayout(headPanel, BoxLayout.LINE_AXIS));

        JButton back = new JButton("BACK");

        back.setBackground(Color.WHITE);
        back.setIcon(new ImageIcon(new ImageIcon(IMAGEPATH+"back.png").getImage().getScaledInstance(30, 30, Image.SCALE_DEFAULT)));
        back.setBorder(BorderFactory.createEmptyBorder());

        back.addActionListener(this);

        JLabel title = new JLabel("REGISTRAZIONE");

        headPanel.add(back);
        headPanel.add(Box.createHorizontalStrut(200));
        headPanel.add(title);

        nickInput = new JTextField("",10);
        pwdInput = new JTextField("",10);

        JPanel nickPanel = new JPanel();
        nickPanel.setBackground(Color.WHITE);
        nickPanel.add(new JLabel("Nickname: "));
        nickPanel.add(nickInput);

        JPanel pwdPanel = new JPanel();
        pwdPanel.setBackground(Color.WHITE);
        pwdPanel.add(new JLabel("Password: "));
        pwdPanel.add(pwdInput);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.WHITE);

        JButton go = new JButton("ISCRIVITI");

        go.setPreferredSize(new Dimension(BUTTONWIDTH,BUTTONHEIGHT));

        go.addActionListener(this);

        buttonPanel.add(go);

        answer = new JLabel("", JLabel.CENTER);
        answer.setForeground(Color.BLACK);
        answer.setBackground(Color.WHITE);
        answer.setOpaque(true);

        setLayout(new GridLayout(5,1,3,3));
        add(headPanel);
        add(nickPanel);
        add(pwdPanel);
        add(buttonPanel);
        add(answer);

    }

    public void actionPerformed(ActionEvent actionEvent) {

        if(actionEvent.getActionCommand().equals("BACK")){
            StartGUI startGUI = new StartGUI(window);
            window.setContentPane(startGUI);
            window.validate();
        }
        else{

            String nick, pwd;
            nick = nickInput.getText();
            pwd = pwdInput.getText();

            //Controllo che i campi di input non siano vuoti
            if(nick.equals("")) {
                answer.setText("Inserire un nickname");
                return;
            }
            if(pwd.equals("")){
                answer.setText("Inserire una password");
                return;
            }

            try{

                Registry registry = LocateRegistry.getRegistry(RMIRegistrationInterface.PORT);
                RMIRegistrationInterface serverObject = (RMIRegistrationInterface) registry.lookup(RMIRegistrationInterface.REMOTE_OBJECT_NAME);

                String message = null;
                int response = serverObject.registra_utente(nick,pwd); //Effettuo la registrazione

                switch(response){ //Leggo la risposta
                    case -101: message = "Nickname non valido";
                        break;
                    case -102: message = "Nickname già esistente";
                        break;
                    case -103: message = "Password non valida";
                        break;
                    case -104: message = "Password troppo corta. Inserire una password di almeno 5 caratteri";
                        break;
                    case -105: message = "Password troppo lunga. Inserire una password di massimo 20 caratteri";
                        break;
                    case -106: message = "Il nickname non può contenere spazi";
                        break;
                    case -1: message = "Errore generico";
                        break;
                    case 1: message = "Ok";
                        break;
                }

                answer.setText(message);

                if(response>0){ //Se la registrazione è andata a buon fine mostro la schermata di login
                    Login login = new Login(window);
                    window.setContentPane(login);
                    window.validate();
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Errore durante la registrazione");
                answer.setText("Errore col server");
                e.printStackTrace();
            }
        }
    }
}
