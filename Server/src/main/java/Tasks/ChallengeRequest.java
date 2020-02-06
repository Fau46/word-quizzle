package Tasks;

import User.User;
import Server.Con;
import Costanti.Costanti;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ChallengeRequest implements Runnable, Costanti {
    private User user, friend;
    private Selector serverSelector; //Selector principale del server
    private SelectionKey userKey, friendKey;


    private SelectionKey newFriendKey;
    private boolean stopSelector = false; //Flag che mi fa eventualmente terminare il selector temporaneo di sfida

    public ChallengeRequest(User user, User friend, SelectionKey userKey, SelectionKey friendKey, Selector selector){
        this.user = user;
        this.friend = friend;
        this.userKey = userKey;
        this.friendKey = friendKey;
        this.serverSelector = selector;
    }

    @Override
    public void run() {
            sendChallengeRequest(); //Invio la richiesta di sfida
            Selector selector = registerFriendKey(); //Registro la key di friend sul selettore temporaneo di sfida
            if(selector != null) readResponse(selector); //Leggo la risposta di friend
    }


    //Metodo che si occupa di inviare la richiesta di sfida a friend
    private void sendChallengeRequest(){
        InetAddress address;
        DatagramSocket datagramSocket;
        DatagramPacket datagramPacket;

        try {
            datagramSocket = new DatagramSocket();
            address = InetAddress.getByName(HOSTNAME);

            String request = "CHALLENGE\n"+user.getNickname()+"\n"+(SELECTOR_TIMEOUT/1000)+"\n"; //Invio la richiesta indicando quanto tempo il server attende per la risposta (intervallo di tempo T1)
            byte[] byteRequest = request.getBytes();

            int friendSocketPort = ((SocketChannel) friendKey.channel()).socket().getPort(); //Reperisco la porta della socket di friend dalla sua key

            datagramPacket = new DatagramPacket(byteRequest,byteRequest.length,address,friendSocketPort); //Creo il pacchetto da spedire
            datagramSocket.send(datagramPacket); //Invio il pacchetto di richiesta di sfida a friend
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //Metodo che si occupa di registrare friendKey di friend sul selettore temporaneo di sfida
    private Selector registerFriendKey(){
        try {
            Selector selector = Selector.open(); //Apro un selettore per la sfida

            Con keyAttachmentFriend = (Con) this.friendKey.attachment();
            this.friendKey.interestOps(0); //resetto l'interestop della chiave di friend registrata nel selettore principale

            SocketChannel friendSocket = (SocketChannel) this.friendKey.channel();
            newFriendKey = friendSocket.register(selector, SelectionKey.OP_READ); //Registro la key di friend sul selettore temporaneo di sfida
            keyAttachmentFriend.response = "Risposta non ancora data"; //Indico che non ho ancora ricevuto risposta da friend
            newFriendKey.attach(keyAttachmentFriend);

            return selector;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    //Metodo che si occupa di leggere la risposta di friend se arriva prima del timeout, termina altrimenti mandando un KO
    private void readResponse(Selector selector){
        boolean receivedResponse = false; //Flag che mi indica se ho ricevuto la risposta da friend

        do{
            try {

                if(receivedResponse) selector.select(TIMER);
                else selector.select(SELECTOR_TIMEOUT);

            } catch (IOException e) {
                System.out.println("[ERROR] Errore nel selettore");
                return;
            }

            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();

            while (iterator.hasNext()) {
                if (!receivedResponse) receivedResponse = true; //Setto il flag che mi indica l'avvenuta risposta di friend

                SelectionKey key = iterator.next();
                try {
                    if (key.isReadable()) {
                        this.Readable(key, iterator);
                    }
                } catch (IOException e) {
//                  Gestisco la chiusura del canale
                    Con keyAttachment = (Con) key.attachment();

                    deregisterFriendKey(key);

                    if(keyAttachment.response.equals("Risposta non ancora data")){ //Caso in cui friend si disconnette ancora prima di dare una risposta
                        disconnectedFriend("KO\n"+keyAttachment.nickname+" si e' disconnesso\n");
                    }

                    stopSelector = true;
                }
            }
        }while (receivedResponse && !stopSelector);

        if(!receivedResponse){ //Controllo se il timer è scaduto e friend non ha dato una risposta
            negativeResponse("KO\nTempo scaduto");
        }
    }


    private void Readable(SelectionKey key, Iterator<SelectionKey> iterator) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        byte[] byteBuffer = new byte[BUF_SIZE];
        ByteBuffer intput = ByteBuffer.wrap(byteBuffer);
        Con keyAttachment = (Con) key.attachment();
        StringBuilder requestBuilder;

        String request = keyAttachment.request;
        if(request ==  null) requestBuilder = new StringBuilder();
        else requestBuilder = new StringBuilder(request);
        int read = client.read(intput); //Leggo dalla socket

        if (read == -1) throw new IOException("Canale chiuso"); //Mi accerto che il canale non sia chiuso
        else if(read == 0){ //Se ho finito di leggere parso la request
            parser(key);
            iterator.remove();
        }
        else{ //Allego ciò che ho letto alla request della key
            String string = new String(byteBuffer,0,read);
            requestBuilder.append(string);
            keyAttachment.request = requestBuilder.toString();
        }
    }


    private void Write(String string, SelectionKey key){
        ByteBuffer buffer = ByteBuffer.allocate(string.length());

        buffer.put(string.getBytes());
        buffer.flip();

        while (buffer.hasRemaining()){
            try {
                ((SocketChannel)key.channel()).write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Parser che si occupa di interpretare l'operazione richiesta
    private void parser(SelectionKey key) {
        Con keyAttachment = (Con) key.attachment();
        String[] aux = keyAttachment.request.split("\n");
        stopSelector = true;

        if(aux[0].equals("REFUSED")){
            if(!aux[1].equals("Utente occupato")) Write("OK\nOK richiesta rifiutata\n", key); //Rispondo a friend

            negativeResponse("KO\n"+aux[1]); //Rispondo a user
        }
        else if(aux[0].equals("ACCEPTED")){
            if(!userKey.isValid()){ //Controllo che user non abbia chiuso la connessione
                String string = "KO\n"+((Con)userKey.attachment()).nickname+" ha abbandonato\n";
                Write(string,key);

                deregisterFriendKey(key);
                setFlag(); //Resetto i flag necessari
            }
            else{
                deregisterFriendKey(key);
                Challenge challenge = new Challenge(user, friend, userKey,friendKey,serverSelector); //TODO forse problema con friendKey
                challenge.startChallenge();
            }
        }
    }


    //Funzione che ritorna una risposta negativa a user
    private void negativeResponse(String response) {
        Con keyAttachment = (Con) userKey.attachment();

        keyAttachment.response =  response; //Allego la risposta negativa a user

        Con keyAttachmentFriend = deregisterFriendKey(newFriendKey); //Registro nuovamente friend sul selettore principale

        keyAttachmentFriend.request = null;
        keyAttachmentFriend.response = null;

        try{
            userKey.interestOps(SelectionKey.OP_WRITE);
            setFlag(); //Resetto i flag necessari
        }catch (Exception e){
            e.printStackTrace();
            setFlag();
            return;
        }
    }


    //Metodo che si occupa di segnalare la risposta a user
    private void disconnectedFriend(String response) {
        Con keyAttachment = (Con) userKey.attachment();

        keyAttachment.response =  response; //Allego la risposta negativa a user

        try{
            userKey.interestOps(SelectionKey.OP_WRITE);
            setFlag(); //Resetto i flag necessari
        }catch (Exception e){
            e.printStackTrace();
            setFlag();
            return;
        }
    }


    //Metodo che si occupa di resettare diversi flag per la chiusura
    private void setFlag(){
        Con keyAttachment = (Con) userKey.attachment();
        Con keyAttachmentFriend = (Con) friendKey.attachment();

        keyAttachment.challenge = false;
        keyAttachmentFriend.challenge = false;

        user.decrementUse();
        friend.decrementUse();
    }

//    Metodo che si occupa di registrare nuovamente la chiave key sul selettore principale
    private Con deregisterFriendKey(SelectionKey key){
        Con keyAttachment = (Con) key.attachment();

        try {
            key.interestOps(0);

            SocketChannel keySocket = (SocketChannel) key.channel();

            SelectionKey key1 = keySocket.register(this.serverSelector, SelectionKey.OP_READ);
            key1.attach(keyAttachment);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return keyAttachment;
    }
}

