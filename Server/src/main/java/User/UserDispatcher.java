package User;

import Costanti.Costanti;
import Database.DBMS;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserDispatcher  implements Costanti {
    private ConcurrentHashMap<String, User> userMap;
    private DBMS dbms;
    private static UserDispatcher userDispatcher;



    private UserDispatcher(){
        this.userMap = new ConcurrentHashMap<>();
        this.dbms = DBMS.getIstance();

        Thread cleaner = new Thread(new Runnable() { //Thread che si occupa di ripulire la userMap
            @Override
            public void run() {
                while (true){
                    try {
                        Thread.sleep(SLEEP_TIMER * 1000);
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


    //Metodo che ritorna l'istanza della classe
    public static UserDispatcher getIstance(){
        if(userDispatcher == null) userDispatcher = new UserDispatcher();
        return userDispatcher;
    }


    //Metodo che ritorna la classe user associata a nickname, null se non vi è un'associazione
    public User getUser(String nickname){
        User user;
        synchronized (userMap){
            if((user=userMap.get(nickname)) == null){
                user = dbms.getUser(nickname);

                if(user != null) userMap.put(nickname, user); //Se l'utente associato a nickname esiste lo aggiungo alla struttura userMap
            }
        }

        return user;
    }


    //Metodo che richiede la serializzazione di user al dbms
    public void Serialize(User user) {
        dbms.serializeUser(user);
    }


    //Metodo che si occupa della pulizia periodica della user map
    private void clean(){
        synchronized (userMap){
            for (Map.Entry<String, User> entry : userMap.entrySet()) {
                User value = entry.getValue();
                String key = entry.getKey();

                if(value.getUse() == 0){ //Rimuovo esclusivamente gli utenti con il parametro use a 0
//                    System.out.println("Rimuovo "+value.getNickname());
                    this.Serialize(value);
                    userMap.remove(key);
                }
            }
        }
    }
}
