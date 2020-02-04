
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
    private JTextField inputWord;
    private JLabel response, word;
    private ChallengeFlag challengeFlag;
    private JButton traduciButton, homeButton, skipButton;
    private Timer challengeTimer, test;


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
        homeButton = new JButton("HOME");
        traduciButton = new JButton("TRADUCI");
        skipButton = new JButton("SKIP");

        traduciButton.setVisible(false);
        homeButton.setVisible(false);
        skipButton.setVisible(false);

        homeButton.addActionListener(this);
        traduciButton.addActionListener(this);
        skipButton.addActionListener(this);

        buttonPanel.add(homeButton);
        buttonPanel.add(traduciButton);
        buttonPanel.add(skipButton);

        JPanel responsePanel = new JPanel();

        response = new JLabel("Caricamento...");
        JLabel time = new JLabel();

        responsePanel.add(response);
        responsePanel.add(time);

        setLayout(new GridLayout(4,1,3,3));

        add(titlePanel);
        add(inputPanel);
        add(buttonPanel);
        add(responsePanel);


        //-----------TEST-------
        ActionListener al = new ActionListener() {
            Integer i = 0;

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                time.setText(i.toString());
                i++;
            }
        };

        test = new Timer(1000,al);

//        test.start();

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
                traduciButton.setVisible(true);
                skipButton.setVisible(true);

                word.setText(responseArray[2]);
                response.setText("");

                int time = Integer.parseInt(responseArray[3]);
                time = time * 1000;

                challengeTimer = new Timer(time,this::actionPerformed);
                challengeTimer.start();

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
            if(response[0].equals("CHALLENGE")){
                this.word.setText(response[1]);
                this.inputWord.setText("");
                challengeTimer.restart();
            }
            else if(response[0].equals("FINISH")){
                challengeTimer.stop();

                inputWord.setVisible(false);

                traduciButton.setVisible(false);
                skipButton.setVisible(false);
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
        if(actionEvent.getActionCommand() == null){
            serverComunication("skip");
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
                serverComunication(word);
            }
            else{
                response.setText("Inserisci la traduzione.");
            }
        }
        else if(actionEvent.getActionCommand().equals("SKIP")){
            serverComunication("skip");
        }

//        test.restart();
    }

}
