import java.net.*;
import java.io.*;

public class UcDrive_Server{
    private static int serverPort = 6000;

    public static void main(String args[]){
        
        try (ServerSocket listenSocket = new ServerSocket(serverPort)) {
            System.out.println("A escuta no porto 6000");
            System.out.println("LISTEN SOCKET=" + listenSocket);
            while(true) {
                Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
                System.out.println("CLIENT_SOCKET (created at accept())="+clientSocket);
                new ClientHandler(clientSocket);
            }
        } catch(IOException e) {
            System.out.println("Listen:" + e.getMessage());
        }
    }
}