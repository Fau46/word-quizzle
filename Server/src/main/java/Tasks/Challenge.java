package Tasks;

import Server.Con;
import User.User;

import java.io.IOException;
import java.net.*;
import java.nio.channels.SelectionKey;

public class Challenge implements Runnable {
    private User user, friend;
    private SelectionKey userKey, friendKey;
    private int BUF_SIZE = 256;
    private int TIME_OUT = 2000;

    public Challenge(User user, User friend, SelectionKey userKey, SelectionKey friendKey){
        this.user = user;
        this.friend = friend;
        this.userKey = userKey;
        this.friendKey = friendKey;
    }

    @Override
    public void run() {
        DatagramSocket datagramSocket;
        InetAddress address;
        DatagramPacket datagramPacket;
        byte[] readerBuffer = new byte[BUF_SIZE];
        Con keyAttachment = (Con) userKey.attachment();

        try {
            datagramSocket = new DatagramSocket();
            address = InetAddress.getByName("localhost");

            String request = user.getNickname()+"\n"+datagramSocket.getLocalPort()+"\n";
            byte[] byteRequest = request.getBytes();

            datagramPacket = new DatagramPacket(byteRequest,byteRequest.length,address,friend.getPort());
            datagramSocket.send(datagramPacket);

            DatagramPacket receivePacket = new DatagramPacket(readerBuffer, BUF_SIZE);

            friendKey.interestOps(0);
            try{
                datagramSocket.setSoTimeout(TIME_OUT);
                datagramSocket.receive(receivePacket);
            }catch (SocketTimeoutException e){

            }
            friendKey.interestOps(SelectionKey.OP_READ);

            System.out.println("Stringa ricevuta:"+new String(readerBuffer));
            keyAttachment.response = "KO\nTEST";

        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try{
            userKey.interestOps(SelectionKey.OP_WRITE);
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
