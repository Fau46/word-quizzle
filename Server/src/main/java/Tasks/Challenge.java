package Tasks;

import Server.Con;
import User.User;

import java.io.IOException;
import java.net.*;
import java.nio.channels.SelectionKey;

public class Challenge implements Runnable {
    private User user, friend;
    private SelectionKey key;
    private int BUF_SIZE = 256;
    private int TIME_OUT = 2000;

    public Challenge(User user, User friend, SelectionKey key){
        this.user = user;
        this.friend = friend;
        this.key = key;
    }

    @Override
    public void run() {
        DatagramSocket datagramSocket;
        InetAddress address;
        DatagramPacket datagramPacket;
        byte[] readerBuffer = new byte[BUF_SIZE];
        Con keyAttachment = (Con) key.attachment();

        try {
            datagramSocket = new DatagramSocket();
            address = InetAddress.getByName("localhost");

            String request = user.getNickname()+"\n"+datagramSocket.getLocalPort()+"\n";
            byte[] byteRequest = request.getBytes();

            datagramPacket = new DatagramPacket(byteRequest,byteRequest.length,address,friend.getPort());
            datagramSocket.send(datagramPacket);

            DatagramPacket receivePacket = new DatagramPacket(readerBuffer, BUF_SIZE);

            try{
                datagramSocket.setSoTimeout(TIME_OUT);
                datagramSocket.receive(receivePacket);
            }catch (SocketTimeoutException e){

            }

            System.out.println("Stringa ricevuta:"+new String(readerBuffer));
            keyAttachment.response = "KO\nTEST";

        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
