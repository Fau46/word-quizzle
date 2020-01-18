import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIRegistrationInterface extends Remote {
    int PORT = 8000;
    String REMOTE_OBJECT_NAME = "RemoteRegister";

    //Status Code
    int INVALID_NICK = -101;
    int EXISTS_NICK = -102;
    int INVALID_PWD = -103;
    int TOO_SHORT_PWD = -104;
    int OK = 1;

    int Registration(String nick, String pwd) throws RemoteException;
}
