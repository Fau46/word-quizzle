import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

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
        buttonPanel.setLayout(new GridLayout(1,4));

        JButton logout = new JButton("LOGOUT");
        JButton addFriend = new JButton("AGGIUNGI AMICO");

        logout.addActionListener(this);
        addFriend.addActionListener(this);

        buttonPanel.add(logout);
        buttonPanel.add(addFriend);

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
        else{
            addFriend();
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
        String friend = JOptionPane.showInputDialog(window,"Inserisci l'amico che vuoi aggiungere");
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


    private void serverError(){
        JOptionPane.showMessageDialog(window, "Impossibile comunicare col server.\n Verrai disconnesso", "Server error", JOptionPane.ERROR_MESSAGE);
        StartGUI startGUI = new StartGUI(window);
        window.setContentPane(startGUI);
        window.validate();
    }
}
