import java.io.*;
import java.net.*;

public class UDPPortManager extends Thread{
    private DatagramSocket aSocket;
    private String otherServerIp; 
    private int port;
    private boolean isPrimary;
    private int timeout;

    //If this is the primary PortManager, this will receive port numbers
    public UDPPortManager(String otherServerIp, int port, boolean isPrimary){
        this.otherServerIp = otherServerIp;
        this.port = port;
        this.isPrimary = isPrimary;
        start();
    }

    public void run(){
        if(isPrimary){




        }
        else{
            //open socket for communicating
            int availablePort;
            try{
                aSocket = new DatagramSocket(port);
                aSocket.setSoTimeout(timeout);
            } catch (SocketException e) {
                System.out.println("Socket: " + e.getMessage());
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
                byte buffer[];

                ByteArrayOutputStream baot = ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baot);
                dos.writeInt(availablePort);
                dos.flush();
                dos.close();

                buffer = baot.toByteArray();
                System.out.println("Length do buffer!! deve ser 4 bytes");
                System.out.println(buffer.length);

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                aSocket.send(packet);
            } catch(IOException e){
                System.out.println("IO: " + e.getMessage());
            }

        }


    }

    private ByteArrayOutputStream ByteArrayOutputStream() {
        return null;
    }
}