package Tasks;

import Server.Con;
import User.*;

import java.nio.channels.SelectionKey;
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

        if(!userName.equals(friendName)){//Controllo che un utente non si aggiunga da solo alla lista di amici

            synchronized (friendsList=user.getFriends()){ //Prendo la lista di amici di user
                if(friendsList.contains(friendName)){ //Controllo che non siano già amici
                    keyAttachment.response = "KO\n"+friendName+" e' gia' tuo amico";
                }
                else{
                    friendsList.add(friendName);
                    userDispatcher.Serialize(user); //TODO provare a fare la serializzazione di user mentre quealucno sta provando ad aggiornare score
                    keyAttachment.response = "OK\n"+friendName+" inserito nella lista di amici";
                }
            }

            synchronized (friendsList=friend.getFriends()){ //prendo la lista di amici di friendName
                if(!friendsList.contains(userName)){//Se username non risulta tra gli amici di friend allora lo inserisco
                    friendsList.add(userName);
                    userDispatcher.Serialize(friend); //TODO migliorare la serializzazione
                }
            }
        }
        else{
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

        user.decrementUse();
        friend.decrementUse();
    }

}
