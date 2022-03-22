import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.Arrays;

public class UDPFileReceiver extends Thread{
    private String filePath;
    private int port;
    private DatagramSocket aSocket;

    private static int timeout = 60000;
    private static int bufSize = 8192;

    public UDPFileReceiver(int port){
        this.port = port;

        this.start();
    }

    public void run(){
        try{
            aSocket = new DatagramSocket(port);
            aSocket.setSoTimeout(timeout);
        } catch (SocketException e) {
            System.out.println("UDPFileReceiver - Socket: " + e.getMessage());
            return;
        }

        byte[] buffer = new byte[bufSize];
        byte[] ackBuffer = new byte[] {(byte) 0xAA};
        byte[] emptyBuffer = createEmptyByteArray(bufSize);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        DatagramPacket ack;

        try {
            //get filepath and filename
            aSocket.receive(packet);

            //store file path
            filePath = new String(packet.getData());
            File newFile = new File(filePath);
            FileOutputStream fos = new FileOutputStream(newFile);


            // send acknowledgement
            ack = new DatagramPacket(ackBuffer, ackBuffer.length, packet.getAddress(), packet.getPort());
            aSocket.send(ack);

            while(true){
                packet = new DatagramPacket(buffer, buffer.length);

                aSocket.receive(packet);
                System.out.println("UDPFileReceiver - Received " + packet.getLength() + " bytes");

                //if packet is empty
                if(Arrays.equals(packet.getData(), emptyBuffer))
                    break;


                //write data to file
                fos.write(packet.getData(), 0, packet.getLength());
                aSocket.send(ack);
            }
            fos.close();

            //calculate MD5 hash for the new file
            byte hash[] = getMD5Hash(filePath);
            packet = new DatagramPacket(hash, hash.length, ack.getAddress(), ack.getPort());
            aSocket.send(packet);

            //receive signal from sender
            ack = new DatagramPacket(ackBuffer, ackBuffer.length);
            aSocket.receive(ack);

            if(ack.getData()[0] == (byte)0xAA){
                System.out.println("UDPFileReceiver - File correct.");
            }
            else{
                System.out.println("UDPFileReceiver - File bad.");
            }




        } catch (IOException e) {
            System.out.println("UDPFileReceiver - IO: " + e.getMessage());
            return;
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
