import java.net.*;
import java.io.*;

public class ClientHandler extends Thread{
    private Socket clientSocket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private boolean isAuthenticated;

    public ClientHandler(Socket clientSocket){
        this.clientSocket = clientSocket;

        try {
            this.dis = new DataInputStream(clientSocket.getInputStream());
            this.dos = new DataOutputStream(clientSocket.getOutputStream());
            this.start();
        } catch (IOException e) {
            System.out.println("Connection:" + e.getMessage());
            
        }
    }

    public void run(){
        String answer;
        try {
            while(true){
                //an echo server
                String data = dis.readUTF();
                System.out.println("ClientHandler received " + data);
                answer = data.toUpperCase();
                dos.writeUTF(answer);
            }
        } catch(EOFException e) {
            System.out.println("EOF:" + e);
        } catch(IOException e) {
            System.out.println("IO:" + e);
        }

    }


}