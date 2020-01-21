import Database.DBMS;
import User.User;
import sun.nio.cs.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

public class Server {
    private Selector selector;
    private ThreadPoolExecutor executor;
    private DBMS dbms;
    private int BUF_SIZE = 512;
    private Map<String,User> mapUser;



    public Server(Selector selector, ThreadPoolExecutor executor){
        this.selector = selector;
        this.executor = executor;
        this.dbms = DBMS.getIstance();
        this.mapUser = Collections.synchronizedMap(new HashMap());
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
                        System.out.println("[CLOSED CLIENT]: " + ((SocketChannel) key.channel()).getRemoteAddress());
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
        System.out.println(string);

        if(string.equals("LOIN")){
            byte[] readAll = new byte[BUF_SIZE];
            ByteBuffer in = ByteBuffer.wrap(readAll);

            client.read(in); //leggo il contenuto della socket

            String aux[] = (new String(readAll)).split("\n"); //splitto quello che ho letto

            String nickname = aux[1];
            String password = aux[2];

            String response;
            User user = null;

            if(mapUser.get(nickname) == null){
                user = dbms.loginUser(nickname, password);

                if(user == null) response = "KO\nNickname non presente";
                else if(user.getNickname().equals(nickname) && user.getPassword().equals(password)) response = "OK\nUtente autenticato";
                else response = "KO\nNickname o password errate";
            }
            else response = "KO\nUtente gia' connesso";

            if(!response.contains("KO")){
                System.out.println("Inserisco utente");
                mapUser.put(nickname,user);
            }

            key.attach(response);
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private void Writable(SelectionKey key) throws IOException{
        SocketChannel client = (SocketChannel) key.channel();

        String string = (String) key.attachment();
        ByteBuffer buffer = ByteBuffer.allocate(string.length());
        System.out.println("Writable "+string.length());
        buffer.put(string.getBytes());
        buffer.flip();

        while (buffer.hasRemaining()){
            client.write(buffer);
        }

        key.attach(null);
        key.interestOps(SelectionKey.OP_READ);

    }
}
