package Database;

import User.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.stream.JsonReader;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DBMS {
    private  ArrayList<User> map;
    private static DBMS dbms;
    private DBMS(){
        map = new ArrayList<>();
    }

    public static DBMS getIstance(){
        if(dbms==null) dbms = new DBMS();
        return dbms;
    }

    public boolean existUser(String nick){
        Gson gson = new Gson();
        try(JsonReader reader = new JsonReader(new FileReader("./Server/src/main/java/Database/registrazioni.json"))) {

            reader.beginArray();
            while (reader.hasNext()){
                reader.beginObject();
                while (reader.hasNext()){
                    String name = reader.nextName();
                    System.out.println(name);
                    if(name.equals("nickname")){
                        String string = reader.nextString();
                        System.out.println(string);
                        if (string.equals(nick)){
                            return true;
                        }
                    }
                    else reader.nextString();
                }
                System.out.println("sono qui");
                reader.endObject();

            }
            reader.endArray();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){

        }

        return false;
    }

    public boolean registerUser(User user){
        try{
            FileChannel writer = FileChannel.open(Paths.get("./Server/src/main/java/Database/registrazioni.json"), StandardOpenOption.WRITE);
            Gson gson = new Gson();
            StringBuilder strinJson = new StringBuilder();
            strinJson.append(","+gson.toJson(user)+"]");
            System.out.println(writer.position());
            long pos = writer.size()-1;
            System.out.println(pos);
            writer.position(pos);
            System.out.println(writer.position());
            writer.write(ByteBuffer.wrap(strinJson.toString().getBytes()));
            writer.close();
//            Writer writer = Files.newBufferedWriter(Paths.get("./Server/src/main/java/Database/registrazioni.json"));
//            map.add(user);
//            gson.toJson(map,writer);
//            writer.close();
//            System.out.println("Utente "+user.getNickname()+" iscritto");

        } catch (IOException e) {
            System.out.println("IOException");
            return false;
        }
        return true;
    }
}
