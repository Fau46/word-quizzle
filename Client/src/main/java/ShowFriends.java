import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.tools.javac.util.List;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Vector;

public class ShowFriends extends JPanel implements ActionListener {
    private JFrame window;
    private SocketChannel client;
    private String nickname;
    private int BUF_SIZE = 512;
    private JTextArea friendList;

    public ShowFriends(JFrame window, SocketChannel client, String nickname) {
        this.window = window;
        this.client = client;
        this.nickname = nickname;

        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        friendList = new JTextArea(1,1);
        friendList.setEditable(false);

        JScrollPane friendPanel = new JScrollPane(friendList);

        JPanel buttonPanel = new JPanel();

        JButton home = new JButton("HOME");

        home.addActionListener(this);

        buttonPanel.add(home);

        setLayout(new GridLayout(2,1,3,3));
        add(friendPanel);
        add(buttonPanel);

        this.getFriendsList();
    }

    private void getFriendsList(){
        String request = "SHOWFRIENDS\n"+nickname+"\n";
        ByteBuffer buffer = ByteBuffer.allocate(request.length());

        buffer.put(request.getBytes());
        buffer.flip();

        while (buffer.hasRemaining()){
            try {
                client.write(buffer);
            } catch (IOException e) {
                System.out.println("[ERROR] Errore scrittura del buffer nella socket del server (SHOWFRIENDS)");
                JOptionPane.showMessageDialog(window, "Impossibile comunicare col server.\n Verrai disconnesso", "Server error", JOptionPane.ERROR_MESSAGE);
                StartGUI startGUI = new StartGUI(window);
                window.setContentPane(startGUI);
                window.validate();
                break;
            }
        }

        buffer = ByteBuffer.allocate(BUF_SIZE);

        try {
            int read = client.read(buffer); //TODO mettere in un ciclo

            if(read == - 1){//Se riscontro un errore nella lettura
                System.out.println("[ERROR] Errore lettura della socket del server (SHOWFRIENDS)");
                JOptionPane.showMessageDialog(window, "Impossibile comunicare col server.\n Verrai disconnesso", "Server error", JOptionPane.ERROR_MESSAGE);
                StartGUI startGUI = new StartGUI(window);
                window.setContentPane(startGUI);
                window.validate();
                return;
            }
            else{ //se la lettura è andata a buon fine
                Gson gson = new Gson();
                String aux[] = (new String(buffer.array())).split("\n");
                System.out.println("[RESPONSE] "+aux[1]);

                Type listType = new TypeToken<Vector<String>>(){}.getType();
                Vector<String> listaAmici = gson.fromJson(aux[1],listType);

                if(aux[0].equals("OK")){ //se il logout è andato a buon fine
                        for(String friend : listaAmici){
                            friendList.append(friend+"\n");
                        }
                }
                else {
//                    response.setText(aux[1]);
                }
            }

        } catch (IOException e) {
            System.out.println("[ERROR] Server chiuso");
            e.printStackTrace();
        }
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if(actionEvent.getActionCommand().equals("HOME")){
            System.out.println("Sonoo qui");
            HomePage startGUI = new HomePage(nickname,window,client);
            window.setContentPane(startGUI);
            window.validate();
        }
    }
}




