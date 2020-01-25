package Tasks;

import Server.Con;
import User.User;
import com.google.gson.Gson;

import java.nio.channels.SelectionKey;
import java.util.Vector;
import java.util.Vector;

public class ShowFriends implements Runnable{
    private User user;
    private SelectionKey key;
    public ShowFriends(User user, SelectionKey key){
        this.user = user;
        this.key = key;
    }

    @Override
    public void run() {
        Vector<String> friendList;
        StringBuilder string = new StringBuilder();
        Gson gson = new Gson();
        Con keyAttachment = (Con) key.attachment();

        synchronized (friendList= user.getFriends()){
            string.append("OK\n");
            string.append(gson.toJson(friendList));
            string.append("\n");

            keyAttachment.response=string.toString();

            try {
                System.out.println("Mi sospendo");
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            key.interestOps(SelectionKey.OP_WRITE);
        }catch (Exception e){
            user.decrementUse();
            e.printStackTrace();
            return;
        }

        user.decrementUse();

    }
}

