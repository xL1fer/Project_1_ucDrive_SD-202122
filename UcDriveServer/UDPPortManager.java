/*
 *  "UDPPortManager.java"
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
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * Class to open ports for file replication
 * (fail over)
 */
public class UDPPortManager extends Thread {
    private DatagramSocket aSocket;
    private String otherServerIp; 
    private int port;
    private boolean isPrimary;
    protected static ArrayList<FileTransferType> filesToTransfer = new ArrayList<>();
    private static int maxTimeouts = 5;
    private static int timeout = 2000;

    public UDPPortManager(String otherServerIp, int port, boolean isPrimary) {
        this.otherServerIp = otherServerIp;
        this.port = port;
        this.isPrimary = isPrimary;
        if (this.isPrimary && !UcDriveServer.otherServerUp) {
            System.out.println("<UDPPortManager> (Primary): Cannot replicate file because secondary server is down");
            return;
        }

        start();
    }

    public void run() {
        int availablePort;
        byte buffer[] = new byte[4];
        DatagramPacket packet;
        DatagramPacket reply;
        ByteArrayInputStream bais;
        DataInputStream dis;
        ByteArrayOutputStream baos;
        DataOutputStream dos;
        String dir;

        if (isPrimary) {
            emptyQueue();
        }
        else {
            // receive port for communication
            packet = new DatagramPacket(buffer, buffer.length);

            byte pathBuffer[] = new byte[100];
            DatagramPacket pathPacket = new DatagramPacket(pathBuffer, pathBuffer.length);
            int opt;
            try {
                aSocket = new DatagramSocket(port);
                System.out.println("<UDPPortManager> (Secondary): Opened UDP socket for listening.");
                aSocket.setSoTimeout(timeout);
            } catch (SocketException e) {
                System.out.println("<UDPPortManager> (Secondary) Socket: " + e.getMessage());
                return;
            }

            while (true) {
                try {
                    aSocket.receive(packet);
                    sendAcknowledgement(packet);

                    bais = new ByteArrayInputStream(packet.getData());
                    dis = new DataInputStream(bais);

                    opt = dis.readInt();

                    // primary server wants to send a file
                    switch (opt) {
                        // primary server wants to send a file
                        case 1:
                            availablePort = getAvailablePort();
                            //System.out.println("<UDPPortManager> (Secondary) Available Port: " + availablePort);
                            if (availablePort < 0) {
                                System.out.println("<UDPPortManager> (Secondary): Invalid available port");
                            }
                            // convert int to byte array
                            baos = new ByteArrayOutputStream();
                            dos = new DataOutputStream(baos);
                            dos.writeInt(availablePort);
                            dos.flush();
                            dos.close();
                            buffer = baos.toByteArray();

                            reply = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());
                            while (true) {
                                // send available port
                                aSocket.send(reply);
                                try {
                                    // wait for acknowledgement
                                    aSocket.receive(packet);
                                    break;
                                } catch(IOException e) {
                                    System.out.println("<UDPPortManager> (Secondary): Opt 1 ACK receive timeout.");
                                }
                            }

                            // create thread to receive the file
                            new UDPFileReceiver(availablePort);
                            System.out.println("<UDPPortManager> (Secondary): Received file");

                            break;
                        // primary server wants to create a directory
                        case 2:
                            // receive directory to create
                            aSocket.receive(pathPacket);
                            sendAcknowledgement(packet);

                            dir = new String(pathPacket.getData(), 0, pathPacket.getLength());

                            createDirectory(dir);
                            System.out.println("<UDPPortManager> (Secondary): Created directory");

                            break;
                        // primary server wants to delete a directory/file
                        case 3:
                            // receive directory to delete
                            aSocket.receive(pathPacket);
                            sendAcknowledgement(packet);

                            dir = new String(pathPacket.getData(), 0, pathPacket.getLength());

                            File file = new File(dir);
                            deleteDir(file);
                            System.out.println("<UDPPortManager> (Secondary): Deleted directory");
                            
                            break;
                        default:
                            System.out.println("<UDPPortManager> (Secondary): Bad option in switch case");
                            break;
                    }
                    
                } catch(IOException e) {
                    //System.out.println("IO: " + e.getMessage());
                    if (interrupted()) {
                        System.out.println("<UDPPortManager> (Secondary): Thread interrupted");
                        aSocket.close();
                        return;
                    }     
                }   
            }
        }
    }

    synchronized private void emptyQueue() {
        int availablePort;
        byte buffer[] = new byte[4];
        DatagramPacket packet;
        ByteArrayInputStream bais;
        DataInputStream dis;

        //open socket for communicating
        packet = new DatagramPacket(buffer, buffer.length);
        try {
            this.aSocket = new DatagramSocket();
            this.aSocket.setSoTimeout(timeout);
        } catch (SocketException e) {
            System.out.println("<UDPPortManager> (Primary) Socket: " + e.getMessage());
            return;
        }

        byte pathBuffer[];
        DatagramPacket path;

        while (filesToTransfer.size() > 0) {
            FileTransferType file =  filesToTransfer.get(0);
            filesToTransfer.remove(0);

            System.out.println("<UDPPortManager> (Primary) Sending file with option: " + file.getOpt() + " with path: " + file.getFilePath() + " and name: " + file.getFileName() + ".");
            try {
                switch (file.getOpt()) {
                    // send file to secondary, but first receive port
                    case 1:
                        sendOpt(file.getOpt());

                        // receive acknowledgement
                        aSocket.receive(packet);

                        // receive port
                        aSocket.receive(packet);

                        sendAcknowledgement(packet);

                        // byte array to int
                        bais = new ByteArrayInputStream(packet.getData());
                        dis = new DataInputStream(bais);

                        availablePort = dis.readInt();

                        //System.out.println("<UDPPortManager> (Primary) Available Port: " + availablePort);

                        new UDPFileSender(otherServerIp, availablePort, file.getFilePath(), file.getFileName());

                        break;
                    // option 2 to create directory, option 3 to delete directory
                    case 2:
                    case 3:
                        sendOpt(file.getOpt());

                        // receive acknowledgement
                        aSocket.receive(packet);

                        pathBuffer = file.getFilePath().getBytes();
                        path = new DatagramPacket(pathBuffer, pathBuffer.length, packet.getAddress(), packet.getPort());
                        aSocket.send(path);

                        // receive acknowledgement
                        aSocket.receive(packet);
                        break;

                    default:
                        System.out.println("<UDPPortManager> (Primary): Bad option in switch case");
                        break;
                }

            } catch(IOException e) {
                System.out.println("<UDPPortManager> (Primary) IO: " + e.getMessage());
                return;
            }
            
        }

    }

    private void createDirectory(String newDir) {
        //System.out.println("DirName: " + newDir);
        File f = new File(newDir);

        //System.out.println("<UDPPortManager> (Primary): Dir: " + f);
        if (f.exists() == false) {
            System.out.println("<UDPPortManager> (Secondary): Directory created");
            f.mkdirs();
        }
    }
    
    private void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (! Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        if (!file.delete())
            System.out.println("<UDPPortManager> (Secondary): Could not delete file \"" + file + "\".");
    }

    private int getAvailablePort() {
        DatagramSocket bSocket;
        int availablePort;
        //find available port
        try {
            bSocket = new DatagramSocket(0);
            availablePort = bSocket.getLocalPort();
            bSocket.close();
        } catch(SocketException e) {
            System.out.println("<UDPPortManager> Socket: " + e.getMessage());
            return -1;
        }
        return availablePort;
    }

    //sends acknowledgement to host that send packet replyTo
    private void sendAcknowledgement(DatagramPacket replyTo) {
        byte buffer[];
        
        //send acknowledgement
        buffer = new byte[] {(byte) 0xAA};
        DatagramPacket reply = new DatagramPacket(buffer, buffer.length, replyTo.getAddress(), replyTo.getPort());
        try {
            aSocket.send(reply);
        } catch(IOException e) {
            System.out.println("<UDPPortManager> IO: " + e.getMessage());
        }
    }

    private void sendOpt(int opt) {
        //convert int to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeInt(opt);
            dos.flush();
            dos.close();

            byte buffer[] = baos.toByteArray();
            InetAddress aHost = InetAddress.getByName(otherServerIp);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, aHost, port);
            //send opt to secondary server
            aSocket.send(packet);
        } catch(IOException e) {
            System.out.println("<UDPPortManager> (Primary) IO: " + e.getMessage());
            return;
        }
    }

    synchronized public static void addFileTransfer(int opt, String filePath, String fileName) {
        filesToTransfer.add(new FileTransferType(opt, filePath, fileName));
    }
}