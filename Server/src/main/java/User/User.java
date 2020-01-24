package User;

import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class User {
    private String nickname;
    private String password;
    private int score;
    private Vector<String> friends;
    private int use;

    public User(String nick, String pwd){
        this.nickname = nick;
        this.password = pwd;
        this.score = 0;
        this.friends = new Vector<>();
        this.use = 0;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPassword() {
        return password;
    }

    public synchronized void incrementUse(){
        this.use++;
    }

    public synchronized void decrementUse(){
        if(use > 0) this.use--;
    }

    public Vector<String> getFriends() {
        return friends;
    }

    public void showFriend(){
        System.out.println("Utente:"+nickname);
        for(String string : friends){
            System.out.println(string);
        }
    }
}

