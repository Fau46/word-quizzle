import Database.Costants;
import sun.nio.ch.ThreadPool;
import sun.tools.jconsole.Worker;

import javax.swing.plaf.synth.SynthOptionPaneUI;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.*;


public class MainClassServer implements Costants,TCPConnection {
    public MainClassServer(){}

    public static void main(String[] args) {
        ServerSocket serverSocket;
        Selector selector;
        ThreadPoolExecutor executor;
        MainClassServer mainClassServer = new MainClassServer();
        System.out.println("[START] Server avviato");

        //Configurazione server RMI
        try {
            mainClassServer.RMIConfiguration();
            System.out.println("[START] Server RMI configurato");
        } catch (ExportException e) {
            System.out.println("[ERROR] Porta server RMI già in uso");
            return;
        } catch (AlreadyBoundException e) {
            System.out.println("[ERROR] Oggetto già presente nel registry");
            return;
        } catch (RemoteException e) {
            System.out.println("[ERROR] Errore nella configurazione del server RMI");
            return;
        }

        //Configurazione connessione TCP
        try {
            selector = mainClassServer.TCPConfiguration();
            System.out.println("[START] Socket TCP configurata, in ascolto sulla porta " + TCPConnection.PORT);
        } catch (IOException e) {
            System.out.println("[ERROR] Errore nella configurazione della socket TCP");
            return;
        }

//        try {
//            serverSocket = new ServerSocket(TCPConnection.PORT);
//            System.out.println("[START] Socket TCP configurata");
//        } catch (IOException e) {
//            System.out.println("[ERROR] Errore nella configurazione della socket TCP");
//            e.printStackTrace();
//            return;
//        }

        executor = new ThreadPoolExecutor(2, 10, 100L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>()); //TODO trasformare in modo da fare un multithread multiplexer
        System.out.println("[START] Threadpool avviato");

        System.out.println("[START] Server ready");

        Server server = new Server(selector, executor);
        server.run();

    }

    //Configurazione server RMI
    private void RMIConfiguration() throws ExportException, AlreadyBoundException, RemoteException {
        RMIRegistrationImpl reg = RMIRegistrationImpl.getServerRMI();
        Registry registry = LocateRegistry.createRegistry(RMIRegistrationInterface.PORT);
        RMIRegistrationInterface stub = (RMIRegistrationInterface) UnicastRemoteObject.exportObject(reg, 0);
        registry.bind(RMIRegistrationInterface.REMOTE_OBJECT_NAME, stub);
    }

    //Configurazione socket TCP
    private Selector TCPConfiguration() throws IOException {
        ServerSocketChannel serverChannel;
        Selector selector;

        serverChannel = ServerSocketChannel.open();
        ServerSocket serverSocket = serverChannel.socket();

        serverSocket.bind(new InetSocketAddress(HOSTNAME, PORT));
        serverChannel.configureBlocking(false);
        selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        return selector;
    }

}