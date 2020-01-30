import java.util.concurrent.atomic.AtomicInteger;

public class ChallengeFlag {
    public AtomicInteger flag;
    private static ChallengeFlag challengeFlag;

    private ChallengeFlag(){
        this.flag = new AtomicInteger(0);
    }

    public static ChallengeFlag getInstance(){
        if(challengeFlag == null) challengeFlag = new ChallengeFlag();

        return challengeFlag;
    }


}
