import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.channels.SocketChannel;

public class Challenge extends JPanel implements ActionListener {
    private JFrame window;
    public SocketChannel client;
    private String nickname;
    private ChallengeFlag challengeFlag;
    private static Challenge challenge;

    private Challenge(JFrame window, SocketChannel client, String nickname){
        this.window = window;
        this.client = client;
        this.nickname = nickname;

        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        JPanel titlePanel = new JPanel();
        JLabel title = new JLabel("SFIDA");
        titlePanel.add(title);

        JPanel buttonPanel = new JPanel();
        JButton homeButton = new JButton("HOME");

        homeButton.addActionListener(this);

        buttonPanel.add(homeButton);



        setLayout(new GridLayout(4,1,3,3));

        add(titlePanel);
        add(buttonPanel);
    }

    public static void setChallenge(JFrame window, SocketChannel client, String nickname){
        if(challenge == null) challenge = new Challenge(window,client,nickname);
    }

    public static Challenge getChallenge(){
        return challenge;
    }

    public void printChallenge(){

    }


    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if(actionEvent.getActionCommand().equals("HOME")){
            HomePage homePage = new HomePage(nickname,window,client);
//            sfida_in_corso.flag = false; //abilito il flag per ricevere richieste di sfida
            window.setContentPane(homePage);
            window.validate();
        }
    }
}
