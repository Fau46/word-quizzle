package User;

import Database.DBMS;

import java.util.concurrent.ConcurrentHashMap;

public class UserDispatcher {
    private ConcurrentHashMap<String, User> userMap;
    private DBMS dbms;
    private static UserDispatcher userDispatcher;


    private UserDispatcher(){
        userMap = new ConcurrentHashMap<>();
        this.dbms = DBMS.getIstance();
    }

    public static UserDispatcher getIstance(){
        if(userDispatcher == null) userDispatcher = new UserDispatcher();
        return userDispatcher;
    }

    public User getUser(String nickname){
        User user;
        if((user=userMap.get(nickname)) == null){
            user = dbms.getUser(nickname);
            if(user != null) userMap.put(nickname, user);
        }

        return user;
    }

    public void Serialize(User user) {
        dbms.serializeUser(user);
    }
}
