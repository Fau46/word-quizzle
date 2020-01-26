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
    private Vector<String> listaAmici;

    public ShowFriends(JFrame window, SocketChannel client, String nickname, Vector<String> listaAmici) {
        this.window = window;
        this.client = client;
        this.nickname = nickname;
        this.listaAmici = listaAmici;

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

//        this.getFriendsList();
    }

    public void show(){
        for(String amico : listaAmici){
            friendList.append(amico+"\n");
        }
    }


    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if(actionEvent.getActionCommand().equals("HOME")){
            HomePage startGUI = new HomePage(nickname,window,client);
            window.setContentPane(startGUI);
            window.validate();
        }
    }
}




