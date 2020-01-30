import javax.swing.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class UDPListener implements Runnable,TCPConnection{
    private int port;
    private DatagramSocket datagramSocket;
    private JFrame window;
    private ChallengeFlag challengeFlag;
    private SocketChannel client;
    private String nickname;

    public UDPListener(int port, JFrame window, SocketChannel client, String nickname){
        this.window = window;
        this.port = port;
        this.challengeFlag = ChallengeFlag.getInstance();
        this.client = client;
        this.nickname = nickname;
    }

    @Override
    public void run() {
        try {
            datagramSocket = new DatagramSocket(port);
            System.out.println("[OK] Listener UDP in ascolto");
        } catch (SocketException e) {
            System.out.println("[ERROR] Errore nella configurazione del listener UDP");
            e.printStackTrace();
        }

        byte[] bufferByte = new byte[512];
        DatagramPacket datagramPacket = new DatagramPacket(bufferByte,bufferByte.length);
        String request = null;

        while (true){
            try {
                datagramSocket.receive(datagramPacket); //Leggo la richiesta di sfida
                request = new String(datagramPacket.getData(),0,datagramPacket.getLength(),"UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }

            String[] aux = request.split("\n");
            String[] buttons = {"ACCETTA", "RIFIUTA"};

            if(challengeFlag.flag.intValue() == 0){ //Se l'utente non è impegnato a effettuare una sfida gli mando la richiesta
                challengeFlag.flag.set(1);
                int choose = JOptionPane.showOptionDialog(window, aux[0]+" ti vuole sfidare", "Sfida",JOptionPane.INFORMATION_MESSAGE, JOptionPane.PLAIN_MESSAGE, null, buttons,null);

                if(choose == 0){
                    Challenge challenge = new Challenge(window,client,nickname);
//                    challenge.printChallenge();
                    window.setContentPane(challenge);
                    window.validate();

                }
                else{
                    String response = "KO\nSfida rifiutata\n";
                    ByteBuffer buffer = ByteBuffer.allocate(response.length());

                    buffer.put(response.getBytes());
                    buffer.flip();

                    while (buffer.hasRemaining()){
                        try {
                            client.write(buffer);
                        } catch (IOException e) {
                            System.out.println("[ERROR] Errore scrittura del buffer nella socket del server (SHOWSCORE)");
                            JOptionPane.showMessageDialog(window, "Impossibile comunicare col server.\n Verrai disconnesso", "Server error", JOptionPane.ERROR_MESSAGE);
                            StartGUI startGUI = new StartGUI(window);
                            window.setContentPane(startGUI);
                            window.validate();
                            break;
                        }
                    }



                    buffer = ByteBuffer.allocate(512);

                    try {

                        int read = client.read(buffer);

                        if(read == - 1){//Se riscontro un errore nella lettura
                            System.out.println("[ERROR] Errore lettura della socket del server (ADDFRIEND)");
//                            this.serverError();

                        }
                        else { //se la lettura è andata a buon fine
                            String aux1[] = (new String(buffer.array())).split("\n");
                            if(aux1.length!=0) System.out.println("[RESPONSE] " + aux1[1]);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                challengeFlag.flag.set(0);
            }
        }
    }
}
