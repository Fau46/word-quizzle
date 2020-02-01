package Server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class DictionaryDispatcher {
    private static final int RANDOM_LINES = 5;
    private static DictionaryDispatcher dictionaryDispatcher;
    private List<String> dictionary;

    private DictionaryDispatcher(){

        try {
            dictionary = Files.readAllLines(Paths.get("./Server/src/main/resources/words.italian.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static DictionaryDispatcher getInstance(){
        if(dictionaryDispatcher == null) dictionaryDispatcher = new DictionaryDispatcher();
        return dictionaryDispatcher;
    }

    public Map<String,String> getList(){
//        Map<String,String> list = new TreeMap<>();
        List<String> list = new LinkedList<>();
        Random random = new Random();
        int dictionaryLen = dictionary.size();
        int y;

        long start = System.currentTimeMillis();

        for(int i=0; i<RANDOM_LINES; i++){
            y = random.nextInt(dictionaryLen);
            String word = dictionary.get(y);
            list.add(word);
//            String myMemory = myMemoryTanslator(word);
        }

        List<CompletableFuture<String>> traduzione = list.stream().map(
                parola ->  myMemoryTanslator(parola))
                .collect(Collectors.toList());

        CompletableFuture<Void> traduzione1 = CompletableFuture.allOf(traduzione.toArray(new CompletableFuture[0]));


        CompletableFuture<List<String>> tuttoTradotto = traduzione1.thenApply(
                v -> {
                    return traduzione.stream().map(t -> t.join()).collect(Collectors.toList());
                }
        );

        try {
            System.out.println(tuttoTradotto.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        double finish = (double) (System.currentTimeMillis() - start) / 1000.0;
        System.out.println("TEMPO IMPIEGATO "+finish);
        return null;
    }

//    private String myMemoryTanslator(String word){
    private CompletableFuture<String> myMemoryTanslator(String word){
        return CompletableFuture.supplyAsync(
                () -> {
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

                        System.out.println("word: " + word + " " + (jsonObject.getAsJsonArray("text")).get(0));
                        yardTranslate = (jsonObject.getAsJsonArray("text")).get(0).toString();

                    } catch (MalformedURLException e) {
                        e.printStackTrace();
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
                            System.out.println("RESPONSE[" + word + "] " + myMemoryTranslate);
                            if (yardTranslate.equalsIgnoreCase(myMemoryTranslate)) {
                                System.out.println("ritorno mymemory");
                                return myMemoryTranslate;
                            }
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return yardTranslate;
                }
        );
    }
}