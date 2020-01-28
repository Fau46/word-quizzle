import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPListener implements Runnable,TCPConnection{
    private int port;
    DatagramSocket datagramSocket;

    public UDPListener(int port){
        this.port = port;
    }

    @Override
    public void run() {
        try {
            datagramSocket = new DatagramSocket(port);
            System.out.println("[OK] Listener UDP in ascolto");
        } catch (SocketException e) {
            System.out.println("[ERROR] Errore nella configurazione del listener UDP");
            e.printStackTrace();
        }

        byte[] buffer = new byte[512];
        DatagramPacket datagramPacket = new DatagramPacket(buffer,buffer.length);

        while (true){
            try {
                datagramSocket.receive(datagramPacket);
                String string = new String(datagramPacket.getData(),0,datagramPacket.getLength(),"UTF-8");
                System.out.println(string);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
