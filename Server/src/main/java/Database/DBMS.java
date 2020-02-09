package Database;

import Costanti.Costanti;
import User.User;
import com.google.gson.Gson;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

public class DBMS implements Costanti {
    private static DBMS dbms;


    private DBMS(){
        if(!Files.exists(Paths.get(DATA_PATH))){
            new File(DATA_PATH).mkdir();
        }
    }


    //Metodo per ottenere l'istanza singleton della classe
    public static DBMS getIstance(){
        if(dbms==null) dbms = new DBMS();
        return dbms;
    }


    public synchronized boolean existUser(String nick){ 
        String path = DATA_PATH+nick+EXTENSION;

        if(Files.exists(Paths.get(path))){
            return true;
        }
        return false;
    }


    //Metoodo che permette di registrare user.
    //Ritorna se è andato a buon fine, false altrimenti.
    public synchronized boolean registerUser(User user){
        try{
            FileChannel writer;
            StringBuilder userPath = new StringBuilder(DATA_PATH+user.getNickname()+EXTENSION);
            StringBuilder strinJson = new StringBuilder();
            Gson gson = new Gson();

            new File(userPath.toString()).createNewFile();
            System.out.println("[REGISTRATION] Utente "+user.getNickname()+" registrato");

            strinJson.append(gson.toJson(user)); //serializzo l'oggetto

            writer = FileChannel.open(Paths.get(userPath.toString()), StandardOpenOption.WRITE);
            writer.write(ByteBuffer.wrap(strinJson.toString().getBytes())); //scrivo l'oggetto utente serializzato sul file json
            writer.close();

        } catch (IOException e) {
            System.out.println("IOException");
            return false;
        }
        return true;
    }


    //Metodo che restituisce l'oggetto associato a nick, null se l'oggetto non esiste
    public User getUser(String nick){
        String path = new String(DATA_PATH+nick+EXTENSION);

        if(!existUser(nick)) return null; //Se l'utente non esiste ritorno null

        Gson gson = new Gson();
        User user = null;

        try(Reader reader = Files.newBufferedReader(Paths.get(path))) {
            user = gson.fromJson(reader,User.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return user;
    }


    //Metodo che serializza l'oggetto user nella cartella Dati
    public void serializeUser(User user){
        FileChannel writer;
        StringBuilder userPath = new StringBuilder(DATA_PATH+user.getNickname()+EXTENSION);
        StringBuilder strinJson = new StringBuilder();
        Gson gson = new Gson();

        strinJson.append(gson.toJson(user));

        try {
            writer = FileChannel.open(Paths.get(userPath.toString()),StandardOpenOption.WRITE);
            writer.write(ByteBuffer.wrap(strinJson.toString().getBytes()));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
