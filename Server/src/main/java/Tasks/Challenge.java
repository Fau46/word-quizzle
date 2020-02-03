package Tasks;

import Server.Con;
import Server.ConChallenge;
import Server.DictionaryDispatcher;
import User.User;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Challenge {
    private int BUF_SIZE = 512 ;
    private User user,friend;
    private SelectionKey userKey,friendKey;
    private Selector serverSelector;
    private int SHUTDOWN;
    private int TIMER = 100;
    private DictionaryDispatcher dictionaryDispatcher;
    private Map<String,String> wordsList;
    private Object[] keySet;

    public Challenge(User user, User friend, SelectionKey userKey, SelectionKey friendKey, Selector serverSelector){
        this.user = user;
        this.friend = friend;
        this.userKey = userKey;
        this.friendKey = friendKey;
        this.serverSelector = serverSelector;
        this.dictionaryDispatcher = DictionaryDispatcher.getInstance();
    }

    public void startChallenge(){
        try {
            Selector selector = Selector.open();

            SelectionKey newUserKey = registerKey(selector,userKey);
            SelectionKey newFriendKey = registerKey(selector,friendKey);

            Writable(newUserKey);
            Writable(newFriendKey);

            wordsList = dictionaryDispatcher.getList(); //Prendo la lista di parole da usare nella sfida
            SHUTDOWN = wordsList.size() * 2;

            keySet = wordsList.keySet().toArray();
            String key = (String) keySet[0];

            System.out.print("KEYSET: ");
            for(Object i : keySet){
                System.out.print((String) i+" ");
            }
            System.out.println();

            //Setup utenti per l'inizio della sfida
            ConChallenge keyAttachmentUser = new ConChallenge();
            keyAttachmentUser.response = "OK\nSfida cominciata\n"+key+"\n";
            keyAttachmentUser.nextIndex = 1;
            keyAttachmentUser.translate = wordsList.get(key);
            keyAttachmentUser.user = "user";
            newUserKey.attach(keyAttachmentUser);

            ConChallenge keyAttachmentFriend = new ConChallenge();
            keyAttachmentFriend.response = "OK\nSfida cominciata\n"+key+"\n";
            keyAttachmentFriend.nextIndex = 1;
            keyAttachmentFriend.translate = wordsList.get(key);
            keyAttachmentFriend.user = "friend";
            newFriendKey.attach(keyAttachmentFriend);


            newUserKey.interestOps(SelectionKey.OP_WRITE);
            newFriendKey.interestOps(SelectionKey.OP_WRITE);

            run(selector);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private SelectionKey registerKey(Selector selector, SelectionKey key) throws ClosedChannelException {
//        Con keyAttachment = (Con) key.attachment();
        ConChallenge keyAttachmentChalleng = new ConChallenge();

        key.interestOps(0);

        SocketChannel keySocket = (SocketChannel) key.channel();
        SelectionKey key1 =  keySocket.register(selector, SelectionKey.OP_WRITE);

//        keyAttachment.request = null;
        keyAttachmentChalleng.response = "OK\nCaricamento";
        key1.attach(keyAttachmentChalleng);

        return key1;
    }


    private void run(Selector selector){
        while (SHUTDOWN > 0){
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
        ConChallenge keyAttachment = (ConChallenge) key.attachment();
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
            System.out.println("LETTO: "+string);
            keyAttachment.request = requestBuilder.toString();
        }
    }


    private void Writable(SelectionKey key) throws IOException{
        SocketChannel client = (SocketChannel) key.channel();
        ConChallenge keyAttachment = (ConChallenge) key.attachment();
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


    private void parser(SelectionKey key){
        ConChallenge keyAttachment = (ConChallenge) key.attachment();
        String[] response = keyAttachment.request.split("\n");

        SHUTDOWN--;

        if(!response[0].equals("skip")){
            if(response[0].equals(keyAttachment.translate)){
                keyAttachment.correct++;
            }
            else{
                keyAttachment.not_correct++;
            }
        }

//        System.out.println("INDEX: "+keyAttachment.nextIndex+" SHUTDOWN: "+SHUTDOWN);
        if(keyAttachment.nextIndex<keySet.length){
            String word = (String) keySet[keyAttachment.nextIndex];
            keyAttachment.response = "OK\n"+word;
            keyAttachment.translate = wordsList.get(word);
            keyAttachment.nextIndex++;
        }
        else{
            keyAttachment.response = "FINISH\nSfida terminata";
        }

//        keyAttachment.response

        key.interestOps(SelectionKey.OP_WRITE);

    }


    //Deregistra key dal selettore e lo registra sul selettore principale
    private Con deregisterKey(SelectionKey key){
        Con keyAttachment = (Con) key.attachment(); //TODO migliorare con con conchallege
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
