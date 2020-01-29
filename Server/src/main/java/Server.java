import Tasks.*;
import User.*;
import Server.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

public class Server {
    private Selector selector;
    private ThreadPoolExecutor executor;
    private UserDispatcher userDispatcher;
    private int BUF_SIZE = 512;
    private int SELECTOR_TIMEOUT = 100;
    private ConcurrentHashMap<String, User> mapUser;


    public Server(Selector selector, ThreadPoolExecutor executor){
        this.selector = selector;
        this.executor = executor;
        this.userDispatcher = UserDispatcher.getIstance();
        this.mapUser = new ConcurrentHashMap<>();
    }

    //Avvio selettore server
    public void run() {
        while (true) {
            try {
                selector.select(SELECTOR_TIMEOUT);
//                selector.selectNow();
            } catch (IOException e) {
                System.out.println("[ERROR] Errore nel selettore");
                return;
            }

            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                try {
                    if (key.isAcceptable()) {
                        this.Acceptable(key);
                        iterator.remove();
                    } else if (key.isReadable()) {
                        this.Readable(key,iterator);
                    }
                    else if(key.isWritable()){
                        this.Writable(key);
                        iterator.remove();
                    }
                } catch (IOException e) {
                    key.cancel();
                    try {
                        //Gestisco la chiusura del canale
                        Con keyAttachment = (Con) key.attachment();
                        String nick = keyAttachment.nickname;
                        if(nick != null) {
                            User user = mapUser.get(nick);
                            user.decrementUse(); //Decremento use in user
                            user.setPort(0);
                            mapUser.remove(nick); //Rimuovo dagli user online
                        }

                        System.out.println("[CLOSED CLIENT]: (" +nick+") "+ ((SocketChannel) key.channel()).getRemoteAddress());

                        key.channel().close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }

        }
    }


    private void Acceptable(SelectionKey key) throws IOException{
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();

        System.out.println("[ACCEPTED CLIENT]:"+client.getRemoteAddress());

        client.configureBlocking(false);
        SelectionKey key1 = client.register(selector, SelectionKey.OP_READ);
        key1.attach(new Con());
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
        int read=client.read(intput); //Leggo dalla socket

        if (read ==- 1) throw new IOException("Canale chiuso"); //Mi accerto che il canale non sia chiuso
        else if(read == 0){ //Se ho finito di leggere parso la request TODO e se non sono arrivati tutti i pacchetti? (suggerimento nicola)
            parser(key);
            iterator.remove();
        }
        else{ //Allego ciò che ho letto alla request della key
            String string = new String(byteBuffer,0,read);
            requestBuilder.append(string);
            keyAttachment.request = requestBuilder.toString();
        }
    }


    private void Writable(SelectionKey key) throws IOException{
        SocketChannel client = (SocketChannel) key.channel();
        Con keyAttachment = (Con) key.attachment();
        String string = keyAttachment.response;
        ByteBuffer buffer = ByteBuffer.allocate(string.length());

        buffer.put(string.getBytes());
        buffer.flip();

        if(keyAttachment.lenght!=0){ //Controllo se devo passare prima la lunghezza
            String responseLen = keyAttachment.lenght.toString();
            ByteBuffer auxBuffer = ByteBuffer.allocate(responseLen.length());

            auxBuffer.put(responseLen.getBytes());
            auxBuffer.flip();

            while (auxBuffer.hasRemaining()){
                client.write(auxBuffer);
            }

            keyAttachment.lenght = 0;
        }

        //Scrivo la risposta al client
        while (buffer.hasRemaining()){
            client.write(buffer);
        }

        if(!keyAttachment.logout){
            keyAttachment.response = null;
            keyAttachment.request = null;
            key.interestOps(SelectionKey.OP_READ);
        }
        else { //se risulta che logout=true sollevo l'eccezione così faccio chiudere la socket
            throw new IOException("Logout");
        }

    }


    //TODO mettere il caso in cui l'operazione è malformata
    private void parser(SelectionKey key) throws IOException{
        Con keyAttachment = (Con) key.attachment();
        String[] aux = keyAttachment.request.split("\n"); //Splitto la request

        String op = aux[0];

        if(op.equals("LOGIN")){
            login(aux,key);
            key.interestOps(SelectionKey.OP_WRITE);
        }
        else if(op.equals("LOGOUT")){
            logout(aux,key);
            key.interestOps(SelectionKey.OP_WRITE);
        }
        else if(op.equals("ADDFRIEND")){
            addFriend(aux,key);
        }
        else if(op.equals("SHOWFRIENDS")){
            showFriends(aux,key);
        }
        else if(op.equals("SHOWSCORE")){
            showScore(aux,key);
            key.interestOps(SelectionKey.OP_WRITE);
        }
        else if(op.equals("SHOWRANK")){
            showRank(aux,key);
        }
        else if(op.equals("CHALLENGE")){
            challenge(aux,key);
        }
    }


    //Funzione che effettua il login
    private void login(String[] aux, SelectionKey key) throws IOException { //TODO forse da delegare a un thread
        Con keyAttachment = (Con) key.attachment();
        keyAttachment.nickname = null;

        String nickname = aux[1];
        String password = aux[2];
        Integer port = Integer.valueOf(aux[3]);

        User user = null;

        if (mapUser.get(nickname) == null) { //Controllo che nickname non sia online
            user = userDispatcher.getUser(nickname); //Chiedo al dispatcher l'oggetto relativo a nickname

            if (user == null){
                keyAttachment.response = "KO\nNickname non presente";} //TODO non ha senso visto che dico 'nick o pwd errate'
            else if (user.getNickname().equals(nickname) && user.getPassword().equals(password)){
                System.out.println("[LOGIN] Inserisco "+nickname+" nella mapUser");
                mapUser.put(nickname,user);
                keyAttachment.nickname = nickname;
                keyAttachment.response = "OK\nLogin effettuato";
                user.incrementUse();
                user.setPort(port);
            }
            else keyAttachment.response = "KO\nNickname o password errate";
        } else keyAttachment.response = "KO\nUtente gia' connesso";

    }


    private void logout(String[] aux, SelectionKey key) throws IOException {
        String nickname = aux[1];

        Con keyAttachment = (Con) key.attachment();

        if(mapUser.get(nickname) == null) keyAttachment.response = "KO\nUtente non in linea"; //TODO forse inutile
        else {
            keyAttachment.response = "OK\nUtente disconnesso";
            keyAttachment.logout = true;
        }
    }


    private void addFriend(String[] aux, SelectionKey key){
        String nickname = aux[1];
        String nickFriend = aux[2];
        Con keyAttachment = (Con) key.attachment();
        User user = mapUser.get(nickname);
        User friend;

        if((friend = mapUser.get(nickFriend)) == null){ //se non trovo friend tra gli utenti online
            friend = userDispatcher.getUser(nickFriend); //chiedo friend al dispatcher
        }

        if(user == null){
            keyAttachment.response = "KO\nUtente non online\n";
            key.interestOps(SelectionKey.OP_WRITE);
        }
        else if(friend == null){
            keyAttachment.response = "KO\nIl nickname "+nickFriend+" non valido\n";
            key.interestOps(SelectionKey.OP_WRITE);
        }
        else{
            user.incrementUse();
            friend.incrementUse();
            AddFriend task = new AddFriend(user,friend,key);
            executor.execute(task);
        }

    }


    private void showFriends(String[] aux, SelectionKey key) {
        User user = mapUser.get(aux[1]);

        user.incrementUse();
        ShowFriends task = new ShowFriends(user, key);
        executor.execute(task);
    }


    private void showScore(String[] aux, SelectionKey key) {
        User user = mapUser.get(aux[1]);
        Con keyAttachment = (Con) key.attachment();

        keyAttachment.response = "OK\n"+user.getScore().toString()+"\n";
    }


    private void showRank(String[] aux, SelectionKey key) {
        User user = mapUser.get(aux[1]);

        user.incrementUse();
        ShowRank task = new ShowRank(user,key);
        executor.execute(task);
    }

    private void challenge(String[] aux, SelectionKey key) {
        Con keyAttachment = (Con) key.attachment();
        User user = mapUser.get(aux[1]);
        String friendNick = aux[2];
        User friend;

        if(!user.getNickname().equals(friendNick)){
            if((friend = mapUser.get(friendNick)) != null){
                user.incrementUse();;
                friend.incrementUse();

                Challenge task = new Challenge(user,friend,key);
                executor.execute(task);
            }
            else{
                keyAttachment.response = "KO\n"+friendNick+" non online";
                key.interestOps(SelectionKey.OP_WRITE);
            }
        }
        else{
            keyAttachment.response = "KO\nNon puoi sfidarti";
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }
}
