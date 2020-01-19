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

public class DBMS implements Costants{
    private  ArrayList<User> map;
    private static DBMS dbms;
    private String path;

    private DBMS(){
        path = "./Server/src/main/java/Database/registrazioni.json";
        map = new ArrayList<>();
    }

    public static DBMS getIstance(){
        if(dbms==null) dbms = new DBMS();
        return dbms;
    }

    public boolean existUser(String nick){
        if(!this.fileEmpty()){

            Gson gson = new Gson();
            try(JsonReader reader = new JsonReader(new FileReader("./Server/src/main/java/Database/registrazioni.json"))) {

                reader.beginArray();
                while (reader.hasNext()){
                    reader.beginObject();
                    while (reader.hasNext()){
                        String name = reader.nextName();
                        if(name.equals("nickname")){
                            String string = reader.nextString();
                            System.out.println(string);
                            if (string.equals(nick)){
                                return true;
                            }
                        }
                        else reader.nextString();
                    }
                    reader.endObject();

                }
                reader.endArray();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e){

            }
        }
        return false;
    }

    public boolean registerUser(User user){
        try{

            FileChannel writer = FileChannel.open(Paths.get(path), StandardOpenOption.WRITE);
            StringBuilder strinJson = new StringBuilder();
            Gson gson = new Gson();

            //controllo se il file è vuoto e non è ancora stato inizializzato

            if(this.fileEmpty()) strinJson.append("[");
            else strinJson.append(",");

            strinJson.append(gson.toJson(user)+"]");
            if(writer.size()!=0){
                writer.position(writer.size()-1);
            }

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

    private boolean fileEmpty(){
        try {
            FileChannel reader = FileChannel.open(Paths.get(path), StandardOpenOption.READ);
            ByteBuffer byteBuffer = ByteBuffer.allocate(5);

            int r = reader.read(byteBuffer);
            if (r ==-1) return true;

            byteBuffer.flip();
            byte[] bbuff = new byte[5];
            byteBuffer.get(bbuff,0,1);
            System.out.println("Stringa letta:"+new String(bbuff));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}
