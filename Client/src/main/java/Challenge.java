
import Costanti.Costanti;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


public class Challenge extends JPanel implements ActionListener,Costanti {
    private JFrame window;
    private String nickname;
    private SocketChannel client;

    private Timer countdown;
    private JTextField inputWord;
    private ChallengeFlag challengeFlag;
    private JLabel response, word, countdownDisplay;
    private JButton traduciButton, homeButton, skipButton;


    public Challenge(JFrame window, SocketChannel client, String nickname){
        this.window = window;
        this.client = client;
        this.nickname = nickname;
        this.challengeFlag = ChallengeFlag.getInstance();

        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(Color.WHITE);

        JLabel title = new JLabel("SFIDA");
        title.setFont(new Font(title.getFont().getName(), Font.PLAIN,25));


        titlePanel.add(title);

        word = new JLabel();
        word.setFont(new Font(word.getFont().getName(), Font.PLAIN,15));

        inputWord = new JTextField("",10);
        inputWord.setBackground(Color.WHITE);
        inputWord.setVisible(false);

        JPanel inputPanel = new JPanel();
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setLayout(new GridLayout(0, 1));

        inputPanel.add(word);
        inputPanel.add(inputWord);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.WHITE);

        homeButton = new JButton("HOME");
        skipButton = new JButton("SKIP");
        traduciButton = new JButton("TRADUCI");

        homeButton.setPreferredSize(new Dimension(BUTTONWIDTH,BUTTONHEIGHT));
        skipButton.setPreferredSize(new Dimension(BUTTONWIDTH,BUTTONHEIGHT));
        traduciButton.setPreferredSize(new Dimension(BUTTONWIDTH+30,BUTTONHEIGHT));

        homeButton.setVisible(false);
        skipButton.setVisible(false);
        traduciButton.setVisible(false);

        homeButton.setIcon(new ImageIcon(new ImageIcon(IMAGEPATH+"home.png").getImage().getScaledInstance(15, 15, Image.SCALE_DEFAULT)));
        skipButton.setIcon(new ImageIcon(new ImageIcon(IMAGEPATH+"skip.png").getImage().getScaledInstance(15, 15, Image.SCALE_DEFAULT)));
        traduciButton.setIcon(new ImageIcon(new ImageIcon(IMAGEPATH+"translate.png").getImage().getScaledInstance(25, 25, Image.SCALE_DEFAULT)));

        homeButton.addActionListener(this);
        skipButton.addActionListener(this);
        traduciButton.addActionListener(this);

        buttonPanel.add(homeButton);
        buttonPanel.add(traduciButton);
        buttonPanel.add(skipButton);

        JPanel responsePanel = new JPanel();
        responsePanel.setBackground(Color.WHITE);

        response = new JLabel("Caricamento...");

        countdownDisplay = new JLabel();

        responsePanel.add(response);
        responsePanel.add(countdownDisplay);

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
                JOptionPane.showMessageDialog(window, responseArray[1], "Error", JOptionPane.ERROR_MESSAGE);
                HomePage homePage = new HomePage(nickname,window,client);

                challengeFlag.resetFlag(); //abilito il flag per ricevere richieste di sfida
                window.setContentPane(homePage);
                window.validate();

            }
            else if(responseArray[0].equals("OK")){
                int timer = Integer.parseInt(responseArray[3]); //Parso il timer da settare successivamente

                countdown = new Timer(1000, new Countdown(timer));

                countdown.start();

                inputWord.setVisible(true);
                traduciButton.setVisible(true);
                skipButton.setVisible(true);

                word.setText(responseArray[2]);
                response.setText("");
                window.validate();
            }
        }
    }


    //Metodo che si occupa di inviare la parola tradotta al server e parsare la risposta
    private void serverComunication(String word){
        sendWord(word);
        String[] response = readResponse();

        if(response != null){
            if(response[0].equals("CHALLENGE")){
                this.word.setText(response[1]);
                this.inputWord.setText("");
                this.response.setText("");

            }
            else if(response[0].equals("FINISH")){
                countdown.stop();

                countdownDisplay.setVisible(false);

                inputWord.setVisible(false);

                traduciButton.setVisible(false);
                skipButton.setVisible(false);

                this.response.setText("<html>"+response[1]+".<br/>In attesa che finisca il tuo avversario.</html>");
                this.word.setVisible(false);

                finishChallenge();
            }
        }
    }


    ///Metodo che si occupa di inviare la response al server
    private void sendWord(String response){
        ByteBuffer buffer = ByteBuffer.allocate(response.length());

        buffer.put(response.getBytes());
        buffer.flip();

        //Invio la stringa di registrazione al server con nick e password
        while (buffer.hasRemaining()) {
            try {
                client.write(buffer);
            } catch (Exception e) {
                System.out.println("[ERROR] Errore scrittura del buffer nella socket del server (CHALLENGE)");
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
                System.out.println("[ERROR] Errore lettura della socket del server (CHALLENGE/1)");
                UDPListener.getInstance().serverError();
                return null;
            }
            else{
                String tempString = new String(buffer.array(),0,read); //Inserisco quello che ho letto in una stringa temporanea

                int indexNewLine = tempString.indexOf('\n');

                String responseLenString = new String(buffer.array(),0,indexNewLine); //Leggo la lunghezza della stringa
                StringBuilder responseServer = new StringBuilder(tempString.substring(indexNewLine+1)); //Leggo la risposta del server

                int responseLen = Integer.parseInt(responseLenString); //Converto la lunghezza in int

                if(responseLen !=  responseServer.length()){ //Se ho ancora roba da leggere
                    buffer = ByteBuffer.allocate(responseLen); //
                    read = client.read(buffer); //leggo la risposta del server


                    if(read == - 1){//Se riscontro un errore nella lettura
                        System.out.println("[ERROR] Errore lettura della socket del server (CHALLENGE/2)");
                        UDPListener.getInstance().serverError();
                        return null;
                    }
                    else{
                        responseServer.append(new String(buffer.array(),0,read)); //Appendo la stringa letta
                    }
                }

                String aux[] = (responseServer.toString().split("\n"));

                System.out.println("[RESPONSE] "+aux[1]);

                return aux;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void finishChallenge(){
        JOptionPane.showMessageDialog(window, "Sfida terminata!", "Finish", JOptionPane.INFORMATION_MESSAGE);

        homeButton.setVisible(true);

        String[] responseArray = readResponse();

        StringBuilder stringBuilder = new StringBuilder("<html>");

        for(int i = 1; i<responseArray.length; i++){
            stringBuilder.append(responseArray[i]+"<br/>");
        }

        stringBuilder.append("</html>");

        this.response.setText("stringBuilder.toString()");
        this.response.setText(stringBuilder.toString());
    }


    @Override
    public void actionPerformed(ActionEvent actionEvent) {

        if(actionEvent.getActionCommand() == null || actionEvent.getActionCommand().equals("SKIP")){
            serverComunication("SKIP\n");
        }
        else if(actionEvent.getActionCommand().equals("HOME")){
            HomePage homePage = new HomePage(nickname,window,client);
            challengeFlag.resetFlag(); //abilito il flag per ricevere richieste di sfida
            window.setContentPane(homePage);
            window.validate();
        }
        else if(actionEvent.getActionCommand().equals("TRADUCI")){
            String word = inputWord.getText();

            if(!word.equals("")){
                serverComunication("RESPONSE\n"+word+"\n");
            }
            else{
                response.setText("Inserisci la traduzione.");
            }
        }
    }

    //Classe che si occupa del countdown per il termine della sfida
    class Countdown implements ActionListener{
        Integer time;

        public Countdown(int timer){
            this.time = timer;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            countdownDisplay.setText(time.toString());
            time--;

            if(time == -1) {
                serverComunication("COUNTDOWN\n");
            }
        }
    }

}
