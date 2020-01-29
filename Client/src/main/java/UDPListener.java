import javax.swing.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPListener implements Runnable,TCPConnection{
    private int port;
    DatagramSocket datagramSocket;
    private JFrame window;

    public UDPListener(int port, JFrame window){
        this.window = window;
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
        String request = null;

        while (true){
            try {
                datagramSocket.receive(datagramPacket);
                request = new String(datagramPacket.getData(),0,datagramPacket.getLength(),"UTF-8");
//                System.out.println(string);
            } catch (IOException e) {
                e.printStackTrace();
            }

//            if(!(Challenge.getInstance()).isChallenge());
            String[] aux = request.split("\n");
            String[] buttons = {"ACCETTA", "RIFIUTA"};
            JOptionPane.showOptionDialog(window, aux[0]+" ti vuole sfidare", "Sfida",JOptionPane.INFORMATION_MESSAGE, JOptionPane.PLAIN_MESSAGE, null, buttons,null);
//            JOptionPane.showMessageDialog(window, aux[0]+" ti vuole sfidare", "Sfida", JOptionPane.YES_NO_OPTION);
        }
    }
}
