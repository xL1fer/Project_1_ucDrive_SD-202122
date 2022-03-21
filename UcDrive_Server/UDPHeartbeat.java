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
			this.start();
		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		}
    }

    public void run(){
        System.out.println("\n:: UDP Socket listening on port " + port + " ::");
        try{
            while(true){
                byte buffer[] = new byte[1];
                buffer[0] = (byte)0xAA;	
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(request);

                DatagramPacket reply = new DatagramPacket(buffer, 
                buffer.length, request.getAddress(), request.getPort());
                aSocket.send(reply);
                Thread.sleep(heartbeatDelay);
            }
        } catch(IOException e){
            System.out.println("IOException: " + e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
