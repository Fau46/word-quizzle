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
    private int BUF_SIZE = 512;
    private boolean SHUTDOWN = false;
    private static UDPListener udpListener;


    private UDPListener(int port, JFrame window, SocketChannel client, String nickname){
        this.window = window;
        this.port = port;
        this.challengeFlag = ChallengeFlag.getInstance();
        this.client = client;
        this.nickname = nickname;
    }


    public static UDPListener getInstance(int port, JFrame window, SocketChannel client, String nickname){
        if(udpListener == null) udpListener = new UDPListener(port,window,client,nickname);
        return udpListener;
    }


    public static UDPListener getInstance(){
        return udpListener;
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

        byte[] bufferByte = new byte[BUF_SIZE];
        DatagramPacket datagramPacket = new DatagramPacket(bufferByte,bufferByte.length);
        String request = null;

        while (!SHUTDOWN){
            try {
                datagramSocket.receive(datagramPacket); //Leggo la richiesta di sfida
                request = new String(datagramPacket.getData(),0,datagramPacket.getLength(),"UTF-8");
            } catch (SocketException e){
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }


            String[] aux = request.split("\n");
            String[] buttons = {"ACCETTA", "RIFIUTA"};

            if(challengeFlag.flag.intValue() == 0){ //Se l'utente non è impegnato a effettuare una sfida gli mando la richiesta
                challengeFlag.flag.set(1);
                int choose = JOptionPane.showOptionDialog(window, aux[0]+" ti vuole sfidare\nHai "+aux[1]+" secondi per accettare la sfida", "Sfida",JOptionPane.INFORMATION_MESSAGE, JOptionPane.PLAIN_MESSAGE, null, buttons,null);

                if(choose == 0){ //Se l'utente ha scelto 'ACCETTA'
//
                    String response = "OK\nSfida accettata\n";

                    String[] responseArray = ReadWrite(response);

                    if(responseArray != null){
                        if(responseArray[0].equals("KO")){
                            JOptionPane.showMessageDialog(window, responseArray[1], "Error", JOptionPane.ERROR_MESSAGE);
                        }
                        else if(responseArray[0].equals("OK")){
                            Challenge challenge = new Challenge(window,client,nickname);
                            window.setContentPane(challenge);
                            window.validate();
                        }
                    }
                }
                else {
                    String response = "KO\nSfida rifiutata\n";
                    ReadWrite(response);

                    challengeFlag.flag.set(0);
                }
            }
        }
        System.out.println("[CLOSED] Thread UDP closed");
    }


    private String[] ReadWrite(String response){
        ByteBuffer buffer = ByteBuffer.allocate(response.length());

        buffer.put(response.getBytes());
        buffer.flip();

        while (buffer.hasRemaining()){
            try {
                client.write(buffer);
            } catch (IOException e) {
                System.out.println("[ERROR] Errore scrittura del buffer nella socket del server (UDP Thread)");
                this.serverError();
                break;
            }
        }

        buffer = ByteBuffer.allocate(512);

        try {
            int read = client.read(buffer);

            if(read == - 1){//Se riscontro un errore nella lettura
                System.out.println("[ERROR] Errore lettura della socket del server (UDP Thread)");
                this.serverError();
            }
            else { //se la lettura è andata a buon fine
                String[] aux1 = (new String(buffer.array())).split("\n");
                System.out.println("[RESPONSE] " + aux1[1]);
                return aux1;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return  null;
    }


    public void serverError(){
        shutdownAndClear();

        JOptionPane.showMessageDialog(window, "Impossibile comunicare col server.\n Verrai disconnesso", "Server error", JOptionPane.ERROR_MESSAGE);

        StartGUI startGUI = new StartGUI(window);
        window.setContentPane(startGUI);
        window.validate();
    }


    public void shutdownAndClear(){
        datagramSocket.close();
        challengeFlag.flag.set(0);
        SHUTDOWN = true;
        udpListener = null;
    }

}
