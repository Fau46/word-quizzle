package Costanti;

public interface Costanti {
    int BUF_SIZE = 512;
    int TIMER = 100; //Timer per il selector

    //------- ChallengeRequest -------
    int SELECTOR_TIMEOUT = 5000; //(Intervallo T1)
    String HOSTNAME = "localhost";

    //------- Challenge -------
    int winnerScore = 3,
        correctScore = 2,
        notCorretScore = -1;
    int CHALLENGE_TIMER = 10;
}
