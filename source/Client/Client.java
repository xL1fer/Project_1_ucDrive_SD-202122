/*
 *  "Client.java"
 * 
 *  ====================================
 *
 *  Universidade de Coimbra
 *  Faculdade de Ciências e Tecnologia
 *  Departamento de Engenharia Informatica
 * 
 *  Alexandre Gameiro Leopoldo - 2019219929
 *  Luís Miguel Gomes Batista  - 2019214869
 * 
 *  ====================================
 * 
 *  "ucDrive Project"
 */

import java.net.*;
import java.util.Scanner;
import java.io.*;

/**
 * Client class responsible for handling
 * the client application
 */
public class Client {

	public static void main(String args[]) {
        DataInputStream dis;
        DataOutputStream dos;

        Scanner sc = new Scanner(System.in);
        
        System.out.println("> Server IP [localhost]:");
        String serverIp = sc.nextLine();
        if (serverIp == "") serverIp = "localhost";

        System.out.println("> Server Port [6000]:");
        String serverPort = sc.nextLine();
        if (serverPort == "") serverPort = "6000";

        try (Socket s = new Socket(serverIp, Integer.parseInt(serverPort))) {
            System.out.println(":: Successfully connected to server ::");

            dis = new DataInputStream(s.getInputStream());
            dos = new DataOutputStream(s.getOutputStream());

            // authentication loop
            while (true) {
                // READ STRING FROM KEYBOARD
                String texto = sc.nextLine();

                // WRITE INTO THE SOCKET
                dos.writeUTF(texto);

                texto = dis.readUTF();

                System.out.println("Answer: " + texto);
            }

            // commands loop
            //while (true) {
                
            //}
        
        } catch (IOException e) {
            System.out.println("IO:" + e.getMessage());
        }

    }
}