package Tasks;

import Server.Con;
import Server.DictionaryDispatcher;
import User.User;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jdk.internal.jline.internal.Urls;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

public class ChallengeRequest implements Runnable {
    private User user, friend;
    private SelectionKey userKey, friendKey;
    private int BUF_SIZE = 256;
    private int SELECTOR_TIMEOUT = 5000;
    private Selector serverSelector; //Selector principale del server
    private SelectionKey newFriendKey;
    private boolean stopSelector = false; //Flag che mi fa eventualmente terminare il selector di sfida

    public ChallengeRequest(User user, User friend, SelectionKey userKey, SelectionKey friendKey, Selector selector){
        this.user = user;
        this.friend = friend;
        this.userKey = userKey;
        this.friendKey = friendKey;
        this.serverSelector = selector;
    }

    @Override
    public void run() {
            sendChallengeRequest();
            registerFriendKey();
    }

    //Procedura che si occupa di inviare la richiesta di sfida a friend
    private void sendChallengeRequest(){
        DatagramSocket datagramSocket;
        InetAddress address;
        DatagramPacket datagramPacket;

        try {
            datagramSocket = new DatagramSocket();
            address = InetAddress.getByName("localhost");//TODO mettere costante

            String request = user.getNickname()+"\n"+(SELECTOR_TIMEOUT/1000)+"\n";
            byte[] byteRequest = request.getBytes();

            int friendSocketPort = ((SocketChannel) friendKey.channel()).socket().getPort(); //Prendo la porta della socket di friend
            datagramPacket = new DatagramPacket(byteRequest,byteRequest.length,address,friendSocketPort);
            datagramSocket.send(datagramPacket); //Invio il pacchetto di richiesta di sfida a friend
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void registerFriendKey(){
        try {
            Selector selector = Selector.open();

            Con keyAttachmentFriend = (Con) this.friendKey.attachment();
            this.friendKey.interestOps(0); //resetto l'interestop della chiave di friend registrata nel selettore principale

            SocketChannel friendSocket = (SocketChannel) this.friendKey.channel();
            newFriendKey = friendSocket.register(selector, SelectionKey.OP_READ); //registro la key di friend col nuovo selettore
            keyAttachmentFriend.response = "Risposta non ancora data";
            newFriendKey.attach(keyAttachmentFriend);

            readResponse(selector);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void readResponse(Selector selector){
        boolean receivedResponse = false;

        do{
            try {

                if(receivedResponse) selector.select(100);
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
//                      Gestisco la chiusura del canale
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

        if(aux[0].equals("KO")){

            //Rispondo al thread UDP di friend
            Write("OK\nOK richiesta rifiutata\n", key);

            negativeResponse("KO\nSfida non accettata");
        }
        else if(aux[0].equals("OK")){
            if(!userKey.isValid()){ //Controllo che user non abbia chiuso la connessione
                String string = "KO\n"+((Con)userKey.attachment()).nickname+" ha abbandonato\n";
                Write(string,key);
                deregisterFriendKey(key);
            }
            else{
                deregisterFriendKey(key);
//                Map<String,String> dictionary = translateWords();
                Challenge challenge = new Challenge(user, friend, userKey,friendKey,serverSelector); //TODO forse problema con friendKey
                challenge.startChallenge();
            }
        }

    }

    //Funzione che ritorna una risposta negativa a user
    private void negativeResponse(String response) {
        Con keyAttachment = (Con) userKey.attachment();

        keyAttachment.response =  response; //Allego la risposta negativa a user

        Con keyAttachmentFriend = deregisterFriendKey(newFriendKey);

        keyAttachmentFriend.request = null;
        keyAttachmentFriend.response = null;

        try{
            userKey.interestOps(SelectionKey.OP_WRITE);
            user.decrementUse();
            friend.decrementUse();
        }catch (Exception e){
            user.decrementUse();
            friend.decrementUse();
            e.printStackTrace();
            return;
        }
    }

    //Procedura che si occupa di segnalare la risposta a user
    private void disconnectedFriend(String response) {
        Con keyAttachment = (Con) userKey.attachment();

        keyAttachment.response =  response; //Allego la risposta negativa a user

        try{
            userKey.interestOps(SelectionKey.OP_WRITE);
            user.decrementUse();
            friend.decrementUse();
        }catch (Exception e){
            user.decrementUse();
            friend.decrementUse();
            e.printStackTrace();
            return;
        }

    }

//    Procedura che si occupa di registrare nuovamente la chiave key sul selettore principale
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




//    -----------------------AREA DI TEST-----------------------

    private Map<String,String> translateWords(){
        Set<String> set = new TreeSet<>();

        try {
            for(int i=0; i<5; i++){
                String string = test();
                set.add(string);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("LUNGHEZZA set "+set.size());
        traduzione(set);

        return null;
    }

    private void traduzione(Set<String> set) {
        for(String string : set){
            try {
                String req = "https://api.mymemory.translated.net/get?q="+string+"&langpair=it|en";
                String req1 = "https://translate.yandex.net/api/v1.5/tr.json/translate?key=trnsl.1.1.20200201T115631Z.c3b0cdde609dde53.2228d16c158e2da155316068ad1bee64e3af99f5&text="+string+"&lang=it-en";
                URL url = new URL(req);
                URL url1 = new URL(req1);

                Reader reader = new InputStreamReader(url.openStream());
                Reader reader1 = new InputStreamReader(url1.openStream());

                Gson gson = new Gson();

                JsonObject aux = gson.fromJson(reader,JsonObject.class);
                JsonObject aux1 = gson.fromJson(reader1, JsonObject.class);

                JsonArray auxArray = aux.getAsJsonArray("matches");

                for(int i=0; i<auxArray.size(); i++){
                    System.out.println("RESPONSE["+string+"] "+(auxArray.get(i)).getAsJsonObject().get("translation"));
                }
                System.out.println("RESPONSE1["+string+"] "+aux1.toString());

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String test() throws IOException {
        File file = new File("./Server/src/main/resources/words.italian.txt");
        final RandomAccessFile f = new RandomAccessFile(file, "r");
        final long randomLocation = (long) (Math.random() * f.length());
        f.seek(randomLocation);
        f.readLine();
        return f.readLine();
    }

}

