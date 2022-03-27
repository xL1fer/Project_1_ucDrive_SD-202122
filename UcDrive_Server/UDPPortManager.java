import java.io.*;
import java.net.*;

public class UDPPortManager extends Thread{
    private DatagramSocket aSocket;
    private String filePath; // will include filename if its a file 
    private String fileName;
    private String otherServerIp; 
    private int port;
    private boolean isPrimary;
    private int opt;

    private static int maxTimeouts = 5;
    private static int timeout = 2000;

    //this is the primary constructor
    public UDPPortManager(String otherServerIp, int port, String filePath, String fileName, int opt){
        this.otherServerIp = otherServerIp;
        this.port = port;
        this.filePath = filePath;
        this.fileName = fileName;
        this.opt = opt;
        this.isPrimary = true;
        if(!UcDrive_Server.otherServerUp){
            System.out.println("UDPPortManager(Primary) - Cannot replicate file because other server is down.");
            return;
        }

        start();
    }

    //this is the secondary constructor   
    public UDPPortManager(String otherServerIp, int port){
        this.otherServerIp = otherServerIp;
        this.port = port;
        this.isPrimary = false;
        start();
    }

    public void run(){
        int availablePort;
        byte buffer[] = new byte[4];
        DatagramPacket packet;
        DatagramPacket reply;
        ByteArrayInputStream bais;
        DataInputStream dis;
        ByteArrayOutputStream baos;
        DataOutputStream dos;
        String dir;

        if(isPrimary){
            //open socket for communicating

            packet = new DatagramPacket(buffer, buffer.length);
            try{
                this.aSocket = new DatagramSocket();
                this.aSocket.setSoTimeout(timeout);
            } catch (SocketException e) {
                System.out.println("Socket: " + e.getMessage());
                return;
            }

            try{
                switch(opt){
                    //send file to secondary, but first receive port
                    case 1:
                        sendOpt(opt);

                        //receive acknowledgement
                        aSocket.receive(packet);

                        //receive port
                        aSocket.receive(packet);

                        sendAcknowledgement(packet);

                        //byte array to int
                        bais = new ByteArrayInputStream(packet.getData());
                        dis = new DataInputStream(bais);

                        availablePort = dis.readInt();

                        new UDPFileSender(otherServerIp, availablePort, filePath, fileName);

                        break;
                }
                

            } catch(IOException e){
                System.out.println("IO: " + e.getMessage());
                return;
            }
        }
        else{
            //receive port for communication
            packet = new DatagramPacket(buffer, buffer.length);

            int opt;
            try{
                aSocket = new DatagramSocket(port);
                System.out.println("Opened UDP socket for listening.");
                aSocket.setSoTimeout(timeout);
            } catch (SocketException e) {
                System.out.println("Socket: " + e.getMessage());
                return;
            }

            while(true){
                try{
                    aSocket.receive(packet);
                    sendAcknowledgement(packet);

                    bais = new ByteArrayInputStream(packet.getData());
                    dis = new DataInputStream(bais);

                    opt = dis.readInt();

                    //primary server wants to send a file
                    switch(opt){
                        //primary server wants to send a file
                        case 1:
                            availablePort = getAvailablePort();
                            if(availablePort < 0){
                                System.out.println("UDPPortManager (Secondary) - Invalid available port.");
                            }
                            //convert int to byte array
                            baos = new ByteArrayOutputStream();
                            dos = new DataOutputStream(baos);
                            dos.writeInt(availablePort);
                            dos.flush();
                            dos.close();
                            buffer = baos.toByteArray();

                            reply = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());
                            while(true){
                                //send available port
                                aSocket.send(reply);
                                try{
                                    //wait for acknowledgement
                                    aSocket.receive(packet);
                                    break;
                                } catch(IOException e){
                                    System.out.println("UDPPortManager (Secondary) - Opt 1 ACK receive timeout.");
                                }
                            }

                            //create thread to receive the file
                            new UDPFileReceiver(availablePort);

                            break;
                        //primary server wants to create a directory
                        case 2:
                            //receive directory to create
                            aSocket.receive(packet);
                            sendAcknowledgement(packet);
                            
                            //byte array to utf
                            bais = new ByteArrayInputStream(packet.getData());
                            dis = new DataInputStream(bais);

                            dir = dis.readUTF();

                            createDirectory(dir);

                            break;
                        //primary server wants to delete a directory/file
                        case 3:
                            break;
                        default:
                            System.out.println("UDPPortManager (Secondary) - Bad option in switch case.");
                            break;
                    }
                    
                } catch(IOException e){
                    System.out.println("IO: " + e.getMessage());
                    if(interrupted()){
                        System.out.println("UDPPortManager (Secondary) - Thread interrupted.");
                        aSocket.close();
                        return;
                    }     
                }   
            }
        }
    }

    private void createDirectory(String newDir){
        File f = new File(newDir);
        System.out.println("UDPPortManager (Secondary) - Dir: " + f);
        if(f.exists() == false){
            f.mkdirs();
        }
    }
    

    private int getAvailablePort(){
        DatagramSocket bSocket;
        int availablePort;
        //find available port
        try{
            bSocket = new DatagramSocket(0);
            availablePort = bSocket.getLocalPort();
            bSocket.close();
        } catch(SocketException e){
            System.out.println("Socket: " + e.getMessage());
            return -1;
        }
        return availablePort;
    }

    //sends acknowledgement to host that send packet replyTo
    private void sendAcknowledgement(DatagramPacket replyTo){
        byte buffer[];
        
        //send acknowledgement
        buffer = new byte[] {(byte) 0xAA};
        DatagramPacket reply = new DatagramPacket(buffer, buffer.length, replyTo.getAddress(), replyTo.getPort());
        try{
            aSocket.send(reply);
        } catch(IOException e){
            System.out.println("UDPPortManager - " + e.getMessage());
        }
    }

    private void sendOpt(int opt){
        //convert int to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try{
            dos.writeInt(opt);
            dos.flush();
            dos.close();

            byte buffer[] = baos.toByteArray();
            InetAddress aHost = InetAddress.getByName(otherServerIp);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, aHost, port);
            //send opt to secondary server
            aSocket.send(packet);
        } catch(IOException e){
            System.out.println("UDPPortManager (Primary) - " + e.getMessage());
            return;
        }
    }
}