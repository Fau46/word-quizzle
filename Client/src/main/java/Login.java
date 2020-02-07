import Costanti.Costanti;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Login extends JPanel implements ActionListener,Costanti{
    private JFrame window;

    private SocketChannel client;
    private JButton connectButton;
    private JLabel answer,connectAnswer;
    private JTextField nickInput, pwdInput;


    public Login(JFrame window){
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

        JLabel title = new JLabel("LOGIN");


        connectAnswer = new JLabel("", JLabel.CENTER);
        connectAnswer.setForeground(Color.BLACK);
        connectAnswer.setBackground(Color.WHITE);
        connectAnswer.setOpaque(true);

        headPanel.add(back);
        headPanel.add(Box.createHorizontalStrut(200));
        headPanel.add(title);
        headPanel.add(Box.createHorizontalStrut(50));
        headPanel.add(connectAnswer);

        nickInput = new JTextField("fausto",10);
        nickInput.setBackground(Color.WHITE);

        pwdInput = new JTextField("faustofausto",10);
        pwdInput.setBackground(Color.WHITE);

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

        JButton go = new JButton("LOGIN");
        connectButton = new JButton("CONNECT");

        go.setPreferredSize(new Dimension(BUTTONWIDTH,BUTTONHEIGHT));
        connectButton.setPreferredSize(new Dimension(BUTTONWIDTH,BUTTONHEIGHT));

        connectButton.setVisible(false);

        go.addActionListener(this);
        connectButton.addActionListener(this);

        buttonPanel.add(go);
        buttonPanel.add(connectButton);

        JPanel answerPanel = new JPanel();
        answerPanel.setBackground(Color.WHITE);
//        answerPanel.setLayout(new GridLayout(1,2));

        answer = new JLabel("", JLabel.CENTER);
        answer.setForeground(Color.BLACK);
        answer.setBackground(Color.WHITE);
        answer.setOpaque(true);

        answerPanel.add(answer);

        setLayout(new GridLayout(5,1,3,3));
        add(headPanel);
        add(nickPanel);
        add(pwdPanel);
        add(buttonPanel);
        add(answerPanel);

        this.serverConnection();
        this.window.validate();
    }

    //Connessione TCP col server
    private void serverConnection(){
        SocketAddress address = new InetSocketAddress(TCPConnection.HOSTNAME,TCPConnection.PORT);
        try{
            if(client!= null && client.isOpen()) {
                System.out.println("[RECONNECTION] Chiudo la socket");
                client.close();// todo elimina
            }
            client = SocketChannel.open(address);
        } catch (IOException e) {
            System.out.println("[ERROR] Server non disponibile");

            connectAnswer.setText("Server non disponibile");
            connectAnswer.setIcon(new ImageIcon(new ImageIcon(IMAGEPATH+"notconnected.png").getImage().getScaledInstance(15, 15, Image.SCALE_DEFAULT)));

            connectButton.setVisible(true);
            return;
        }

        System.out.println("[OK] Connessione col server stabilita");

        connectAnswer.setText("Connessione stabilita");
        connectAnswer.setIcon(new ImageIcon(new ImageIcon(IMAGEPATH+"connected.png").getImage().getScaledInstance(15, 15, Image.SCALE_DEFAULT)));

        connectButton.setVisible(false);
    }


    public void actionPerformed(ActionEvent actionEvent) {

        System.out.println(actionEvent.getActionCommand());

        if (actionEvent.getActionCommand().equals("LOGIN")){

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

            String request = "LOGIN\n"+nick+"\n"+pwd+"\n"; //Creo la stringa del protocollo

            ByteBuffer buffer = ByteBuffer.allocate(request.length());

            buffer.put(request.getBytes());
            buffer.flip();

            //Invio la stringa di registrazione al server con nick e password
            while (buffer.hasRemaining()) {
                try {
                    client.write(buffer);
                } catch (Exception e) {
                    System.out.println("[ERROR] Errore scrittura del buffer nella socket del server (LOGIN)");

                    connectAnswer.setText("<html>Errore di connessione<br/> col server</html>");
                    connectAnswer.setIcon(new ImageIcon(new ImageIcon(IMAGEPATH+"notconnected.png").getImage().getScaledInstance(15, 15, Image.SCALE_DEFAULT)));

                    connectButton.setVisible(true);

                    return;
                }
            }

            buffer = ByteBuffer.allocate(BUF_SIZE);

            try {
                int read = client.read(buffer);

                if(read == -1){
                    System.out.println("[ERROR] Errore lettura della socket del server (LOGIN)");

                    connectAnswer.setText("<html>Impossibile comunicare</br> col server</html>");
                    connectAnswer.setIcon(new ImageIcon(new ImageIcon(IMAGEPATH+"notconnected.png").getImage().getScaledInstance(15, 15, Image.SCALE_DEFAULT)));

                    connectButton.setVisible(true);

                    return;
                }
                else {
                    String aux[] = (new String(buffer.array())).split("\n");
                    System.out.println("[RESPONSE] "+aux[1]);
                    answer.setText(aux[1]);

                    if(aux[0].equals("OK")){ //se il login è andato a buon fine mostro la homepage
                        int socketPort = client.socket().getLocalPort();
                        Thread t = new Thread(UDPListener.getInstance(socketPort,window,client,nick));
                        t.start();

                        HomePage homePage = new HomePage(nick,window,client);
                        window.setContentPane(homePage);
                        window.validate();
                    }
                }
            } catch (IOException e) {
                System.out.println("[ERROR] Server chiuso");

                connectAnswer.setText("<html>Impossibile comunicare</br> col server</html>");
                connectAnswer.setIcon(new ImageIcon(new ImageIcon(IMAGEPATH+"notconnected.png").getImage().getScaledInstance(15, 15, Image.SCALE_DEFAULT)));

                connectButton.setVisible(true);

                e.printStackTrace();
            }
        }
        else if(actionEvent.getActionCommand().equals("BACK")){
            StartGUI startGUI = new StartGUI(window);
            window.setContentPane(startGUI);
            window.validate();
        }
        else{
            this.serverConnection();
        }
    }
}
