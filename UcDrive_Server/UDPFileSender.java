import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.Arrays;

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
        this.fileName = fileName;
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
            System.out.println("UDPFileSender - Socket: " + e.getMessage());
            return;
        }

        try {
            File file = new File(filePath + "\\" + fileName);
            FileInputStream fis = new FileInputStream(file);

            byte[] buffer = new byte[bufSize];
            byte[] ackBuf = new byte[1];

            InetAddress aHost = InetAddress.getByName(receiverIp);

            DatagramPacket packet;
            DatagramPacket ack = new DatagramPacket(ackBuf, ackBuf.length);



            //send file name
            buffer = (filePath + "\\" + fileName).getBytes();
            packet = new DatagramPacket(buffer, buffer.length);
            while(true){
                aSocket.send(packet);
                try{
                    aSocket.receive(ack);
                    timeoutCounter = 0;
                    break;
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

            int n;
            //send file
            while((n = fis.read(buffer)) > 0){
                while(true){
                    packet = new DatagramPacket(buffer, n, aHost, port);
                    System.out.println("UDPFileSender - Sent " + packet.getLength() + " bytes");
                    aSocket.send(packet);

                    //if this thread receives ACK, break this loop and continue sending parts of the file
                    try{
                        aSocket.receive(ack);
                        timeoutCounter = 0;
                        break;
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

            buffer = createEmptyByteArray(bufSize);
            packet = new DatagramPacket(buffer, buffer.length, aHost, port);
            
            //send empty packet, to signal stop
            aSocket.send(packet);


            packet = new DatagramPacket(buffer, buffer.length);
            //receive calculated MD5 Hash from receiver
            aSocket.receive(packet);

            byte hash[] = getMD5Hash(filePath + "\\" + fileName);
            
            //hashs are different
            if(!Arrays.equals(packet.getData(), hash)){
                System.out.println("UDPFileSender - File hash different.");
                //send wrong signal
                ackBuf = new byte[] {(byte) 0xFF};
            }
            else{
                //send valid signal
                ackBuf = new byte[] {(byte) 0xAA};
            }

            packet = new DatagramPacket(ackBuf, ackBuf.length, aHost, port);
            aSocket.send(packet);


            System.out.println("UDPFileSender - SOCKET CLOSING");
            aSocket.close();
            fis.close();
        } catch (IOException e) {
            System.out.println("UDPFileSender - IO: " + e.getMessage());
        }
    }

    private byte[] createEmptyByteArray(int arraySize){
        byte array[] = new byte[arraySize];

        for(int i = 0; i < arraySize; i++){
            array[i] = 0x00;
        }

        return array;
    }
    
    /* 
        Method referenced from StackOverflow.
    */
    private byte[] getMD5Hash(String filePath){
        MessageDigest md;
        try{
            md = MessageDigest.getInstance("MD5");
            try (InputStream is = Files.newInputStream(Paths.get("file.txt"));
                    DigestInputStream dis = new DigestInputStream(is, md)) {
            }
        } catch(IOException e){
            System.out.println("UDPFileSender - IO: " + e.getMessage());
            return null;
        } catch(NoSuchAlgorithmException e){
            System.out.println("UDPFileSender - NoSuchAlgorithm: " + e.getMessage());
            return null;
        }
        return md.digest();
    }
    
}
                