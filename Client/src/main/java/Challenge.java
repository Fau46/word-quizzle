import com.sun.media.sound.JDK13Services;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Challenge extends JPanel implements ActionListener {
    private int BUF_SIZE = 512;
    private JFrame window;
    public SocketChannel client;
    private String nickname;
    private ChallengeFlag challengeFlag;
    private JPanel buttonPanel, inputPanel;
    private JLabel response, word;
    private JTextField inputWord;

    public Challenge(JFrame window, SocketChannel client, String nickname){
        this.window = window;
        this.client = client;
        this.nickname = nickname;
        this.challengeFlag = ChallengeFlag.getInstance();

        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        JPanel titlePanel = new JPanel();
        JLabel title = new JLabel("SFIDA");
        titlePanel.add(title);

        word = new JLabel();

        inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(0, 1));

//        JPanel buttonPanel = new JPanel();
        buttonPanel = new JPanel();
        JButton homeButton = new JButton("HOME");

        homeButton.addActionListener(this);

        buttonPanel.add(homeButton);

        JPanel responsePanel = new JPanel();

        response = new JLabel("Caricamento...");

        responsePanel.add(response);


        setLayout(new GridLayout(4,1,3,3));

        add(titlePanel);
        add(inputPanel);
        add(buttonPanel);
        add(responsePanel);

    }

    public void run(){
        JOptionPane.showMessageDialog(window, "La sfida comincerà a breve", "Loadin", JOptionPane.INFORMATION_MESSAGE);

        String[] responseArray = readResponse();

        if(responseArray[0].equals("KO")){
//                    response.setText(aux[1]);
            this.challengeFlag.flag.set(0);
        }
        else if(responseArray[0].equals("OK")){
            inputWord = new JTextField("",10);
            inputWord.setBackground(Color.WHITE);

            word.setText(responseArray[2]);

            inputPanel.add(word);
            inputPanel.add(inputWord);

            JButton traduci = new JButton("TRADUCI");
            traduci.addActionListener(this);
            buttonPanel.add(traduci);
            response.setText("");
            window.validate();
        }
    }

    public void sendWord(String word){
        ByteBuffer buffer = ByteBuffer.allocate(word.length());

        buffer.put(word.getBytes());
        buffer.flip();

        //Invio la stringa di registrazione al server con nick e password
        while (buffer.hasRemaining()) {
            try {
                client.write(buffer);
            } catch (Exception e) {
                System.out.println("[ERROR] Errore scrittura del buffer nella socket del server (LOGIN)");
//                connectAnswer.setText("Errore di connessione col server");
                return;
            }
        }
    }

    private String[] readResponse(){
        try {
            ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);

            int read = client.read(buffer);

            if(read == -1){
                System.out.println("[ERROR] Errore lettura della socket del server (CHALLENGE)");
                UDPListener.getInstance().serverError();
                return null;
            }
            else{
                String aux[] = (new String(buffer.array())).split("\n");
                System.out.println("[RESPONSE] "+aux[1]);

                return aux;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void serverComunication(String word){
        sendWord(word);
        String[] response = readResponse();

        this.word.setText(response[0]);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if(actionEvent.getActionCommand().equals("HOME")){
            HomePage homePage = new HomePage(nickname,window,client);
            challengeFlag.flag.set(0); //abilito il flag per ricevere richieste di sfida
            window.setContentPane(homePage);
            window.validate();
        }
        if(actionEvent.getActionCommand().equals("TRADUCI")){
            serverComunication(inputWord.getText());
        }
    }
}
