import java.io.*;
import java.net.*;

public class ClientDownloadHandler extends Thread{
    private String serverIp;
    private int serverPort;
    private DataInputStream dis;
    private String localPath;
    private int bufSize;
    private byte buffer[];

    public ClientDownloadHandler(String serverIp, int serverPort, String localPath){
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.localPath = localPath;
        this.bufSize = 8192;
        this.buffer = new byte[bufSize];
        this.start();
    }

    public void run(){
        try (Socket s = new Socket(serverIp, serverPort)) {
            dis = new DataInputStream(s.getInputStream());
            
            System.out.println("\n> Downloading file from server...");
            if (Client.onServerDirectory)
                System.out.print("(Server) " + Client.serverDirectory + ">");
            else
                System.out.print("(Local) " + Client.localDirectory + ">");

            String fileName = dis.readUTF();

            File file = new File(localPath + "\\" + fileName);
            FileOutputStream fos = new FileOutputStream(file);

            int n;
            while((n = dis.read(buffer)) > 0){
                //System.out.println("Read " + n + "B.");
                fos.write(buffer, 0, n);
            }

            //System.out.println("\n> Download finished.");
            fos.close();
            dis.close();
            s.close();

        } catch (IOException e) {
			System.out.println("Listen:" + e.getMessage());
		}

        System.out.println("\n> Download finished...");
        if(Client.onServerDirectory)
            System.out.print("(Server) " + Client.serverDirectory + ">");
        else
            System.out.print("(Local) " + Client.localDirectory + ">");

        return;
    }


}
