import Database.DBMS;
import User.User;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

public class Server {
    private Selector selector;
    private ThreadPoolExecutor executor;
    private DBMS dbms;
    private int BUF_SIZE = 512;
    private Map<String, User> mapUser;

    class Con{
        String nickname;
        String response;
        Boolean logout;

        public Con(){
            logout = false;
        }
    }

    public Server(Selector selector, ThreadPoolExecutor executor){
        this.selector = selector;
        this.executor = executor;
        this.dbms = DBMS.getIstance();
        this.mapUser = Collections.synchronizedMap(new HashMap()); //hashmap thead-safe
    }

    //Avvio server
    public void run() {
        while (true) {
            try {
                selector.select();
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
                        this.Readable(key);
                        iterator.remove();
                    }
                    else if(key.isWritable()){
                        this.Writable(key);
                        iterator.remove();
                    }
                } catch (IOException e) {
                    key.cancel();
                    try {
                        Con keyAttachment = (Con) key.attachment();
                        String nick = keyAttachment.nickname;
                        if(nick != null) mapUser.remove(nick); //Rimuovo il client TODO nullpointerexception se chiudo prima di fare il login

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
    }


    private void Readable(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        byte[] byteBuffer = new byte[4];
        ByteBuffer intput = ByteBuffer.wrap(byteBuffer);

        int read=client.read(intput); //Leggo dalla socket
        if (read==-1) throw new IOException("Canale chiuso"); //mi accerto che il canale non sia chiuso

        String string = new String(byteBuffer);
        System.out.println("[READ OP] "+string); //TODO ELIMINA

        if(string.equals("LOIN")){
            Con keyAttachment = login(client);
            key.attach(keyAttachment);
            key.interestOps(SelectionKey.OP_WRITE);
        }
        else if(string.equals("LOUT")){
            logout(client,key);
            key.interestOps(SelectionKey.OP_WRITE);
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
            key.interestOps(SelectionKey.OP_READ);
        }
        else { //se risulta che logout=true sollevo l'eccezione così faccio chiudere la socket
            throw new IOException("Logout");
        }

    }

    //Funzione che effettua il login
    private Con login(SocketChannel client) throws IOException {
        byte[] readAll = new byte[BUF_SIZE];
        ByteBuffer in = ByteBuffer.wrap(readAll);
        Con keyAttachment = new Con();
        keyAttachment.nickname = null;

        client.read(in); //leggo il contenuto della socket

        String[] aux = (new String(readAll)).split("\n"); //splitto quello che ho letto

        String nickname = aux[1];
        String password = aux[2];

        User user = null;

        if (mapUser.get(nickname) == null) {
            user = dbms.loginUser(nickname, password);

            if (user == null) keyAttachment.response = "KO\nNickname non presente";
            else if (user.getNickname().equals(nickname) && user.getPassword().equals(password)){
                System.out.println("[LOGIN] Inserisco "+nickname+" nella mapUser");
                mapUser.put(nickname,user);
                keyAttachment.nickname = nickname;
                keyAttachment.response = "OK\nLogin effettuato";
            }
            else keyAttachment.response = "KO\nNickname o password errate";
        } else keyAttachment.response = "KO\nUtente gia' connesso";

        return keyAttachment;
    }

    private void logout(SocketChannel client, SelectionKey key) throws IOException {
        byte[] readAll = new byte[BUF_SIZE];
        ByteBuffer in = ByteBuffer.wrap(readAll);

        client.read(in);

        String[] aux = (new String(readAll)).split("\n"); //splitto quello che ho letto
        String nickname = aux[1];

        Con keyAttachment = (Con) key.attachment();

        if(mapUser.get(nickname) == null) keyAttachment.response = "KO\nUtente non in linea";
        else {
            keyAttachment.response = "OK\nUtente disconnesso";
            keyAttachment.logout = true;
        }
    }

}
