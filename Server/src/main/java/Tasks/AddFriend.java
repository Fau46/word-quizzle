package Tasks;

import Server.Con;
import User.*;

import java.nio.channels.SelectionKey;
import java.util.Vector;
import java.util.Vector;

public class AddFriend implements Runnable{
    private User user;
    private User friend;
    private SelectionKey key;
    private UserDispatcher userDispatcher;

    public AddFriend(User user, User friend, SelectionKey key) {
        this.user = user;
        this.friend = friend;
        this.key = key;
        this.userDispatcher = UserDispatcher.getIstance();
    }


    @Override
    public void run() {
        Vector<String> friendsList;
        String friendName = friend.getNickname();
        String userName = user.getNickname();
        Con keyAttachment = (Con) key.attachment();

//        showFriend(friend.getFriends(),friendName,userName);
//        showFriend(user.getFriends(),userName,userName);

        if(!userName.equals(friendName)){//Controllo che un utente non si aggiunga da solo alla lista di amici

            System.out.println("["+userName+"] Prendo la lock di "+userName);
            synchronized (friendsList=user.getFriends()){ //Prendo la lista di amici di user
                System.out.println("["+userName+"] Lock di "+userName+" presa");

                if(friendsList.contains(friendName)){ //Controllo che non siano già amici
//                    System.out.println("["+userName+"] Utente "+friendName+" già amico di "+userName);
                    keyAttachment.response = "KO\n"+friendName+" e' gia' tuo amico";
                }
                else{
//                    System.out.println("["+userName+"] Inserisco "+friendName+" nella lista degli amici di "+userName);
                    friendsList.add(friendName);
                    userDispatcher.Serialize(user); //TODO provare a fare la serializzazione di user mentre quealucno sta provando ad aggiornare score
                    keyAttachment.response = "OK\n"+friendName+" inserito nella lista di amici";
                }
            }

            System.out.println("["+userName+"] Prendo la lock di "+friendName);
            synchronized (friendsList=friend.getFriends()){ //prendo la lista di amici di friendName
                System.out.println("["+userName+"] Lock di "+friendName+" presa");
                if(friendsList.contains(userName)){
//                    System.out.println("["+userName+"] Utente "+userName+" già amico di "+friendName);
                }
                else{
//                    System.out.println("["+userName+"]Inserisco "+userName+" nella lista degli amici di "+friendName);
                    friendsList.add(userName);
                    userDispatcher.Serialize(friend);

                }
            }
//            friend.showFriend();


//            if(keyAttachment.response.contains("OK")){
//
//                synchronized (friend.getFriends()){
////                    System.out.println("["+userName+"] Serializzo "+friendName);
////                    try {
////                        System.out.println("["+userName+"] Il thread si sospende");
////                        Thread.sleep(20000);
////                    } catch (InterruptedException e) {
////                        e.printStackTrace();
////                    }
////                    System.out.println("["+userName+"] Il thread riprende");
//                }
//                synchronized (user.getFriends()){
//                }
//            }

        }
        else{
            System.out.println("Non puoi aggiungerti tra gli amici");
            keyAttachment.response = "KO\nNon puoi aggiungerti tra gli amici";
        }

        try{
            key.interestOps(SelectionKey.OP_WRITE);
        }catch (Exception e){
            user.decrementUse();
            friend.decrementUse();
            e.printStackTrace();
            return;
        }
//        user.showFriend();
        user.decrementUse();
        friend.decrementUse();
    }

    public void showFriend(Vector<String> friendsList, String nickname, String user){
        System.out.println("["+user+"] Amici utente:"+nickname);
        for(String string : friendsList){
            System.out.println(string);
        }
    }
}
