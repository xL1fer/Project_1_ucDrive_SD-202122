import java.io.*;
import java.net.*;
import java.nio.file.Files;

public class ServerUploadHandler extends Thread{
    private ObjectOutputStream oos;
    private String filePath;
    int port;
    
    public ServerUploadHandler(String filePath, int port){
        this.filePath = filePath;
        this.port = port;
        this.start();
    }

    public void run(){

        try (ServerSocket listenSocket = new ServerSocket(port)) {
            System.out.println("\n:: Download Socket listening on port " + port + " ::");
            System.out.println("DOWNLOAD LISTEN SOCKET=" + listenSocket);
            Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
            System.out.println("CLIENT DOWNLOAD SOCKET (created at accept())="+clientSocket);
            
            oos = new ObjectOutputStream(clientSocket.getOutputStream());
            oos.flush();

            File file = new File(filePath);
            String fileName = file.getName();
            //get byte array with data
            byte fileData[] = Files.readAllBytes(file.toPath());

            oos.writeUTF(fileName);
            oos.writeObject(fileData);
            oos.flush();
            
            System.out.println("CLIENT DOWNLOAD SOCKET CLOSING");
            clientSocket.close();
            listenSocket.close();
        } catch (IOException e) {
            System.out.println("IO:" + e.getMessage());
        }

        //System.out.println("Dl removing unused port");
        UcDrive_Server.ports.remove(Integer.valueOf(port));
        return;

    }

}
