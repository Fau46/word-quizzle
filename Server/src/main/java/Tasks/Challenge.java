package Tasks;

import Server.Con;
import Server.DictionaryDispatcher;
import User.User;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Challenge {
    private int BUF_SIZE = 512 ;
    private User user,friend;
    private SelectionKey userKey,friendKey;
    private Selector serverSelector;
    private boolean SHUTDOWN = false;
    private int TIMER = 100;
    private DictionaryDispatcher dictionaryDispatcher;

    public Challenge(User user, User friend, SelectionKey userKey, SelectionKey friendKey, Selector serverSelector){
        this.user = user;
        this.friend = friend;
        this.userKey = userKey;
        this.friendKey = friendKey;
        this.serverSelector = serverSelector;
        this.dictionaryDispatcher = DictionaryDispatcher.getInstance();

    }

    public void startChallenge(){

        Map<String,String> wordsList = dictionaryDispatcher.getList();

        try {
            Selector selector = Selector.open();

            registerKey(selector,userKey);
            registerKey(selector,friendKey);
            run(selector);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void registerKey(Selector selector, SelectionKey key) throws ClosedChannelException {
        Con keyAttachment = (Con) key.attachment();

        key.interestOps(0);

        SocketChannel keySocket = (SocketChannel) key.channel();
        SelectionKey key1 =  keySocket.register(selector, SelectionKey.OP_WRITE);

        keyAttachment.request = null;
        keyAttachment.response = "OK\nSfida cominciata";
        key1.attach(keyAttachment);
    }


    private void run(Selector selector){
        while (!SHUTDOWN){
            try {
                selector.select(TIMER);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                try {
                    if (key.isReadable()) {
                        this.Readable(key,iterator);
                    }
                    else if(key.isWritable()){
                        this.Writable(key);
                        iterator.remove();
                    }
                } catch (IOException e) {
                    deregisterKey(key);
                }
            }
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
        int read=client.read(intput); //Leggo dalla socket

        if (read ==- 1) throw new IOException("Canale chiuso"); //Mi accerto che il canale non sia chiuso
        else if(read == 0){ //Se ho finito di leggere parso la request TODO e se non sono arrivati tutti i pacchetti? (suggerimento nicola)
//            parser(key);
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

        //Scrivo la risposta al client
        while (buffer.hasRemaining()){
            client.write(buffer);
        }

        keyAttachment.response = null;
        keyAttachment.request = null;
        key.interestOps(SelectionKey.OP_READ);
    }

    private Con deregisterKey(SelectionKey key){
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
