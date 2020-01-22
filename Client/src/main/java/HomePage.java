import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class HomePage extends JPanel implements ActionListener{
    private JFrame window;
    private JPanel buttonPanel;
    private SocketChannel client;
    private String nickname;

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

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1,4));

        JButton logout = new JButton("LOGOUT");

        logout.addActionListener(this);

        buttonPanel.add(logout);

        setLayout(new GridLayout(4,1,3,3));
        add(welcomePanel);
        add(nickPanel);
        add(buttonPanel);


    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if(actionEvent.getActionCommand().equals("LOGOUT")){
            String request = "LOUT\n"+nickname+"\n";
            ByteBuffer buffer = ByteBuffer.allocate(request.length());

            buffer.put(request.getBytes());
            buffer.flip();

            while (buffer.hasRemaining()){
                try {
                    client.write(buffer);
                } catch (IOException e) {
                    System.out.println("[ERROR] Errore scrittura del buffer nella socket del server"); //TODO mettere un messaggio
                    return;
                }
            }

            StartGUI startGUI = new StartGUI(window);
            window.setContentPane(startGUI);
            window.validate();
        }
        else {
            JButton prova = new JButton("TEST");
            buttonPanel.add(prova);
            window.validate();
        }
    }
}
