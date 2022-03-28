/*
 *  "UDPFileReceiver.java"
 * 
 *  ====================================
 *
 *  Universidade de Coimbra
 *  Faculdade de Ciências e Tecnologia
 *  Departamento de Engenharia Informatica
 * 
 *  Alexandre Gameiro Leopoldo - 2019219929
 *  Luís Miguel Gomes Batista  - 2019214869
 * 
 *  ====================================
 * 
 *  "ucDrive Project"
 */

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.Arrays;

/**
 * File replicate receiver class
 */
public class UDPFileReceiver extends Thread {
    private String filePath;
    private int port;
    private DatagramSocket aSocket;
    private static int timeout = 60000;
    private static int bufSize = 8192;

    public UDPFileReceiver(int port) {
        this.port = port;
        this.start();
    }

    public void run() {
        try {
            aSocket = new DatagramSocket(port);
            aSocket.setSoTimeout(timeout);
        } catch (SocketException e) {
            System.out.println("<UDPFileReceiver> Socket: " + e.getMessage());
            return;
        }

        byte buffer[] = new byte[bufSize];
        byte ackBuffer[] = new byte[] {(byte) 0xAA};
        byte emptyBuffer[] = createEmptyByteArray(bufSize);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        DatagramPacket ack;

        while (true) {
            try {
                // get filepath and filename
                aSocket.receive(packet);

                // store file path
                filePath = new String(packet.getData(), 0, packet.getLength());
                //System.out.println("<UDPFileReceiver> Filepath: " + filePath);
                File newFile = new File(filePath);
                FileOutputStream fos = new FileOutputStream(newFile);


                // send acknowledgement
                ack = new DatagramPacket(ackBuffer, ackBuffer.length, packet.getAddress(), packet.getPort());
                aSocket.send(ack);

                while (true) {
                    packet = new DatagramPacket(buffer, buffer.length);

                    aSocket.receive(packet);
                    //System.out.println("<UDPFileReceiver> Received " + packet.getLength() + " bytes");

                    // if packet is empty
                    if (Arrays.equals(packet.getData(), emptyBuffer)) {
                        System.out.println("<UDPFileReceiver> Received empty packet");
                        break;
                    }


                    // write data to file
                    fos.write(packet.getData(), 0, packet.getLength());
                    aSocket.send(ack);
                }
                fos.close();

                // calculate SHA256 hash for the new file
                byte hash[] = checksum(filePath);

                //System.out.println("<UDPFileReceiver> Hash: " + Arrays.toString(hash));
                //System.out.println("<UDPFileReceiver> Hash size: " + hash.length);

                packet = new DatagramPacket(hash, hash.length, ack.getAddress(), ack.getPort());
                aSocket.send(packet);

                // receive signal from sender
                ack = new DatagramPacket(ackBuffer, ackBuffer.length);
                aSocket.receive(ack);

                if (ack.getData()[0] == (byte)0xAA) {
                    System.out.println("<UDPFileReceiver> File received");
                    break;
                }
                else {
                    System.out.println("<UDPFileReceiver> File corrupted, retrying");
                }
            } catch (IOException e) {
                System.out.println("<UDPFileReceiver> IO: " + e.getMessage());
                return;
            }
        }
    }
    
    private byte[] createEmptyByteArray(int arraySize) {
        byte array[] = new byte[arraySize];

        for (int i = 0; i < arraySize; i++) {
            array[i] = 0x00;
        }

        return array;
    }

    /* 
    *   Method referenced from StackOverflow.
    */
    private static byte[] checksum(String filepath) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("<UDPFileReceiver> NoSuchAlgorithm: " + e.getMessage());
            return null;
        }
        // file hashing with DigestInputStream  
        try (DigestInputStream dis = new DigestInputStream(new FileInputStream(filepath), md)) {
            while (dis.read() != -1) ; //empty loop to clear the data
            md = dis.getMessageDigest();
        }

        return md.digest();
    }
}
