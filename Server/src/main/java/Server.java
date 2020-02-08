import Costanti.Costanti;
import Tasks.*;
import User.*;
import Server.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

public class Server implements Costanti {
    private Selector selector;
    private ThreadPoolExecutor executor;

    private UserDispatcher userDispatcher;
    private ConcurrentHashMap<String, User> mapUser;
    private ConcurrentHashMap<String, SelectionKey> mapKey;


    public Server(Selector selector, ThreadPoolExecutor executor){
        this.selector = selector;
        this.executor = executor;
        this.userDispatcher = UserDispatcher.getIstance();
        this.mapUser = new ConcurrentHashMap<>();
        this.mapKey = new ConcurrentHashMap<>();
    }

    //Avvio selettore server
    public void run() {
        while (true) {
            try {
                selector.select(TIMER);
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
                        ConKey keyAttachment = (ConKey) key.attachment();
                        String nick = keyAttachment.nickname;
                        if(nick != null) {
                            User user = mapUser.get(nick);
                            user.decrementUse(); //Decremento use in user
//                            user.setPort(0);
                            mapUser.remove(nick); //Rimuovo dagli user online
                            mapKey.remove(nick); //Rimuovo la sua chiave
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
        key1.attach(new ConKey());
    }


    private void Readable(SelectionKey key, Iterator<SelectionKey> iterator) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        byte[] byteBuffer = new byte[BUF_SIZE];
        ByteBuffer intput = ByteBuffer.wrap(byteBuffer);
        ConKey keyAttachment = (ConKey) key.attachment();
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
        ConKey keyAttachment = (ConKey) key.attachment();
        String string = keyAttachment.response.length()+"\n"+keyAttachment.response;
        ByteBuffer buffer = ByteBuffer.allocate(string.length());

        buffer.put(string.getBytes());
        buffer.flip();

//        if(keyAttachment.lenght!=0){ //Controllo se devo passare prima la lunghezza
//            String responseLen = keyAttachment.lenght.toString()+"\n";
//            ByteBuffer auxBuffer = ByteBuffer.allocate(responseLen.length());
//
//            auxBuffer.put(responseLen.getBytes());
//            auxBuffer.flip();
//
//            while (auxBuffer.hasRemaining()){
//                client.write(auxBuffer);
//            }

//            keyAttachment.lenght = 0;
//        }

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


    private void parser(SelectionKey key) throws IOException{
        ConKey keyAttachment = (ConKey) key.attachment();
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
        else{
            badRequest(aux,key);
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }


    //Funzione che effettua il login
    private void login(String[] aux, SelectionKey key) throws IOException { //TODO forse da delegare a un thread
        ConKey keyAttachment = (ConKey) key.attachment();
        keyAttachment.nickname = null;

        String nickname = aux[1];
        String password = aux[2];

        User user = null;

        if (mapUser.get(nickname) == null) { //Controllo che nickname non sia online
            user = userDispatcher.getUser(nickname); //Chiedo al dispatcher l'oggetto relativo a nickname

            if (user == null){
                keyAttachment.response = "KO\nNickname non presente";} //TODO non ha senso visto che dico 'nick o pwd errate'
            else if (user.getNickname().equals(nickname) && user.getPassword().equals(password)){
                System.out.println("[LOGIN] "+nickname);
                mapUser.put(nickname,user);
                mapKey.put(nickname,key);
                keyAttachment.nickname = nickname;
                keyAttachment.response = "OK\nLogin effettuato";
                user.incrementUse();
//                user.setPort(port);
            }
            else keyAttachment.response = "KO\nNickname o password errate";
        } else keyAttachment.response = "KO\nUtente gia' connesso";

    }


    private void logout(String[] aux, SelectionKey key) throws IOException {
        String nickname = aux[1];

        ConKey keyAttachment = (ConKey) key.attachment();

        if(mapUser.get(nickname) == null) keyAttachment.response = "KO\nUtente non in linea"; //TODO forse inutile
        else {
            keyAttachment.response = "OK\nUtente disconnesso";
            keyAttachment.logout = true;
        }
    }


    private void addFriend(String[] aux, SelectionKey key){
        String nickname = aux[1];
        String nickFriend = aux[2];
        ConKey keyAttachment = (ConKey) key.attachment();
        User user = mapUser.get(nickname);
        User friend;

        System.out.println("[ADD FRIEND] NICKNAME: "+nickname+" | FRIEND NICKNAME: "+nickFriend);

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
        ConKey keyAttachment = (ConKey) key.attachment();

        System.out.println("[SHOW FRIENDS] "+user.getNickname()+" ("+keyAttachment.nickname+")");

        user.incrementUse();
        ShowFriends task = new ShowFriends(user, key);
        executor.execute(task);
    }


    private void showScore(String[] aux, SelectionKey key) {
        User user = mapUser.get(aux[1]);
        ConKey keyAttachment = (ConKey) key.attachment();

        System.out.println("[SHOW SCORE] "+user.getNickname()+" ("+keyAttachment.nickname+")");

        keyAttachment.response = "OK\n"+user.getScore().toString()+"\n";
    }


    private void showRank(String[] aux, SelectionKey key) {
        User user = mapUser.get(aux[1]);
        ConKey keyAttachment = (ConKey) key.attachment();

        System.out.println("[SHOW RANK] "+user.getNickname()+" ("+keyAttachment.nickname+")");

        user.incrementUse();
        ShowRank task = new ShowRank(user,key);
        executor.execute(task);
    }


    private void challenge(String[] aux, SelectionKey key) {
        ConKey keyAttachment = (ConKey) key.attachment();
        User user = mapUser.get(aux[1]);
        String friendNick = aux[2];
        User friend;

        System.out.println("[CHALLENGE] nickname "+user.getNickname()+" friend nickname: "+friendNick+" ("+keyAttachment.nickname+")");

        if(!user.getNickname().equals(friendNick)){ //Controllo che friend non abbia lo stesso nickname di user
            if((friend = mapUser.get(friendNick)) != null){ //Controllo che friend sia online
                if((user.getFriends()).contains(friendNick)){ //Controllo che friend sia amico di user

                    SelectionKey keyFriend = mapKey.get(friendNick); //Prendo la chiave di friend
                    ConKey keyAttachmentFriend = (ConKey) keyFriend.attachment();

                    if(!keyAttachmentFriend.challenge) { //Controllo che friend non sia già occupato con un'altra sfida
                        user.incrementUse();
                        friend.incrementUse();

                        keyAttachment.challenge = true;
                        keyAttachmentFriend.challenge = true;

                        ChallengeRequest task = new ChallengeRequest(user,friend,key,keyFriend,selector);
                        executor.execute(task);
                    }
                    else{
                        keyAttachment.response = "KO\n"+friendNick+" e' impegnato in un'altra sfida\n";
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                }
                else{
                    keyAttachment.response = "KO\n"+friendNick+" non e' tuo amico";
                    key.interestOps(SelectionKey.OP_WRITE);
                }
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


    private void badRequest(String[] aux, SelectionKey key) {
        ConKey keyAttachment = (ConKey) key.attachment();

        System.out.println("[BAD REQUEST] "+aux[1]+" ("+keyAttachment.nickname+")");

        if(aux[1].equals("Sfida accettata")){
            keyAttachment.response = "KO\nTempo scaduto\n";
        }
        else{
            keyAttachment.response = "KO\nErrore operazione richiesta\n";
        }
    }
}
