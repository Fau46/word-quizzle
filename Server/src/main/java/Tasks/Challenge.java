package Tasks;

import Server.Con;
import User.User;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Challenge implements Runnable {
    private User user, friend;
    private SelectionKey userKey, friendKey;
    private int BUF_SIZE = 256;
    private int SELECTOR_TIMEOUT = 5000;
    private Selector serverSelector;
    private SelectionKey newFriendKey;

    public Challenge(User user, User friend, SelectionKey userKey, SelectionKey friendKey, Selector selector){
        this.user = user;
        this.friend = friend;
        this.userKey = userKey;
        this.friendKey = friendKey;
        this.serverSelector = selector;
    }

    @Override
    public void run() {
        DatagramSocket datagramSocket;
        InetAddress address;
        DatagramPacket datagramPacket;
//        byte[] readerBuffer = new byte[BUF_SIZE];
        Con keyAttachment = (Con) userKey.attachment();

        try {
            datagramSocket = new DatagramSocket();
            address = InetAddress.getByName("localhost");//TODO mettere costante

            String request = user.getNickname()+"\n";
            byte[] byteRequest = request.getBytes();

            int friendSocketPort = ((SocketChannel) friendKey.channel()).socket().getPort();//Prendo la porta della socket di friend
            datagramPacket = new DatagramPacket(byteRequest,byteRequest.length,address,friendSocketPort);
            datagramSocket.send(datagramPacket); //Invio il pacchetto di richiesta di sfida a friend

//            keyAttachment.response = "KO\nTEST";
//            userKey.interestOps(SelectionKey.OP_WRITE);
            selectorChallenge();
//            DatagramPacket receivePacket = new DatagramPacket(readerBuffer, BUF_SIZE);
//
//            friendKey.interestOps(0);
//            try{
//                datagramSocket.setSoTimeout(TIME_OUT);
//                datagramSocket.receive(receivePacket);
//            }catch (SocketTimeoutException e){
//
//            }
//            friendKey.interestOps(SelectionKey.OP_READ);
//
//            System.out.println("Stringa ricevuta:"+new String(readerBuffer));

        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        try{
//            userKey.interestOps(SelectionKey.OP_WRITE);
//        }catch (Exception e){
//            user.decrementUse();
//            friend.decrementUse();
//            e.printStackTrace();
//            return;
//        }
//
//        user.decrementUse();
//        friend.decrementUse();
    }

    private void selectorChallenge(){
        Selector selector = null;
        Con keyAttachmentFriend;

        try {
            selector = Selector.open();

            keyAttachmentFriend = (Con) this.friendKey.attachment();
            this.friendKey.interestOps(0); //resetto l'interestop della chiave di friend

            SocketChannel friendSocket = (SocketChannel) this.friendKey.channel();
            newFriendKey = friendSocket.register(selector, SelectionKey.OP_READ); //registro la key di friend col nuovo selettore
            newFriendKey.attach(keyAttachmentFriend);

            runSelector(selector);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runSelector(Selector selector){
        boolean received = false;

        do{
            try {
                if(received) selector.select(100);
                else selector.select(SELECTOR_TIMEOUT);
            } catch (IOException e) {
                System.out.println("[ERROR] Errore nel selettore");
                return;
            }

            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();

            while (iterator.hasNext()) {
                if (!received) received = true;

                SelectionKey key = iterator.next();
                try {
                    if (key.isAcceptable()) {
                        iterator.remove();
                    } else if (key.isReadable()) {
                        this.Readable(key, iterator);
                    } else if (key.isWritable()) {
//                        this.Writable(key);
                        iterator.remove();
                    }
                } catch (IOException e) {
                    key.cancel();
                    try {
                        //Gestisco la chiusura del canale
//                        Con keyAttachment = (Con) key.attachment();
//                        String nick = keyAttachment.nickname;
//                        if (nick != null) {
//                            User user = mapUser.get(nick);
//                            user.decrementUse(); //Decremento use in user
//                            user.setPort(0);
//                            mapUser.remove(nick); //Rimuovo dagli user online
//                            mapKey.remove(nick); //Rimuovo la sua chiave
//                        }
//
//                        System.out.println("[CLOSED CLIENT]: (" + nick + ") " + ((SocketChannel) key.channel()).getRemoteAddress());

                        key.channel().close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }while (received);

        if(!received){
            negativeResponse("KO\nTempo scaduto");

        }
    }


    private void Readable(SelectionKey key, Iterator<SelectionKey> iterator) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        byte[] byteBuffer = new byte[BUF_SIZE];
        ByteBuffer intput = ByteBuffer.wrap(byteBuffer);
        Con keyAttachment = (Con) key.attachment();
        StringBuilder requestBuilder;

        String request = keyAttachment.request;
        if(request ==  null) requestBuilder = new StringBuilder();
        else requestBuilder = new StringBuilder(request);
        int read=client.read(intput); //Leggo dalla socket

        if (read ==- 1) throw new IOException("Canale chiuso"); //Mi accerto che il canale non sia chiuso
        else if(read == 0){ //Se ho finito di leggere parso la request TODO e se non sono arrivati tutti i pacchetti? (suggerimento nicola)
            parser(key);
            iterator.remove();
        }
        else{ //Allego ciò che ho letto alla request della key
            String string = new String(byteBuffer,0,read);
            requestBuilder.append(string);
            keyAttachment.request = requestBuilder.toString();


        }
    }

    private void parser(SelectionKey key) {
        Con keyAttachment = (Con) key.attachment();
        String[] aux = keyAttachment.request.split("\n");

        if(aux[0].equals("KO")){
            String string = "OK\nOK richiesta rifiutata\n";
            ByteBuffer buffer = ByteBuffer.allocate(string.length());

            buffer.put(string.getBytes());
            buffer.flip();

            while (buffer.hasRemaining()){
                try {
                    ((SocketChannel)key.channel()).write(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            negativeResponse("KO\nSfida non accettata");
        }

    }


    private void negativeResponse(String response) {
        Con keyAttachment = (Con) userKey.attachment();
        Con keyAttachmentFriend = (Con) this.newFriendKey.attachment();

        keyAttachment.response =  response; //Allego la risposta negativa a user

        this.newFriendKey.interestOps(0);

        SocketChannel friendSocket = (SocketChannel) this.newFriendKey.channel();

        try {
            SelectionKey friendKey = friendSocket.register(serverSelector,SelectionKey.OP_READ);
            keyAttachmentFriend.response = null;
            keyAttachmentFriend.request = null;
            friendKey.attach(keyAttachmentFriend);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }


        try{
            userKey.interestOps(SelectionKey.OP_WRITE);
            user.decrementUse();
            friend.decrementUse();
        }catch (Exception e){
            user.decrementUse();
            friend.decrementUse();
            e.printStackTrace();
            return;
        }
    }

}

