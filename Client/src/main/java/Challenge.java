import com.sun.media.sound.JDK13Services;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Challenge extends JPanel implements ActionListener {
    private JFrame window;
    private String nickname;
    public SocketChannel client;

    private int BUF_SIZE = 512;
    private ChallengeFlag challengeFlag;
    private JLabel response, word;
    private JTextField inputWord;
    private JButton traduci, homeButton, skip;


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

        inputWord = new JTextField("",10);
        inputWord.setBackground(Color.WHITE);
        inputWord.setVisible(false);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(0, 1));

        inputPanel.add(word);
        inputPanel.add(inputWord);

        JPanel buttonPanel = new JPanel();
//        JButton homeButton = new JButton("HOME");
        homeButton = new JButton("HOME");
        traduci = new JButton("TRADUCI");
        skip = new JButton("SKIP");

        traduci.setVisible(false);
        homeButton.setVisible(false);
        skip.setVisible(false);

        homeButton.addActionListener(this);
        traduci.addActionListener(this);
        skip.addActionListener(this);

        buttonPanel.add(homeButton);
        buttonPanel.add(traduci);
        buttonPanel.add(skip);

        JPanel responsePanel = new JPanel();

        response = new JLabel("Caricamento...");

        responsePanel.add(response);


        setLayout(new GridLayout(4,1,3,3));

        add(titlePanel);
        add(inputPanel);
        add(buttonPanel);
        add(responsePanel);

    }

    //Metodo che si occupa di far cominciare la sfida
    public void run(){
        JOptionPane.showMessageDialog(window, "La sfida comincerà a breve", "Loading", JOptionPane.INFORMATION_MESSAGE);

        String[] responseArray = readResponse();

        if(responseArray != null){
            if(responseArray[0].equals("KO")){
                this.challengeFlag.resetFlag();
            }
            else if(responseArray[0].equals("OK")){
                inputWord.setVisible(true);
                traduci.setVisible(true);
                skip.setVisible(true);

                word.setText(responseArray[2]);
                response.setText("");

                window.validate();
            }
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
                UDPListener.getInstance().serverError();
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
        System.out.println("INVIO "+word);
        String[] response = readResponse();

        if(response != null){
            if(response[0].equals("OK")){
                this.word.setText(response[1]);
                this.inputWord.setText("");
            }
            else if(response[0].equals("FINISH")){
                inputWord.setVisible(false);

                traduci.setVisible(false);
                skip.setVisible(false);
                homeButton.setVisible(true);

                this.response.setText("<html>"+response[1]+".<br/>In attesa che finisca il tuo avversario.</html>");
                this.word.setText("");

                window.validate();

                finishChallenge();
            }
        }


    }

    private void finishChallenge(){
        JOptionPane.showMessageDialog(window, "Sfida terminata!", "Finish", JOptionPane.INFORMATION_MESSAGE);

        String[] response = readResponse();

        StringBuilder stringBuilder = new StringBuilder("<html>");

        for(int i = 1; i<response.length; i++){
            stringBuilder.append(response[i]+"<br/>");
        }

        stringBuilder.append("</html>");

        this.response.setText(stringBuilder.toString());
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if(actionEvent.getActionCommand().equals("HOME")){
            HomePage homePage = new HomePage(nickname,window,client);
            challengeFlag.resetFlag(); //abilito il flag per ricevere richieste di sfida
            window.setContentPane(homePage);
            window.validate();
        }
        else if(actionEvent.getActionCommand().equals("TRADUCI")){
            String word = inputWord.getText();
            if(!word.equals("")){
                serverComunication(word);
            }
            else{
                response.setText("Inserisci la traduzione.");
            }
        }
        else if(actionEvent.getActionCommand().equals("SKIP")){
            serverComunication("skip");
        }
    }
}
