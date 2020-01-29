public class ChallengeFlag {
    public boolean flag;
    private static ChallengeFlag challengeFlag;

    private ChallengeFlag(){
        this.flag = false;
    }

    public static ChallengeFlag getInstance(){
        if(challengeFlag == null) challengeFlag = new ChallengeFlag();

        return challengeFlag;
    }


}
