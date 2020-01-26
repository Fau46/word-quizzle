package Tasks;

import Server.Con;
import User.User;

import java.nio.channels.SelectionKey;

public class ShowScore implements Runnable {
    private SelectionKey key;
    private User user;

    public ShowScore(User user, SelectionKey key){
        this.key = key;
        this.user = user;
    }

    @Override
    public void run() {
        Integer score;
        Con keyAttachment = (Con) key.attachment();



    }
}
