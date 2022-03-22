import java.io.*;
import java.net.*;

public class UDPHeartbeat extends Thread{
    private String serverIp;
    private int port;
    private DatagramSocket aSocket;
    private int heartbeatDelay;

    public UDPHeartbeat(String serverIp, int port, int heartbeatDelay){
        this.serverIp = serverIp;
        this.port = port;
        this.heartbeatDelay = heartbeatDelay - 100;
        try{
			this.aSocket = new DatagramSocket(port);
            this.aSocket.setSoTimeout(heartbeatDelay);
			this.start();
		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		}
    }

    public void run(){
        System.out.println("\n:: UDP Socket listening on port " + port + " ::");
        while(true){
            try{
                byte buffer[] = new byte[1];
                buffer[0] = (byte)0xAA;	
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(request);
                UcDrive_Server.otherServerUp = true;

                DatagramPacket reply = new DatagramPacket(buffer, 
                buffer.length, request.getAddress(), request.getPort());
                aSocket.send(reply);
                Thread.sleep(heartbeatDelay);
            } catch(IOException e){
                //System.out.println("UDPHeartbeat - IOException: " + e.getMessage());
                UcDrive_Server.otherServerUp = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
