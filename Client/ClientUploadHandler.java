import java.io.*;
import java.net.*;
import java.nio.file.Files;

public class ClientUploadHandler extends Thread{
    private String serverIp;
    private int serverPort;
    private DataOutputStream dos;
    private String localPath;
    private int bufSize;
    private byte buffer[];

    public ClientUploadHandler(String serverIp, int serverPort, String localPath){
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.localPath = localPath;
        this.bufSize = 8192; //8KB
        this.buffer = new byte[bufSize];
        this.start();
    }

    public void run(){
        try (Socket s = new Socket(serverIp, serverPort)) {
            dos = new DataOutputStream(s.getOutputStream());
            dos.flush();

            File file = new File(localPath);
            FileInputStream fis = new FileInputStream(file);
            String fileName = file.getName();
            
            System.out.println("\n> Uploading file to server...");
            if (Client.onServerDirectory)
                System.out.print("(Server) " + Client.serverDirectory + ">");
            else
                System.out.print("(Local) " + Client.localDirectory + ">");

            dos.writeUTF(fileName);
            
            int n;
            while((n = fis.read(buffer)) > 0){
                //System.out.println("Sent " + n + "B.");
                dos.write(buffer, 0, n);
            }
            dos.flush();

            dos.close();
            fis.close();
            s.close();
        } catch (IOException e) {
			System.out.println("Listen:" + e.getMessage());
		}

        System.out.println("\n> Upload finished...");
        if(Client.onServerDirectory)
            System.out.print("(Server) " + Client.serverDirectory + ">");
        else
            System.out.print("(Local) " + Client.localDirectory + ">");

        return;
    }


}
