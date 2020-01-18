import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;

public class MainClassServer implements Costants{

    public MainClassServer(){
        //Creazione directory Database

        String pathJsonDB = path+jsonDB;

        if(Files.notExists(Paths.get(pathJsonDB))){
            try {
                new File(pathJsonDB).createNewFile();
                System.out.println("File '"+jsonDB+"' creato");
            } catch (IOException e) {
                System.out.println("Errore nella creazione del file '"+jsonDB+"'");
            }
        }

    }

    private void RMIConfiguration(){
        //Registrazione stub oggetto remoto
        try {
            RMIRegistrationImpl reg = RMIRegistrationImpl.getServerRMI();
            Registry registry = LocateRegistry.createRegistry(RMIRegistrationInterface.PORT);
            RMIRegistrationInterface stub = (RMIRegistrationInterface) UnicastRemoteObject.exportObject(reg,0);
            registry.bind(RMIRegistrationInterface.REMOTE_OBJECT_NAME,stub);

            System.out.println("Server ready");
        }catch (ExportException e){
            System.out.println("Porta già in uso");
            return;
        }catch (AlreadyBoundException e) {
            System.out.println("Oggetto già presente nel registry");
            return;
        }catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        MainClassServer server = new MainClassServer();
        server.RMIConfiguration();

    }


}