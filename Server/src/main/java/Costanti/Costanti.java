package Costanti;

public interface Costanti {
    //------- Costanti generali -------
    int BUF_SIZE = 512;
    int TIMER = 100; //Timer per il selector


    //------- ChallengeRequest -------
    int SELECTOR_TIMEOUT = 5000; //(Intervallo T1 in millisecondi)
    String HOSTNAME = "localhost";


    //------- Challenge -------
    int winnerScore = 3,
        correctScore = 2,
        notCorretScore = -1;
    int CHALLENGE_TIMER = 30; //Timer per la sfida (intervallo T2 in secondi)


    //------- DictionaryDispatcher -------
    String DICTIONARY_PATH = "./Server/src/main/resources/words.italian.txt";
//    String DICTIONARY_PATH = "src/main/resources/words.italian.txt";


    //------- DBMS -------
    String DATA_PATH = "./Server/src/main/java/Database/Dati/";
//    String DATA_PATH = "src/main/java/Database/Dati/";
    String EXTENSION = ".json";


    //------- userDispatcher -------
    int SLEEP_TIMER = 3;
}
