package Tasks;

import Server.ConKey;
import User.*;
import com.google.gson.Gson;

import java.nio.channels.SelectionKey;
import java.util.*;

public class ShowRank implements Runnable{
    private User user;
    private SelectionKey key;
    private UserDispatcher userDispatcher;

    public ShowRank(User user, SelectionKey key){
        this.user = user;
        this.key = key;
        this.userDispatcher = UserDispatcher.getIstance();
    }

    @Override
    public void run() {
        TreeMap<String,Integer> unsortedRank = new TreeMap<>();
        StringBuilder stringBuilder = new StringBuilder();
        ConKey keyAttachment = (ConKey) key.attachment();
        Vector<String> friendList;
        Gson  gson = new Gson();

        unsortedRank.put(user.getNickname(),user.getScore().intValue());

        synchronized (friendList = user.getFriends()){ //Prendo la lista degli amici di user
            for(String friendNick : friendList){
                User friend = userDispatcher.getUser(friendNick); //Richiedo

                friend.incrementUse();
                unsortedRank.put(friendNick,friend.getScore().intValue());
                friend.decrementUse();
            }

            TreeMap<String,Integer> sortedRank = (TreeMap<String,Integer>) sortByValues(unsortedRank); //faccio il sort della lista di amici in base al loro punteggio

            stringBuilder.append("OK\n");
            stringBuilder.append(gson.toJson(sortedRank));
            stringBuilder.append("\n");

            keyAttachment.response = stringBuilder.toString();
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


    //Alrgoritmo che ordina in maniera decrescente di valore la map
    private static Map<String, Integer> sortByValues(Map<String,Integer> map){
        Comparator<String> valueComparator = new Comparator<String>() {
            public int compare(String s1, String s2) {
                if(map.get(s1) >= map.get(s2)){
                    return -1;
                }else{
                    return 1;
                }
            }
        };

        Map<String,Integer> sortedByValues = new TreeMap<String, Integer>(valueComparator);
        sortedByValues.putAll(map);
        return sortedByValues;
    }
}
