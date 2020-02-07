import Costanti.Costanti;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;

public class HomePage extends JPanel implements ActionListener, Costanti {
    private JFrame window;
    private String nickname;
    private SocketChannel client;

    private JLabel response;
    private int BUF_SIZE = 512;
    private ChallengeFlag challengeFlag;

    public HomePage(String nick, JFrame window, SocketChannel client) {
        this.window = window;
        this.client = client;
        this.nickname = nick;
        this.challengeFlag = ChallengeFlag.getInstance();

        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        JPanel headPanel = new JPanel();
        headPanel.setLayout(new FlowLayout());
        headPanel.setBackground(Color.WHITE);

        JPanel welcomePanel = new JPanel();
        welcomePanel.setBackground(Color.WHITE);

        JLabel title = new JLabel();
        JLabel nickLabel = new JLabel(nickname);

        nickLabel.setFont(new Font(title.getFont().getName(),Font.PLAIN,20));

        title.setIcon(new ImageIcon(new ImageIcon(IMAGEPATH+"welcome.png").getImage().getScaledInstance(200, 110, Image.SCALE_DEFAULT)));

        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        nickLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        welcomePanel.add(title);
        welcomePanel.add(Box.createVerticalStrut(10));
        welcomePanel.add(nickLabel);
        welcomePanel.setLayout(new BoxLayout(welcomePanel,BoxLayout.Y_AXIS));

        headPanel.add(welcomePanel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setLayout(new GridLayout(1,1));

        JPanel buttonPanel1 = new JPanel();
        buttonPanel1.setBackground(Color.WHITE);
        buttonPanel1.setLayout(new BoxLayout(buttonPanel1,BoxLayout.Y_AXIS));

        JPanel buttonPanel2 = new JPanel();
        buttonPanel2.setBackground(Color.WHITE);
        buttonPanel2.setLayout(new BoxLayout(buttonPanel2,BoxLayout.Y_AXIS));

        JButton logout = new JButton("LOGOUT");
        JButton addFriend = new JButton("AGGIUNGI AMICO");
        JButton showFriends = new JButton("VISUALIZZA AMICI");
        JButton showScore = new JButton("VISUALIZZA PUNTEGGIO");
        JButton showRank = new JButton("VISUALIZZA CLASSIFICA");
        JButton sfida = new JButton("SFIDA");

        logout.addActionListener(this);
        addFriend.addActionListener(this);
        showFriends.addActionListener(this);
        showScore.addActionListener(this);
        showRank.addActionListener(this);
        sfida.addActionListener(this);

        addFriend.setAlignmentX(Component.CENTER_ALIGNMENT);
        showFriends.setAlignmentX(Component.CENTER_ALIGNMENT);
        showScore.setAlignmentX(Component.CENTER_ALIGNMENT);
        showRank.setAlignmentX(Component.CENTER_ALIGNMENT);
        sfida.setAlignmentX(Component.CENTER_ALIGNMENT);
        logout.setAlignmentX(Component.CENTER_ALIGNMENT);

        buttonPanel1.add(addFriend);
        buttonPanel1.add(Box.createVerticalStrut(20));
        buttonPanel1.add(showFriends);
        buttonPanel1.add(Box.createVerticalStrut(20));
        buttonPanel1.add(showScore);

        buttonPanel2.add(showRank);
        buttonPanel2.add(Box.createVerticalStrut(20));
        buttonPanel2.add(sfida);
        buttonPanel2.add(Box.createVerticalStrut(20));
        buttonPanel2.add(logout);

        buttonPanel.add(buttonPanel1);
        buttonPanel.add(buttonPanel2);

        response = new JLabel("",JLabel.CENTER);
        response.setForeground(Color.BLACK);
        response.setBackground(Color.WHITE);
        response.setOpaque(true);

        JPanel responsePanel = new JPanel();
        responsePanel.setBackground(Color.WHITE);
        responsePanel.add(response);

        setLayout(new GridLayout(3,1,3,3));
        add(headPanel);
//        add(nickPanel);
        add(buttonPanel);
        add(responsePanel);
    }


    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if(actionEvent.getActionCommand().equals("LOGOUT")){
            logout();
        }
        else if(actionEvent.getActionCommand().equals("AGGIUNGI AMICO")){
            addFriend();
        }
        else if(actionEvent.getActionCommand().equals("VISUALIZZA AMICI")){
            showFriends();
        }
        else if(actionEvent.getActionCommand().equals("VISUALIZZA PUNTEGGIO")){
            showScore();
        }
        else if(actionEvent.getActionCommand().equals("VISUALIZZA CLASSIFICA")){
            showRank();
        }
        else if(actionEvent.getActionCommand().equals("SFIDA")){
            challenge();
        }
    }


    private void logout(){
        String request = "LOGOUT\n"+nickname+"\n";
        ByteBuffer buffer = ByteBuffer.allocate(request.length());

        buffer.put(request.getBytes());
        buffer.flip();

        while (buffer.hasRemaining()){
            try {
                client.write(buffer);
            } catch (IOException e) {
                System.out.println("[ERROR] Errore scrittura del buffer nella socket del server (LOGOUT)");
                this.serverError();
                break;
            }
        }

        buffer = ByteBuffer.allocate(BUF_SIZE);

        try {
            int read = client.read(buffer);
            if(read == - 1){//Se riscontro un errore nella lettura
                System.out.println("[ERROR] Errore lettura della socket del server (LOGOUT)");
                this.serverError();
            }
            else{ //se la lettura è andata a buon fine
                String aux[] = (new String(buffer.array())).split("\n");
                System.out.println("[RESPONSE] "+aux[1]);

                if(aux[0].equals("OK")){ //se il logout è andato a buon fine
                    UDPListener.getInstance().shutdownAndClear();

                    StartGUI startGUI = new StartGUI(window);
                    window.setContentPane(startGUI);
                    window.validate();
                }
                else {
                    response.setText(aux[1]);
                }
            }

        } catch (IOException e) {
            System.out.println("[ERROR] Server chiuso");
            e.printStackTrace();
        }
    }


    private void addFriend(){
        String friend = JOptionPane.showInputDialog(window,"Inserisci l'amico che vuoi aggiungere","");

        if(friend!=null && !friend.equals("")){
            String request = "ADDFRIEND\n"+nickname+"\n"+friend+"\n";
            ByteBuffer buffer = ByteBuffer.allocate(request.length());

            buffer.put(request.getBytes());
            buffer.flip();

            while (buffer.hasRemaining()){
                try {
                    client.write(buffer);
                } catch (IOException e) {
                    System.out.println("[ERROR] Errore scrittura del buffer nella socket del server (ADDFRIEND)");
                    this.serverError();
                    break;
                }
            }

            buffer = ByteBuffer.allocate(BUF_SIZE);

            try {
                int read = client.read(buffer);
                if(read == - 1){//Se riscontro un errore nella lettura
                    System.out.println("[ERROR] Errore lettura della socket del server (ADDFRIEND)");
                    this.serverError();

                }
                else { //se la lettura è andata a buon fine
                    String aux[] = (new String(buffer.array())).split("\n");
                    System.out.println("[RESPONSE] " + aux[1]);
                    response.setText(aux[1]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }


    private void showFriends(){
        Gson gson = new Gson();
        String request = "SHOWFRIENDS\n"+nickname+"\n";
        String aux[] = ReadWriteLen(request,"SHOWRANK");

        if(aux != null){
            System.out.println("[RESPONSE] "+aux[1]);

            Type listType = new TypeToken<Vector<String>>(){}.getType();
            Vector<String> listaAmici = gson.fromJson(aux[1],listType);

            if(aux[0].equals("OK")){ //se è andato a buon fine
                ShowFriends showFriends= new ShowFriends(window,client,nickname,listaAmici);
                window.setContentPane(showFriends);
                window.validate();
            }
            else{
                response.setText(aux[1]);
            }
        }
    }


    private void showScore() {
        String request = "SHOWSCORE\n"+nickname+"\n";
        ByteBuffer buffer = ByteBuffer.allocate(request.length());

        buffer.put(request.getBytes());
        buffer.flip();

        while (buffer.hasRemaining()){
            try {
                client.write(buffer);
            } catch (IOException e) {
                System.out.println("[ERROR] Errore scrittura del buffer nella socket del server (SHOWSCORE)");
                serverError();
                break;
            }
        }

        buffer = ByteBuffer.allocate(BUF_SIZE);

        try {
            int read = client.read(buffer);
            if(read == - 1){//Se riscontro un errore nella lettura
                System.out.println("[ERROR] Errore lettura della socket del server (ADDFRIEND)");
                this.serverError();

            }
            else { //se la lettura è andata a buon fine
                String aux[] = (new String(buffer.array())).split("\n");
                System.out.println("[RESPONSE] " + aux[1]);

                if(aux[0].equals("OK")){
                    JOptionPane.showMessageDialog(window, "Il tuo punteggio è "+aux[1], "Punti", JOptionPane.INFORMATION_MESSAGE);
                }
                else{
                    response.setText(aux[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void showRank() {
        String request = "SHOWRANK\n"+nickname+"\n";
        Gson gson = new Gson();
        String aux[] = ReadWriteLen(request,"SHOWRANK");

        if(aux != null){
            System.out.println("[RESPONSE] "+aux[1]);

            Type listType = new TypeToken<Map<String,Integer>>(){}.getType();
            Map<String,Integer> listaAmici = gson.fromJson(aux[1],listType);

            if(aux[0].equals("OK")){ //se è andato a buon fine TODO forse inutile, controlla se il server ritorna sempre ok
                ShowRank showRank = new ShowRank (window,client,nickname,listaAmici);
                window.setContentPane(showRank);
                window.validate();
            }
            else{
                response.setText(aux[1]);
            }
        }
    }


    private void challenge() {
        this.challengeFlag.setFlag();
        String friend = JOptionPane.showInputDialog(window,"Inserisci l'amico che sfidare","");

        if(friend!=null && !friend.equals("")){
            String request = "CHALLENGE\n"+nickname+"\n"+friend+"\n";
            ByteBuffer buffer = ByteBuffer.allocate(request.length());

            buffer.put(request.getBytes());
            buffer.flip();

            while (buffer.hasRemaining()) {
                try {
                    client.write(buffer);
                } catch (Exception e) {
                    System.out.println("[ERROR] Errore scrittura del buffer nella socket del server (CHALLENGE)");
                    serverError();
                    break;
                }
            }

        buffer = ByteBuffer.allocate(BUF_SIZE);

        try {
            int read = client.read(buffer);

            if(read == -1){
                System.out.println("[ERROR] Errore lettura della socket del server (CHALLENGE)");
                serverError();
                return;
            }
            else{
                String aux[] = (new String(buffer.array())).split("\n");
                System.out.println("[RESPONSE] "+aux[1]);

                if(aux[0].equals("KO")){
                    response.setText(aux[1]);
                    this.challengeFlag.resetFlag();
                }
                else if(aux[0].equals("OK")){
                        Challenge challenge = new Challenge(window,client,nickname);
                        window.setContentPane(challenge);
                        window.validate();
                        challenge.run();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        else this.challengeFlag.resetFlag();

    }


    private void serverError(){
        UDPListener.getInstance().serverError(); //Faccio lo shutdown del thread udp
    }


    private String[] ReadWriteLen(String request, String operation){
        ByteBuffer buffer = ByteBuffer.allocate(request.length());

        buffer.put(request.getBytes());
        buffer.flip();

        while (buffer.hasRemaining()){
            try {
                client.write(buffer);
            } catch (IOException e) {
                System.out.println("[ERROR] Errore scrittura del buffer nella socket del server ("+operation+")");
                serverError();
                break;
            }
        }

        try{
            int read;

            buffer = ByteBuffer.allocate(BUF_SIZE);

            read = client.read(buffer); //Leggo la risposta

            if(read == - 1){//Se riscontro un errore nella lettura
                System.out.println("[ERROR] Errore lettura della socket del server ("+operation+"/1)");
                serverError();
                return null;
            }
            else{
                String tempString = new String(buffer.array(),0,read); //Inserisco quello che ho letto in una stringa temporanea

                int indexNewLine = tempString.indexOf('\n');

                String responseLenString = new String(buffer.array(),0,indexNewLine); //Leggo la lunghezza della stringa
                StringBuilder responseServer = new StringBuilder(tempString.substring(indexNewLine+1)); //Leggo la risposta del server

                int responseLen = Integer.parseInt(responseLenString); //Converto la lunghezza in int

                if(responseLen !=  responseServer.length()){ //Se ho ancora roba da leggere
                    buffer = ByteBuffer.allocate(responseLen);
                    read = client.read(buffer); //leggo la risposta del server

                    responseServer.append(new String(buffer.array(),0,read)); //Appendo la stringa letta

                    if(read == - 1){//Se riscontro un errore nella lettura
                        System.out.println("[ERROR] Errore lettura della socket del server ("+operation+"/2)");
                        serverError();
                        return null;
                    }
                }

                String aux[] = (responseServer.toString().split("\n"));
                return aux;
            }
        } catch (IOException e) {
            System.out.println("[ERROR] Server chiuso");
            e.printStackTrace();
        }

        return null;
    }
}
