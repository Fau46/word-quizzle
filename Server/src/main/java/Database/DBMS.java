package Database;

import User.User;
import com.google.gson.Gson;

import java.io.*;
import java.net.UnknownServiceException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class DBMS implements Costants{
    private static DBMS dbms;

    private DBMS(){
    }

    //Metodo per ottenere l'istanza singleton della classe
    public static DBMS getIstance(){
        if(dbms==null) dbms = new DBMS();
        return dbms;
    }


    public synchronized boolean existUser(String nick){
        if(Files.exists(Paths.get(PATH + nick))){
            return true;
        }
        return false;
    }


    public boolean registerUser(User user){
        try{
            FileChannel writer;
            StringBuilder userPath = new StringBuilder(PATH+user.getNickname());
            StringBuilder strinJson = new StringBuilder();
            Gson gson = new Gson();


            new File(userPath.toString()).mkdir(); //Creo la cartella dell'utente
            System.out.println("Directory "+userPath+" creata");

            //creo il file login dell'utente e lo inserisco nella sua cartella
            userPath.append("/"+LOGIN_DB);
            new File(userPath.toString()).createNewFile();
            System.out.println("File "+userPath+" creata");

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


    public User loginUser(String nick, String pwd){
        System.out.println(existUser(nick));
        if(!existUser(nick)) return null; //Se l'utente non esiste ritorno null

        String path = new String(PATH+nick+"/"+LOGIN_DB);
        Gson gson = new Gson();
        User user = null;

        try {
            Reader reader = Files.newBufferedReader(Paths.get(path));
            user = gson.fromJson(reader,User.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return user;
    }
}
