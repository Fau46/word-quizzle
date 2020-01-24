import Tasks.*;
import User.*;
import Server.*;

import java.io.IOException;
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
    private ConcurrentHashMap<String, User> mapUser;

    public Server(Selector selector, ThreadPoolExecutor executor){
        this.selector = selector;
        this.executor = executor;
//        this.dbms = DBMS.getIstance();
        this.userDispatcher = new UserDispatcher();
        this.mapUser = new ConcurrentHashMap<>();
//        this.tempUsers = new ConcurrentHashMap<>();
    }

    //Avvio selettore server
    public void run() {
        while (true) {
            try {
                selector.select(100);
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
                        if(nick != null) mapUser.remove(nick); //Rimuovo dagli user online

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


    private void Writable(SelectionKey key) throws IOException{
        SocketChannel client = (SocketChannel) key.channel();

        Con keyAttachment = (Con) key.attachment();
        String string = keyAttachment.response;
        ByteBuffer buffer = ByteBuffer.allocate(string.length());

        buffer.put(string.getBytes());
        buffer.flip();

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
    }


    //Funzione che effettua il login
    private void login(String[] aux, SelectionKey key) throws IOException {
        Con keyAttachment = (Con) key.attachment();
        keyAttachment.nickname = null;

        String nickname = aux[1];
        String password = aux[2];

        User user = null;

        if (mapUser.get(nickname) == null) { //Controllo che nickname non sia online
            user = userDispatcher.getUser(nickname); //Chiedo al dispatcher l'oggetto relativo a nickname

            if (user == null) keyAttachment.response = "KO\nNickname non presente"; //TODO non ha senso visto che dico 'nick o pwd errate'
            else if (user.getNickname().equals(nickname) && user.getPassword().equals(password)){
                System.out.println("[LOGIN] Inserisco "+nickname+" nella mapUser");
                mapUser.put(nickname,user);
                keyAttachment.nickname = nickname;
                keyAttachment.response = "OK\nLogin effettuato";
            }
            else keyAttachment.response = "KO\nNickname o password errate";
        } else keyAttachment.response = "KO\nUtente gia' connesso";

    }


    private void logout(String[] aux, SelectionKey key) throws IOException {
        String nickname = aux[1];

        Con keyAttachment = (Con) key.attachment();

        if(mapUser.get(nickname) == null) keyAttachment.response = "KO\nUtente non in linea";
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
        }
        else if(friend == null){
            keyAttachment.response = "KO\nIl nickname "+nickFriend+" non valido\n";
        }
        else{
            AddFriend task = new AddFriend(user,friend,key);
            executor.execute(task);
        }

    }

}
