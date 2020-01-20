import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Task implements Runnable {
    private Socket socketClient;

    public Task(Socket socketClient){
        this.socketClient = socketClient;
    }

    @Override
    public void run() {
        try(
                BufferedReader reader = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
        ){
            String command;
            while ((command=reader.readLine()) != null){
                System.out.println(command);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
