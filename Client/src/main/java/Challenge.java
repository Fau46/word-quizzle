import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Challenge extends JPanel implements ActionListener {
    private JFrame window;
    private SocketChannel client;
    private String nickname;
    private JTextField friendInput;
    private int BUF_SIZE = 512;
    private JLabel answer;
    private static Challenge challenge;
    private static boolean sfida_in_corso;


    public Challenge(JFrame window, SocketChannel client, String nickname){
        this.window = window;
        this.client = client;
        this.nickname = nickname;
        this.sfida_in_corso = true;

        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        JPanel titlePanel = new JPanel();
        JLabel title = new JLabel("SFIDA");
        titlePanel.add(title);

        friendInput= new JTextField("",10);
        friendInput.setBackground(Color.WHITE);

        JPanel friendPanel = new JPanel();
        friendPanel.setLayout(new GridLayout(0,1));
        friendPanel.add(new JLabel("Inserisci il nickname del tuo amico"),JLabel.CENTER);
        friendPanel.add(friendInput);

        friendPanel.setBorder(BorderFactory.createEmptyBorder(10, 100, 10, 100) );

        JPanel buttonPanel = new JPanel();
        JButton challengeButton = new JButton("SFIDA");
        JButton homeButton = new JButton("HOME");

        challengeButton.addActionListener(this);
        homeButton.addActionListener(this);

        buttonPanel.add(challengeButton);
        buttonPanel.add(homeButton);

        JPanel answerPanel = new JPanel();

        answer = new JLabel("",JLabel.CENTER);
        answer.setForeground(Color.BLACK);
        answer.setBackground(Color.WHITE);
        answer.setOpaque(true);

        answerPanel.add(answer);

        setLayout(new GridLayout(4,1,3,3));
        add(titlePanel);
        add(friendPanel);
        add(buttonPanel);
        add(answerPanel);
    }

//    public static Challenge getInstance(JFrame window, SocketChannel client, String nickname){
//        if(challenge ==  null) challenge = new Challenge(window,client,nickname);
////        else{
////            this.window = window; //TODO forse inutili
////            this.client = client;
////            this.nickname = nickname;
////        }
//
//        return challenge;
//    }


    public boolean isChallenge() {
        return sfida_in_corso;
    }

    public void setChallenge(){
        this.sfida_in_corso = false;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if(actionEvent.getActionCommand().equals("HOME")){
            HomePage homePage = new HomePage(nickname,window,client);
            window.setContentPane(homePage);
            window.validate();
        }
        else if(actionEvent.getActionCommand().equals("SFIDA")){
            String friendNick = friendInput.getText();

            if(friendNick.equals("")){
                answer.setText("Inserire il nickname dell'amico che si vuole sfidare");
                return;
            }

            String request = "CHALLENGE\n"+nickname+"\n"+friendNick+"\n";
            ByteBuffer buffer = ByteBuffer.allocate(request.length());

            buffer.put(request.getBytes());
            buffer.flip();

            while (buffer.hasRemaining()) {
                try {
                    client.write(buffer);
                } catch (Exception e) {
                    System.out.println("[ERROR] Errore scrittura del buffer nella socket del server (CHALLENGE)");
                    answer.setText("Errore di connessione col server");
                    return;
                }
            }

            buffer = ByteBuffer.allocate(BUF_SIZE);

            try {
                int read = client.read(buffer);

                if(read == -1){
                    System.out.println("[ERROR] Errore lettura della socket del server (CHALLENGE)");
                    answer.setText("Impossibile comunicare col server");
                    return;
                }
                else{
                    String aux[] = (new String(buffer.array())).split("\n");
                    System.out.println("[RESPONSE] "+aux[1]);

                    if(aux[0].equals("KO")){
                        answer.setText(aux[1]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
