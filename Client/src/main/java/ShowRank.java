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
    private HashMap<String, Integer> listaAmici;

    public ShowRank(JFrame window, SocketChannel client, String nickname, HashMap<String, Integer> listaAmici){
        this.window = window;
        this.client = client;
        this.nickname = nickname;
        this.listaAmici = listaAmici;

        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        friendList = new JTextArea(1,1);
        friendList.setEditable(false);

        for (Map.Entry<String,Integer> entry : listaAmici.entrySet()) {
            Integer value = entry.getValue();
            String key = entry.getKey();
            friendList.append(key+" "+value+"\n");
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

    public void show(){

//        for(String username : listaAmici.keySet()){
//            friendList.append(username+" "+listaAmici.get(username)+"\n");
//        }
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
