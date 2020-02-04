package Tasks;

import Costanti.Costanti;
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

public class Challenge implements Costanti {
    private User user,friend;
    private Selector serverSelector;
    private SelectionKey userKey,friendKey;

    private Object[] keySet;
    private int count_word_user;
    private int count_word_friend;
    private Map<String,String> wordsList;
    private SelectionKey newUserKey,newFriendKey;
    private DictionaryDispatcher dictionaryDispatcher;


    public Challenge(User user, User friend, SelectionKey userKey, SelectionKey friendKey, Selector serverSelector){
        this.user = user;
        this.friend = friend;
        this.userKey = userKey;
        this.friendKey = friendKey;
        this.serverSelector = serverSelector;
        this.dictionaryDispatcher = DictionaryDispatcher.getInstance();
    }


    //Funzione che si occupa del setup iniziale della sfida
    public void startChallenge(){
        try {
            Selector selector = Selector.open();

            //Registro gli utenti sul nuovo selettore
            newUserKey = registerKey(selector,userKey); //TODO proseguire con l'ispezione del codice
            newFriendKey = registerKey(selector,friendKey);

            Writable(newUserKey);
            Writable(newFriendKey);

            wordsList = dictionaryDispatcher.getList(); //Prendo la lista di parole da usare nella sfida

            count_word_user = wordsList.size();
            count_word_friend = wordsList.size();

            keySet = wordsList.keySet().toArray();
            String key = (String) keySet[0]; //prendo la prima parola da tradurre

            System.out.print("KEYSET: "); //TODO elimina
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

            run(selector); //avvio la sfida

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private SelectionKey registerKey(Selector selector, SelectionKey key) throws ClosedChannelException {
        ConChallenge keyAttachmentChalleng = new ConChallenge();

        key.interestOps(0);

        SocketChannel keySocket = (SocketChannel) key.channel();
        SelectionKey key1 =  keySocket.register(selector, SelectionKey.OP_WRITE);

        keyAttachmentChalleng.response = "OK\nCaricamento";
        key1.attach(keyAttachmentChalleng);

        return key1;
    }


    private void run(Selector selector){
        while (count_word_user > 0 || count_word_friend > 0){ //finchè entrambi gli utenti hanno parole da tradurre
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
                    deregisterKey(key,"not finished");
                }
            }
        }

        finishChallenge();
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

        if (read == -1) throw new IOException("Canale chiuso"); //Mi accerto che il canale non sia chiuso
        else if(read == 0){ //Se ho finito di leggere parso la request
            parser(key);
            iterator.remove();
        }
        else{ //Allego ciò che ho letto alla request della key
            String string = new String(byteBuffer,0,read);
            requestBuilder.append(string);
            System.out.println("LETTO "+string); //TODO elimina
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


    private void parser(SelectionKey key) throws IOException {
        ConChallenge keyAttachment = (ConChallenge) key.attachment();
        String[] response = keyAttachment.request.split("\n");

        if(keyAttachment.user.equals("user")){
            count_word_user--;
        }
        else{
            count_word_friend--;
        }

        
        if(!response[0].equals("skip")){
            if(response[0].equals(keyAttachment.translate)){
                keyAttachment.correct++;
            }
            else{
                keyAttachment.not_correct++;
            }
        }

        if(keyAttachment.nextIndex<keySet.length){
            String word = (String) keySet[keyAttachment.nextIndex];
            keyAttachment.response = "OK\n"+word;
            keyAttachment.translate = wordsList.get(word);
            keyAttachment.nextIndex++;

            key.interestOps(SelectionKey.OP_WRITE);
        }
        else{
            keyAttachment.response = "FINISH\nSfida terminata";

            Writable(key);
        }

    }


    private void finishChallenge() {
        ConChallenge keyAttachmentChallenge;
        Con userKeyAttachment = null, friendKeyAttachment = null;
        String challenge = "finished";

        int userScore = 0, friendScore = 0;
        int winnerScore = 3, correctScore = 2, notCorretScore = -1;

        if(newUserKey.isValid()){
            keyAttachmentChallenge = (ConChallenge) newUserKey.attachment();
            userKeyAttachment = (Con) userKey.attachment();

            userScore = (correctScore * keyAttachmentChallenge.correct) + (notCorretScore * keyAttachmentChallenge.not_correct);
            int neutralWords = keySet.length - keyAttachmentChallenge.correct - keyAttachmentChallenge.not_correct;

            userKeyAttachment.response = "TERMINATED\nParole corrette "+keyAttachmentChallenge.correct+"\nParole sbagliate "+keyAttachmentChallenge.not_correct+"\nParole non tradotte "+neutralWords+"\nPunti totalizzati "+userScore+"\n";
            user.addScore(userScore);
        }

        if(newFriendKey.isValid()){
            keyAttachmentChallenge = (ConChallenge) newFriendKey.attachment();
            friendKeyAttachment = (Con) friendKey.attachment();

            friendScore = (2 * keyAttachmentChallenge.correct) + (-1 * keyAttachmentChallenge.not_correct);
            int neutralWords = keySet.length - keyAttachmentChallenge.correct - keyAttachmentChallenge.not_correct;

            friendKeyAttachment.response = "TERMINATED\nParole corrette "+keyAttachmentChallenge.correct+"\nParole sbagliate "+keyAttachmentChallenge.not_correct+"\nParole non tradotte "+neutralWords+"\nPunti totalizzati "+friendScore+"\n";
            friend.addScore(friendScore);;
        }


        if(newUserKey.isValid() && newFriendKey.isValid()){

            if(userScore > friendScore){
                user.addScore(winnerScore);
                userKeyAttachment.response += "Hai vinto, guadagni "+winnerScore+" punti extra!"+"\nPunti attuali "+user.getScore()+"\n";

                friendKeyAttachment.response += "Hai perso!\nPunti attuali"+friend.getScore()+"\n";
            }
            else if(friendScore > userScore){
                friend.addScore(winnerScore);
                friendKeyAttachment.response += "Hai vinto, guadagni "+winnerScore+" punti extra!"+"\nPunti attuali "+friend.getScore()+"\n";

                userKeyAttachment.response += "Hai perso!\nPunti attuali "+user.getScore()+"\n";
            }
            else{
                friendKeyAttachment.response += "Pareggio!\nPunti attuali "+friend.getScore()+"\n";
                userKeyAttachment.response += "Pareggio!\nPunti attuali "+user.getScore()+"\n";
            }
            deregisterKey(newUserKey,challenge);
            deregisterKey(newFriendKey,challenge);
        }
        else if(newUserKey.isValid()){
            userKeyAttachment.response += "Il tuo sfidante ha abbandonato!\nPunti attuali "+user.getScore()+"\n";
            deregisterKey(newUserKey,challenge);

        }
        else if(newFriendKey.isValid()){
            friendKeyAttachment.response += "Il tuo sfidante ha abbandonato!\nPunti attuali "+friend.getScore()+"\n";
            deregisterKey(newFriendKey,challenge);
        }
    }


    //Deregistra key dal selettore e lo registra sul selettore principale
    private void deregisterKey(SelectionKey key, String challenge){
        Con keyAttachment;
        ConChallenge keyAttachmentChallenge = (ConChallenge) key.attachment();

        if(keyAttachmentChallenge.user.equals("user")){
            keyAttachment = (Con) userKey.attachment();
            user.decrementUse();
            count_word_user = 0;
        }
        else{
            keyAttachment = (Con) friendKey.attachment();
            friend.decrementUse();
            count_word_friend = 0;
        }

        try {
            key.interestOps(0);

            SocketChannel keySocket = (SocketChannel) key.channel();

            SelectionKey key1;

            if(challenge.equals("finished")){
             key1 = keySocket.register(this.serverSelector, SelectionKey.OP_WRITE);
            }
            else{
                key1 = keySocket.register(this.serverSelector, SelectionKey.OP_READ);
            }

            key1.attach(keyAttachment);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        return keyAttachment;
    }
}
