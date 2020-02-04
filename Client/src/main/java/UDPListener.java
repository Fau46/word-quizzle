import javax.swing.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class UDPListener implements Runnable,TCPConnection{
    private int port;
    private JFrame window;
    private String nickname;
    private SocketChannel client;

    private DatagramSocket datagramSocket;
    private ChallengeFlag challengeFlag;
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
            datagramSocket = new DatagramSocket(port); //Apro la socket UDP
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
//                System.out.println("MI METTO IN ASCOLTO "+nickname); //TODO elimina
                datagramSocket.receive(datagramPacket); //Leggo la richiesta di sfida
                request = new String(datagramPacket.getData(),0,datagramPacket.getLength(),"UTF-8");
                System.out.println("[CHALLENGE REQUEST] "+request);
            } catch (SocketException e){
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }


            String[] aux = request.split("\n");
            String[] buttons = {"ACCETTA", "RIFIUTA"};

            if(!challengeFlag.isOccupied()){ //Se l'utente non è impegnato a effettuare una sfida gli mando la richiesta
                challengeFlag.setFlag();

                int choose = JOptionPane.showOptionDialog(window, aux[0]+" ti vuole sfidare\nHai "+aux[1]+" secondi per accettare la sfida", "Sfida",JOptionPane.INFORMATION_MESSAGE, JOptionPane.PLAIN_MESSAGE, null, buttons,null);

                if(choose == 0){ //Se l'utente ha scelto 'ACCETTA'
                    String response = "OK\nSfida accettata\n";

                    String[] responseArray = ReadWrite(response);

                    if(responseArray != null){
                        if(responseArray[0].equals("KO")){ //Se ricevo una risposta negativa mi rimetto in ascolto
                            JOptionPane.showMessageDialog(window, responseArray[1], "Error", JOptionPane.ERROR_MESSAGE);
                            challengeFlag.resetFlag();
                        }
                        else if(responseArray[0].equals("OK")){
                            Challenge challenge = new Challenge(window,client,nickname);
                            window.setContentPane(challenge);
                            window.validate();
                            challenge.run(); //Avvio la sfida
                        }
                    }
                }
                else {
                    String response = "KO\nSfida rifiutata\n";
                    ReadWrite(response);

                    challengeFlag.resetFlag();
                }
            }
            else{
                String response = "KO\nUtente occupato\n";
                ReadWrite(response);
            }
        }

        System.out.println("[CLOSED] Thread UDP closed");
    }


    //Metodo che si occupa di leggere e scrivere sulla socket.
    //Ritorna un array di stringhe contenente la risposta letta o null in caso di errore
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


    //Metodo che si occupa di resettare i vari flag e ed effettuare la chiusura del thread
    public void shutdownAndClear(){
        datagramSocket.close();
        challengeFlag.resetFlag();
        SHUTDOWN = true;
        udpListener = null;
    }

}
