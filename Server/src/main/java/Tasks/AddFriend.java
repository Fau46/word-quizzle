package Tasks;

import Server.Con;
import User.User;

import java.nio.channels.SelectionKey;
import java.util.Vector;

public class AddFriend implements Runnable{
    private User user;
    private User friend;
    private SelectionKey key;

    public AddFriend(User user, User friend, SelectionKey key) {
        this.user = user;
        this.friend = friend;
        this.key = key;
    }


    @Override
    public void run() {
        Vector<String> friendsList;
        String friendName = friend.getNickname();
        String userName = user.getNickname();
        Con keyAttachment = (Con) key.attachment();


        if(!userName.equals(friendName)){
            System.out.println("["+userName+"] Prendo la lock di friend");
            synchronized (friendsList=friend.getFriends()){
                System.out.println("["+userName+"] Lock di friend presa");
                if(friendName.contains(userName)){
                    System.out.println("Utente amico già inserito");
                }
                else{
                    System.out.println("Inserisco "+userName+" nella lista degli amici di "+friendName);
                    friendsList.add(userName);
                }

//                try {
//                    System.out.println("["+userName+"] Il thread si sospende");
//                    Thread.sleep(10000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
            System.out.println("["+userName+"] Prendo la lock di user");
            friend.showFriend();

            synchronized (friendsList=user.getFriends()){
                System.out.println("["+userName+"] Prendo la lock di user");

                if(friendsList.contains(userName)){
                    System.out.println("Utente già inserito");
                    keyAttachment.response = "KO\n"+friendName+"e' gia' tuo amico";
                }
                else{
                    System.out.println("["+userName+"] Inserisco "+friendName+" nella lista degli amici di "+userName);
                    friendsList.add(friendName);
                    keyAttachment.response = "OK\n"+friendName+" inserito nella lista di amici";
                }
            }
        }
        else{
            System.out.println("Non puoi aggiungerti tra gli amici");
            keyAttachment.response = "KO\nNon puoi aggiungerti tra gli amici";
        }

        key.interestOps(SelectionKey.OP_WRITE);
        user.showFriend();
    }
}
