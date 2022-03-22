import java.io.*;
import java.net.*;

public class UDPFileSender extends Thread{
    private String filePath;
    private String fileName;
    private String receiverIp;
    private int port;
    private DatagramSocket aSocket;

    private static int maxTimeouts = 5;
    private static int timeout = 3000;
    private static int bufSize = 8192;

    public UDPFileSender(String receiverIp, int port, String filePath, String fileName){
        this.filePath = filePath;
        this.receiverIp = receiverIp;
        this.port = port;

        this.start();
    }

    public void run(){
        int timeoutCounter = 0;
        try{
            aSocket = new DatagramSocket();
            aSocket.setSoTimeout(timeout);
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
            return;
        }

        try {
            File file = new File(filePath + "\\" + fileName);
            FileInputStream fis = new FileInputStream(file);

            byte[] buffer = new byte[bufSize];
            byte[] replyBuf = new byte[1];

            InetAddress aHost = InetAddress.getByName(receiverIp);

            DatagramPacket packet;
            DatagramPacket reply = new DatagramPacket(replyBuf, replyBuf.length);

            int n;
            while((n = fis.read(buffer)) > 0){
                while(true){
                    packet = new DatagramPacket(buffer, n, aHost, port);
                    aSocket.send(packet);


                    //HERE
                    try{
                        aSocket.receive(reply);
                        timeoutCounter = 0;
                    } catch (IOException e){
                        System.out.println("UDPFileSender - IO: " + e.getMessage());
                        timeoutCounter++;
                        //if the receive operation times out too much times end this thread
                        if(timeoutCounter > maxTimeouts){
                            fis.close();
                            return;
                        }
                    }

                }
            }

            System.out.println("CLIENT DOWNLOAD SOCKET CLOSING");

            fis.close();
        } catch (IOException e) {
            System.out.println("IO:" + e.getMessage());
        }
    }

    
}
                