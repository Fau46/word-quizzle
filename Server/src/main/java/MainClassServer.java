import Database.Costants;
import sun.tools.jconsole.Worker;

import javax.swing.plaf.synth.SynthOptionPaneUI;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.*;


public class MainClassServer implements Costants,TCPConnection {

    public MainClassServer(){
    }

//    class TCPConf{
//        ServerSocket serverSocket;
//        Selector selector;
//
//        public TCPConf(ServerSocket serverSocket, Selector selector){
//            this.selector = selector;
//            this.serverSocket = serverSocket;
//        }
//
//    }

    private void RMIConfiguration() throws ExportException, AlreadyBoundException,RemoteException{
            //Registrazione stub oggetto remoto
            RMIRegistrationImpl reg = RMIRegistrationImpl.getServerRMI();
            Registry registry = LocateRegistry.createRegistry(RMIRegistrationInterface.PORT);
            RMIRegistrationInterface stub = (RMIRegistrationInterface) UnicastRemoteObject.exportObject(reg,0);
            registry.bind(RMIRegistrationInterface.REMOTE_OBJECT_NAME,stub);
    }

//    private TCPConf TCPConfiguration() throws IOException{
//        ServerSocketChannel serverChannel;
//        Selector selector;
//
//        serverChannel = ServerSocketChannel.open();
//        ServerSocket serverSocket = serverChannel.socket();
//
//        serverSocket.bind(new InetSocketAddress(HOSTNAME,PORT));
//        serverChannel.configureBlocking(false);
//        selector = Selector.open();
//        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
//
//        return new TCPConf(serverSocket,selector);
//    }

    public static void main(String[] args){
        ThreadPoolExecutor executor;
        ServerSocket serverSocket;
        Selector selector;

        System.out.println("[START] Server avviato");
        MainClassServer server = new MainClassServer();

        //Configurazione server RMI
        try {
            server.RMIConfiguration();
            System.out.println("[START] Server RMI configurato");
        }catch (ExportException e){
            System.out.println("[ERROR] Porta server RMI già in uso");
            return;
        }catch (AlreadyBoundException e) {
            System.out.println("[ERROR] Oggetto già presente nel registry");
            return;
        }catch (RemoteException e) {
            System.out.println("[ERROR] Errore nella configurazione del server RMI");
            return;
        }

        //Configurazione connessione TCP
//        try {
//            selector = server.TCPConfiguration().selector;
//            serverSocket = server.TCPConfiguration().serverSocket;
//            System.out.println("[START] Socket TCP configurata");
//        } catch (IOException e) {
//            System.out.println("[ERROR] Errore nella configurazione della socket TCP");
//            return;
//        }

        try {
            serverSocket = new ServerSocket(TCPConnection.PORT);
            System.out.println("[START] Socket TCP configurata");
        } catch (IOException e) {
            System.out.println("[ERROR] Errore nella configurazione della socket TCP");
            e.printStackTrace();
            return;
        }

        executor = new ThreadPoolExecutor(2,10,100L, TimeUnit.MILLISECONDS,new LinkedBlockingQueue<Runnable>()); //TODO trasformare in modo da fare un multithread multiplexer
        System.out.println("[START] Threadpool avviato");

        System.out.println("[START] Server ready");

        while (true){
            try{
                Socket socketClient = serverSocket.accept();
                executor.execute(new Task(socketClient));
            } catch (IOException e) {
                System.out.println("[ERROR] Errore nella accept della serverSocket");
                e.printStackTrace();
            }
        }


//        try {
//            serverSocket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


    }


}