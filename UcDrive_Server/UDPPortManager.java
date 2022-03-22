import java.io.*;
import java.net.*;

public class UDPPortManager extends Thread{
    private DatagramSocket aSocket;
    private String filePath;
    private String fileName;
    private String otherServerIp; 
    private int port;
    private boolean isPrimary;

    private static int maxTimeouts = 5;
    private static int timeout = 2000;


    //If this is the primary PortManager, this will receive port numbers
    public UDPPortManager(String otherServerIp, int port){
        this.otherServerIp = otherServerIp;
        this.port = port;
        this.isPrimary = false;
        start();
    }

    public UDPPortManager(String otherServerIp, int port, String filePath, String fileName){
        this.otherServerIp = otherServerIp;
        this.port = port;
        this.filePath = filePath;
        this.fileName = fileName;
        this.isPrimary = true;
        if(!UcDrive_Server.otherServerUp){
            System.out.println("UDPPortManager(Primary) - Cannot replicate file because other server is down.");
            return;
        }

        start();
    }

    public void run(){
        int availablePort;
        int timeoutCounter = 0;
        if(isPrimary){
            //open socket for communicating
            try{
                System.out.println("here1");
                this.aSocket = new DatagramSocket();
                System.out.println("here2");
                this.aSocket.setSoTimeout(timeout);
            } catch (SocketException e) {
                System.out.println("Socket: " + e.getMessage());
                return;
            }

            DatagramSocket bSocket;
            //find available port
            try{
                bSocket = new DatagramSocket(0);
                availablePort = bSocket.getLocalPort();
                bSocket.close();
            } catch(SocketException e){
                System.out.println("Socket: " + e.getMessage());
                return;
            }
            try{
                System.out.println("Got port " + availablePort + " from OS");
                byte buffer[];

                //convert int to byte array
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeInt(availablePort);
                dos.flush();
                dos.close();
                buffer = baos.toByteArray();
                System.out.println("Length do buffer!! deve ser 4 bytes");
                System.out.println(buffer.length);
                
                InetAddress aHost = InetAddress.getByName(otherServerIp);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, aHost, port);
                
                //loop sending port until this thread receives ACK
                while(true){
                    aSocket.send(packet);
                    System.out.println("Sent port.");

                    DatagramPacket ack = new DatagramPacket(buffer, buffer.length);
                    //try to receive acknowledgement
                    try{
                        aSocket.receive(ack);
                        timeoutCounter = 0;
                    } catch(IOException e){
                        System.out.println("UDPPortManager(Primary) - IO: " + e.getMessage());
                        System.out.println("UDPPortManager(Primary) - Receive timeout.");

                        timeoutCounter++;
                        //if the receive operation times out too much times end this thread
                        if(timeoutCounter > maxTimeouts)
                            return;
                        continue;
                    }
                    System.out.println("Received acknowledgment");
                    break;
                }

                aSocket.close();
                //SEND FILE
                System.out.println("File path: " + filePath);
                System.out.println("File name: " + fileName);
                new UDPFileSender(otherServerIp, availablePort, filePath, fileName);

            } catch(IOException e){
                System.out.println("IO: " + e.getMessage());
                return;
            }
        }
        else{
            //receive port for communication
            byte buffer[] = new byte[4];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

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

                    ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
                    DataInputStream dis = new DataInputStream(bais);

                    availablePort = dis.readInt();

                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());
                    aSocket.send(reply);

                    System.out.println("UDPPortManager - Available port for receiving files is " + availablePort);
                    
                    //RECEIVE FILE
                    new UDPFileReceiver(availablePort);
                    
                } catch(IOException e){
                    System.out.println("IO: " + e.getMessage());
                    if(interrupted()){
                        System.out.println("UDPPortManager- Thread interrupted.");
                        aSocket.close();
                        return;
                    }
                        
                }   
            }
        }
    }
}