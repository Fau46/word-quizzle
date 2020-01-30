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

        JPanel buttonPanel = new JPanel();
        JButton homeButton = new JButton("HOME");

        homeButton.addActionListener(this);

        buttonPanel.add(homeButton);



        setLayout(new GridLayout(4,1,3,3));

        add(titlePanel);
        add(buttonPanel);
    }


    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if(actionEvent.getActionCommand().equals("HOME")){
            HomePage homePage = new HomePage(nickname,window,client);
//            HomePage homePage = HomePage.getHomePage(nickname,window,client);
            challengeFlag.flag.set(0); //abilito il flag per ricevere richieste di sfida
            window.setContentPane(homePage);
            window.validate();
        }
    }
}
