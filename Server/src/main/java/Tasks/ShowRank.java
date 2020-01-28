package Tasks;

import Server.Con;
import User.*;
import com.google.gson.Gson;

import java.nio.channels.SelectionKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
        Con keyAttachment = (Con) key.attachment();
        Vector<String> friendList;
        Gson  gson = new Gson();

        synchronized (friendList = user.getFriends()){
            for(String friendNick : friendList){
                User friend;

                friend = userDispatcher.getUser(friendNick);

                friend.incrementUse();
                unsortedRank.put(friendNick,friend.getScore().intValue());
                friend.decrementUse();
            }

            TreeMap<String,Integer> sortedRank = (TreeMap<String,Integer>) sortByValues(unsortedRank);

            stringBuilder.append("OK\n");
            stringBuilder.append(gson.toJson(sortedRank));
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

        user.decrementUse();
    }

    //https://javahungry.blogspot.com/2017/11/how-to-sort-treemap-by-value-in-java.html
    //Alrgoritmo che ordina in maniera decrescente di valore la map
    public static <K, V extends Comparable<V>> Map<K, V> sortByValues(final Map<K, V> map){
        Comparator<K> valueComparator = new Comparator<K>() {
            public int compare(K k1, K k2) {
                int compare =
                        map.get(k1).compareTo(map.get(k2));
                if (compare == 0)
                    return 1;
                else
                return -compare; //per ottenere l'ordine in maniera decrescente
            }
        };

        Map<K, V> sortedByValues =
                new TreeMap<K, V>(valueComparator);
        sortedByValues.putAll(map);
        return sortedByValues;
    }
}
