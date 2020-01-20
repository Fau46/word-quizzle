import java.net.Socket;

public class Worker implements Runnable {
    private Socket clientSocket;

    public Worker(Socket clientSocket){
        this.clientSocket = clientSocket;
    }
    @Override
    public void run() {

    }
}
