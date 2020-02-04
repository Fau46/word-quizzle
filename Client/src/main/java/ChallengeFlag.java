import java.util.concurrent.atomic.AtomicInteger;

public class ChallengeFlag {
    private AtomicInteger flag;
    private static ChallengeFlag challengeFlag;

    private ChallengeFlag(){
        this.flag = new AtomicInteger(0);
    }

    public static ChallengeFlag getInstance(){
        if(challengeFlag == null) challengeFlag = new ChallengeFlag();

        return challengeFlag;
    }

    public void setFlag() {
        this.flag.set(1);
    }

    public void resetFlag(){
        this.flag.set(0);
    }

    public boolean isOccupied(){
        return this.flag.intValue() == 1;
    }
}
