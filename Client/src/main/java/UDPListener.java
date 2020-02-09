import Costanti.Costanti;

import javax.swing.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class UDPListener implements Runnable,TCPConnection,Costanti {
    private int port;
    private JFrame window;
    private String nickname;
    private SocketChannel client;

    private boolean SHUTDOWN = false;
    private ChallengeFlag challengeFlag;
    private DatagramSocket datagramSocket;
    private static UDPListener udpListener;


    private UDPListener(int port, JFrame window, SocketChannel client, String nickname){
        this.port = port;
        this.client = client;
        this.window = window;
        this.nickname = nickname;
        this.challengeFlag = ChallengeFlag.getInstance();
    }


    //Metodo per ottenere l'istanza della classe
    public static UDPListener getInstance(int port, JFrame window, SocketChannel client, String nickname){
        if(udpListener == null) udpListener = new UDPListener(port,window,client,nickname);
        return udpListener;
    }


    //Metodo per ottenere l'istanza della classe
    public static UDPListener getInstance(){
        return udpListener;
    }


    @Override
    public void run() {
        int SIZE_BUFFER = 512;

        try {
            datagramSocket = new DatagramSocket(port); //Apro la socket UDP
            System.out.println("[OK] Listener UDP in ascolto");
        } catch (SocketException e) {
            System.out.println("[ERROR] Errore nella configurazione del listener UDP");
            e.printStackTrace();
        }

        byte[] bufferByte = new byte[SIZE_BUFFER];
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

            System.out.println("[CHALLENGE REQUEST] User: "+aux[1]);


            if(!challengeFlag.isOccupied()){ //Se l'utente non è impegnato a effettuare una sfida gli faccio comparire la richiesta
                challengeFlag.setFlag();

                int choose = JOptionPane.showOptionDialog(window, aux[1]+" ti vuole sfidare\nHai "+aux[2]+" secondi per accettare la sfida", "Sfida",JOptionPane.INFORMATION_MESSAGE, JOptionPane.PLAIN_MESSAGE, null, buttons,null);

                if(choose == 0){ //Se l'utente ha scelto 'ACCETTA'
                    String response = "ACCEPTED\nSfida accettata\n";

                    String[] responseArray = ReadWrite(response);

                    if(responseArray != null){
                        if(responseArray[0].equals("KO")){ //Se ricevo una risposta negativa mi rimetto in ascolto di nuove richieste di sfida
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
                    String response = "REFUSED\nSfida non accettata\n";
                    ReadWrite(response);

                    challengeFlag.resetFlag();
                }
            }
            else{
                String response = "REFUSED\nUtente occupato\n";
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

        buffer = ByteBuffer.allocate(BUF_SIZE);

        if(!response.contains("Utente occupato")){

            try {
                int read = client.read(buffer);

                if(read == - 1){//Se riscontro un errore nella lettura
                    System.out.println("[ERROR] Errore lettura della socket del server (UDP Thread/1)");
                    this.serverError();
                }
                else { //se la lettura è andata a buon fine
                    String tempString = new String(buffer.array(),0,read); //Inserisco quello che ho letto in una stringa temporanea
                    int indexNewLine = tempString.indexOf('\n');

                    String responseLenString = new String(buffer.array(),0,indexNewLine); //Leggo la lunghezza della stringa
                    StringBuilder responseServer = new StringBuilder(tempString.substring(indexNewLine+1)); //Leggo la risposta del server

                    int responseLen = Integer.parseInt(responseLenString); //Converto la lunghezza in int

                    if(responseLen !=  responseServer.length()){ //Se ho ancora roba da leggere
                        buffer = ByteBuffer.allocate(responseLen);
                        read = client.read(buffer); //leggo la risposta del server


                        if(read == - 1){//Se riscontro un errore nella lettura
                            System.out.println("[ERROR] Errore lettura della socket del server (UDP Thread/2)");
                            serverError();
                            return null;
                        }
                        else{
                            responseServer.append(new String(buffer.array(),0,read)); //Appendo la stringa letta
                        }
                    }

                    String aux1[] = (responseServer.toString().split("\n"));

                    System.out.println("[RESPONSE] " + aux1[1]);
                    return aux1;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return  null;
    }


    //Metodo che mostra la schermata iniziale nel caso ci sia un errore di comunicazione col server
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
