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

public class HomePage extends JPanel implements ActionListener{
    private JFrame window;
    private JLabel response;
    private SocketChannel client;
    private String nickname;
    private int BUF_SIZE = 512;

    public HomePage(String nick, JFrame window, SocketChannel client) {
        this.window = window;
        this.client = client;
        this.nickname = nick;

        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        JPanel welcomePanel = new JPanel();
        welcomePanel.add(new JLabel("BENVENUTO IN WORD QUIZZLE"));

        JPanel nickPanel = new JPanel();
        nickPanel.add(new JLabel(nickname));

        JPanel buttonPanel = new JPanel();

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

        buttonPanel.add(logout);
        buttonPanel.add(addFriend);
        buttonPanel.add(showFriends);
        buttonPanel.add(showScore);
        buttonPanel.add(showRank);
        buttonPanel.add(sfida);

        response = new JLabel("",JLabel.CENTER);
        response.setForeground(Color.BLACK);
        response.setBackground(Color.WHITE);
        response.setOpaque(true);

        JPanel responsePanel = new JPanel();
        responsePanel.add(response);

        setLayout(new GridLayout(4,1,3,3));
        add(welcomePanel);
        add(nickPanel);
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
        System.out.println(friend);
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
        String request = "SHOWFRIENDS\n"+nickname+"\n";
        ByteBuffer buffer = ByteBuffer.allocate(request.length());

        buffer.put(request.getBytes());
        buffer.flip();

            long start = System.currentTimeMillis();

        while (buffer.hasRemaining()){
            try {
                client.write(buffer);
            } catch (IOException e) {
                System.out.println("[ERROR] Errore scrittura del buffer nella socket del server (SHOWFRIENDS)");
                serverError();
                break;
            }
        }

        try {
            int read;

            ByteBuffer auxBuffer = ByteBuffer.allocate(BUF_SIZE);
            read = client.read(auxBuffer); //Leggo la lunghezza della risposta

            if(read == - 1){//Se riscontro un errore nella lettura
                System.out.println("[ERROR] Errore lettura della socket del server (SHOWFRIENDS/LUNGHEZZA)");
                serverError();
                return;
            }
            else{
                String auxString = new String(auxBuffer.array(),0,read);
                int responseLen = Integer.parseInt(auxString); //Converto la lunghezza in int

                buffer = ByteBuffer.allocate(responseLen);
                read = client.read(buffer); //leggo la risposta del server

                if(read == - 1){//Se riscontro un errore nella lettura
                    System.out.println("[ERROR] Errore lettura della socket del server (SHOWFRIENDS)");
                    serverError();
                    return;
                }
                else{ //se la lettura è andata a buon fine
                    Gson gson = new Gson();
                    String aux[] = (new String(buffer.array())).split("\n");
                    System.out.println("[RESPONSE] "+aux[1]);

                        double finish = (double) (System.currentTimeMillis() - start) / 1000.0;
                        System.out.println("Finish: "+finish);

                    Type listType = new TypeToken<Vector<String>>(){}.getType();
                    Vector<String> listaAmici = gson.fromJson(aux[1],listType);

                    if(aux[0].equals("OK")){ //se è andato a buon fine TODO forse inutile, controlla se il server ritorna sempre ok
                        ShowFriends showFriends= new ShowFriends(window,client,nickname,listaAmici);
                        window.setContentPane(showFriends);
                        window.validate();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[ERROR] Server chiuso");
            e.printStackTrace();
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
//                response.setText(aux[1]);
                JOptionPane.showMessageDialog(window, "Il tuo punteggio è "+aux[1], "Punti", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void showRank() {
        String request = "SHOWRANK\n"+nickname+"\n";
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

        try{
            int read;

            ByteBuffer auxBuffer = ByteBuffer.allocate(BUF_SIZE);
            read = client.read(auxBuffer); //Leggo la lunghezza della risposta

            if(read == - 1){//Se riscontro un errore nella lettura
                System.out.println("[ERROR] Errore lettura della socket del server (SHOWFRIENDS/LUNGHEZZA)");
                serverError();
                return;
            }
            else{
                String auxString = new String(auxBuffer.array(),0,read);
                int responseLen = Integer.parseInt(auxString); //Converto la lunghezza in int

                buffer = ByteBuffer.allocate(responseLen);
                read = client.read(buffer); //leggo la risposta del server

                if(read == - 1){//Se riscontro un errore nella lettura
                    System.out.println("[ERROR] Errore lettura della socket del server (SHOWFRIENDS)");
                    serverError();
                    return;
                }
                else{
                    Gson gson = new Gson();
                    String aux[] = (new String(buffer.array())).split("\n");
                    System.out.println("[RESPONSE] "+aux[1]);

                    Type listType = new TypeToken<HashMap<String,Integer>>(){}.getType();
                    HashMap<String,Integer> listaAmici = gson.fromJson(aux[1],listType);

                    if(aux[0].equals("OK")){ //se è andato a buon fine TODO forse inutile, controlla se il server ritorna sempre ok
                        ShowRank showRank = new ShowRank (window,client,nickname,listaAmici);
                        window.setContentPane(showRank);
                        window.validate();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[ERROR] Server chiuso");
            e.printStackTrace();
        }

    }


    private void serverError(){
        JOptionPane.showMessageDialog(window, "Impossibile comunicare col server.\n Verrai disconnesso", "Server error", JOptionPane.ERROR_MESSAGE);
        StartGUI startGUI = new StartGUI(window);
        window.setContentPane(startGUI);
        window.validate();
    }


}
