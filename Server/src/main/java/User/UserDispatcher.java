package User;

import Database.DBMS;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserDispatcher {
    private ConcurrentHashMap<String, User> userMap;
    private DBMS dbms;
    private static UserDispatcher userDispatcher;
    private int sleepTimer = 3;


    private UserDispatcher(){
        this.userMap = new ConcurrentHashMap<>();
        this.dbms = DBMS.getIstance();

        Thread cleaner = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        Thread.sleep(sleepTimer * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    clean();
                }
            }
        });

        cleaner.setDaemon(true);
        cleaner.start();
    }

    public static UserDispatcher getIstance(){
        if(userDispatcher == null) userDispatcher = new UserDispatcher();
        return userDispatcher;
    }

    public User getUser(String nickname){
        User user;
        synchronized (userMap){
            if((user=userMap.get(nickname)) == null){
                user = dbms.getUser(nickname);
                if(user != null) userMap.put(nickname, user);
            }
        }

        return user;
    }

    public void Serialize(User user) {
        dbms.serializeUser(user);
    }

    private void clean(){
        synchronized (userMap){
            for (Map.Entry<String, User> entry : userMap.entrySet()) {
                User value = entry.getValue();
                String key = entry.getKey();

                if(value.getUse() == 0){
                    System.out.println("Rimuovo "+value.getNickname());
                    this.Serialize(value);
                    userMap.remove(key);
                }
            }
        }
    }
}
