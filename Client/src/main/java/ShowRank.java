import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.channels.SocketChannel;
import java.util.*;

public class ShowRank extends JPanel implements ActionListener {
    private JFrame window;
    private SocketChannel client;
    private String nickname;
    private JTextArea friendList;

    public ShowRank(JFrame window, SocketChannel client, String nickname, Map<String, Integer> listaAmici){
        this.window = window;
        this.client = client;
        this.nickname = nickname;

        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        friendList = new JTextArea(1,1);
        friendList.setEditable(false);

        for(String username : listaAmici.keySet()){
            friendList.append(username+" "+listaAmici.get(username)+"\n");
        }

        JScrollPane friendPanel = new JScrollPane(friendList);

        JPanel buttonPanel = new JPanel();

        JButton home = new JButton("HOME");

        home.addActionListener(this);

        buttonPanel.add(home);

        setLayout(new GridLayout(2,1,3,3));
        add(friendPanel);
        add(buttonPanel);
    }


    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if(actionEvent.getActionCommand().equals("HOME")){
            HomePage startGUI = new HomePage(nickname,window,client);
//            HomePage startGUI = HomePage.getHomePage(nickname,window,client);
            window.setContentPane(startGUI);
            window.validate();
        }
    }
}
