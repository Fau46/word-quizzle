package Tasks;

import Server.Con;
import User.*;
import com.google.gson.Gson;

import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ShowRank implements Runnable{
    private User user;
    private SelectionKey key;
    private ConcurrentHashMap<String, User> mapUser;
    private UserDispatcher userDispatcher;

    public ShowRank(User user, SelectionKey key, ConcurrentHashMap<String, User> mapUser){
        this.user = user;
        this.key = key;
        this.mapUser = mapUser;
        this.userDispatcher = UserDispatcher.getIstance();
    }

    @Override
    public void run() {
        Vector<String> friendList;
        HashMap<String, AtomicInteger> rank = new HashMap<>();
        Con keyAttachment = (Con) key.attachment();
        Gson  gson = new Gson();
        StringBuilder stringBuilder = new StringBuilder();

        synchronized (friendList = user.getFriends()){
            for(String friendNick : friendList){
                User friend;
                System.out.println("AMICO DA PRENDERE "+friendNick);
                System.out.println("PRESENTE: "+mapUser.contains(friendNick));
                if(mapUser.contains(friendNick)){
                    synchronized (mapUser){
                        System.out.println("PRENDO LA LOCK "+user.getNickname());
                        friend = mapUser.get(friendNick);
                        System.out.println("MI SOSPENDO "+user.getNickname());
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    friend = userDispatcher.getUser(friendNick);
                }

                rank.put(friendNick,friend.getScore());
            }

            stringBuilder.append("OK\n");
            stringBuilder.append(gson.toJson(rank));
            stringBuilder.append("\n");

            keyAttachment.response = stringBuilder.toString();
            keyAttachment.lenght = stringBuilder.toString().length();
        }


        try {
            key.interestOps(SelectionKey.OP_WRITE);
        }catch (Exception e){
            user.decrementUse();
            e.printStackTrace();
            return;
        }
    }
}
