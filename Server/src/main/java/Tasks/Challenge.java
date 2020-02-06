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

    private int count_word_user; //Contatore che indica quante parole deve tradurre user
    private int count_word_friend; //Contatore che indica quante parole deve tradurre friend
    private Object[] italian_words_list;
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

            //Registro gli utenti sul nuovo selettore e il messaggio di risposta
            newUserKey = registerKey(selector,userKey);
            newFriendKey = registerKey(selector,friendKey);

            //Invio i messaggi di risposta
            Writable(newUserKey);
            Writable(newFriendKey);

            wordsList = dictionaryDispatcher.getList(); //Richiedo la lista di parole da usare nella sfida

            if(wordsList == null){

                noInternet(userKey);
                noInternet(friendKey);

                return;
            }

            count_word_user = wordsList.size();
            count_word_friend = wordsList.size();

            italian_words_list = wordsList.keySet().toArray();
            String italianWord = (String) italian_words_list[0]; //prendo la prima parola da tradurre

            System.out.print("KEYSET: "); //TODO elimina
            for(Object i : italian_words_list){
                System.out.print((String) i+" ");
            }
            System.out.println();

            //Setup utenti per l'inizio della sfida
            ConChallenge keyAttachmentUser = new ConChallenge();
            keyAttachmentUser.response = "OK\nSfida cominciata\n"+italianWord+"\n"+CHALLENGE_TIMER+"\n";
            keyAttachmentUser.nextIndex = 1;
            keyAttachmentUser.translate = wordsList.get(italianWord); //Prendo la traduzione della parola italianWord
            keyAttachmentUser.user = "user";
            newUserKey.attach(keyAttachmentUser);

            ConChallenge keyAttachmentFriend = new ConChallenge();
            keyAttachmentFriend.response = "OK\nSfida cominciata\n"+italianWord+"\n"+CHALLENGE_TIMER+"\n";
            keyAttachmentFriend.nextIndex = 1;
            keyAttachmentFriend.translate = wordsList.get(italianWord); //Prendo la traduzione della parola italianWord
            keyAttachmentFriend.user = "friend";
            newFriendKey.attach(keyAttachmentFriend);


            newUserKey.interestOps(SelectionKey.OP_WRITE);
            newFriendKey.interestOps(SelectionKey.OP_WRITE);

            run(selector); //Avvio la sfida

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void noInternet(SelectionKey key){
        Con keyAttachment = (Con) key.attachment();

        keyAttachment.response = "KO\nNessuna connessione a internet\n";
        keyAttachment.challenge = false;

        key.interestOps(SelectionKey.OP_WRITE);
    }

    //Funzione che si occupa di registrare key in selector e inserisce in response il messaggio di risposta
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
        while (count_word_user > 0 || count_word_friend > 0){ //Finchè entrambi gli utenti hanno parole da tradurre
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

        if(response[0].equals("COUNTDOWN")){ //Se è scattato il countdown al client allora gli faccio terminare la sfida
            if(keyAttachment.user.equals("user")){
                count_word_user = 0;
            }
            else{
                count_word_friend = 0;
            }

            keyAttachment.response = "FINISH\nSfida terminata";
            Writable(key);
        }
        else{
            if(keyAttachment.user.equals("user")){
                count_word_user--;
            }
            else{
                count_word_friend--;
            }

            if(!response[0].equals("SKIP")){ //Controllo  che non abbia premuto il pulsante skip
                if(response[1].equalsIgnoreCase(keyAttachment.translate)){
                    keyAttachment.correct++;
                }
                else{
                    keyAttachment.not_correct++;
                }
            }

            if(keyAttachment.nextIndex<italian_words_list.length){ //Controllo che ci siano ancora parola da tradurre
                String word = (String) italian_words_list[keyAttachment.nextIndex];
                keyAttachment.response = "CHALLENGE\n"+word;
                keyAttachment.translate = wordsList.get(word);
                keyAttachment.nextIndex++;

                key.interestOps(SelectionKey.OP_WRITE);
            }
            else{
                keyAttachment.response = "FINISH\nSfida terminata";

                Writable(key);
            }
        }

    }


    //Metodo che si occupa della chiusura della sfida
    private void finishChallenge() {
        String challenge = "finished";
        int userScore = 0, friendScore = 0;
        ConChallenge keyAttachmentChallenge;
        Con userKeyAttachment = null, friendKeyAttachment = null;


        if(newUserKey.isValid()){ //Controllo che user non abbia abbandonato la sfida
            keyAttachmentChallenge = (ConChallenge) newUserKey.attachment();
            userKeyAttachment = (Con) userKey.attachment();

            userScore = (correctScore * keyAttachmentChallenge.correct) + (notCorretScore * keyAttachmentChallenge.not_correct);
            int neutralWords = italian_words_list.length - keyAttachmentChallenge.correct - keyAttachmentChallenge.not_correct; //Numero di parole che non sono state tradotte

            userKeyAttachment.response = "TERMINATED\nParole corrette "+keyAttachmentChallenge.correct+"\nParole sbagliate "+keyAttachmentChallenge.not_correct+"\nParole non tradotte "+neutralWords+"\nPunti totalizzati "+userScore+"\n";
            user.addScore(userScore); //Aggiorno il punteggio dell'utente
        }

        if(newFriendKey.isValid()){ //Controllo che friend non abbia abbandonato la sfida
            keyAttachmentChallenge = (ConChallenge) newFriendKey.attachment();
            friendKeyAttachment = (Con) friendKey.attachment();

            friendScore = (2 * keyAttachmentChallenge.correct) + (-1 * keyAttachmentChallenge.not_correct);
            int neutralWords = italian_words_list.length - keyAttachmentChallenge.correct - keyAttachmentChallenge.not_correct;

            friendKeyAttachment.response = "TERMINATED\nParole corrette "+keyAttachmentChallenge.correct+"\nParole sbagliate "+keyAttachmentChallenge.not_correct+"\nParole non tradotte "+neutralWords+"\nPunti totalizzati "+friendScore+"\n";
            friend.addScore(friendScore); //Aggiorno il punteggio dell'utente
        }

        if(newUserKey.isValid() && newFriendKey.isValid()){ //Se entrambi gli utenti hanno terminato la sfida

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

        //Resetto i flag necessari
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

        keyAttachment.challenge = false; //Resetto il flag della sfida

        try {
            key.interestOps(0); //Resetto l'interesOp

            SocketChannel keySocket = (SocketChannel) key.channel();

            SelectionKey key1;

            if(challenge.equals("finished")){ //Controllo se l'utente ha finito la sfida
                key1 = keySocket.register(this.serverSelector, SelectionKey.OP_WRITE);
            }
            else{
                key1 = keySocket.register(this.serverSelector, SelectionKey.OP_READ);
            }

            key1.attach(keyAttachment);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
