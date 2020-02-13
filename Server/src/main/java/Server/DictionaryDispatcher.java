package Server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import Costanti.*;

public class DictionaryDispatcher implements Costanti {
    private static final int RANDOM_LINES = 5;
    private static DictionaryDispatcher dictionaryDispatcher;
    private List<String> dictionary;


    private DictionaryDispatcher(){

        try {
            dictionary = Files.readAllLines(Paths.get(DICTIONARY_PATH), StandardCharsets.UTF_8);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static DictionaryDispatcher getInstance(){
        if(dictionaryDispatcher == null) dictionaryDispatcher = new DictionaryDispatcher();
        return dictionaryDispatcher;
    }


    //Ritorna una map formata dalla coppia <parola italiana, parola in inglese>.
    //Ritorna invece null se ha riscontrato un errore nella traduzione delle parole
    public Map<String,String> getList(){
        Map<String,String> translatedWords = new HashMap<>();
        Random random = new Random();
        int dictionaryLen = dictionary.size();
        int y;

        for(int i=0; i<RANDOM_LINES; i++){
            y = random.nextInt(dictionaryLen);
            String word = dictionary.get(y);
            String translatedWord = myMemoryTanslator(word);
//            System.out.println(word+" "+translatedWord);
            if(translatedWord == null) return null;

            translatedWords.put(word, translatedWord);
        }

        return translatedWords;
    }


    //Ritorna la traduzione di word oppure null se non riesce a collegarsi al sito internet
    private String myMemoryTanslator(String word){
                    String reqMyMemory = "https://api.mymemory.translated.net/get?q=" + word + "&langpair=it|en";
                    String reqYandex = "https://translate.yandex.net/api/v1.5/tr.json/translate?key=trnsl.1.1.20200201T115631Z.c3b0cdde609dde53.2228d16c158e2da155316068ad1bee64e3af99f5&text=" + word + "&lang=it-en";
                    String yardTranslate = null;
                    Gson gson = new Gson();
                    URL url;
                    Reader reader;

                    try {
                        url = new URL(reqYandex);
                        reader = new InputStreamReader(url.openStream());

                        JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

                        yardTranslate = (jsonObject.getAsJsonArray("text")).get(0).toString();
                        yardTranslate = yardTranslate.replace("\"","");

                    } catch (MalformedURLException | UnknownHostException e) {
                        e.printStackTrace();
                        return null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        url = new URL(reqMyMemory);
                        reader = new InputStreamReader(url.openStream());

                        JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

                        JsonArray auxArray = jsonObject.getAsJsonArray("matches");
                        String myMemoryTranslate;

                        for (int i = 0; i < auxArray.size(); i++) {
                            myMemoryTranslate = (auxArray.get(i)).getAsJsonObject().get("translation").toString();
                            myMemoryTranslate = myMemoryTranslate.replace("\"","");

                            if (yardTranslate.equalsIgnoreCase(myMemoryTranslate)) { //Se le parole tradotte dai due sistemi combaciano allora ritorno la traduzione
                                return myMemoryTranslate;
                            }
                        }
                    } catch (MalformedURLException | UnknownHostException e) {
                        e.printStackTrace();
                        return null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return yardTranslate;
    }

}
