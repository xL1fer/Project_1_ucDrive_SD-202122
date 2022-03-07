import java.net.*;
import java.util.Scanner;
import java.io.*;

public class Client {

	public static void main(String args[]) {
        
        DataInputStream dis;
        DataOutputStream dos;

        Scanner sc = new Scanner(System.in);
        
        System.out.println("Qual o ip do servidor?");
        String serverIp = sc.nextLine();

        System.out.println("Qual o porto do servidor?");
        int serverPort = sc.nextInt();

        try(Socket s = new Socket(serverIp, serverPort)){
            System.out.println("Conectado ao servidor");

            dis = new DataInputStream(s.getInputStream());
            dos = new DataOutputStream(s.getOutputStream());

            while (true) {
                // READ STRING FROM KEYBOARD
                String texto = sc.nextLine();

                // WRITE INTO THE SOCKET
                dos.writeUTF(texto);

                texto = dis.readUTF();

                System.out.println("Resposta: " + texto);
            }

        
        } catch(IOException e){
            System.out.println("IO:" + e.getMessage());
        }

    }
}