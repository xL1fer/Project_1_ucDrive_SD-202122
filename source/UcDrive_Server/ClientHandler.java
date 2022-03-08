/*
 *  "ClientHandler.java"
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
import java.io.*;

/**
 * Client handler server-sided class
 */
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

            // authentication loop
            while (true) {

            }

            // commands loop
            while (true) {
                //an echo server
                String data = dis.readUTF();
                System.out.println("ClientHandler received " + data);
                answer = data.toUpperCase();
                dos.writeUTF(answer);
            }
        } catch (EOFException e) {
            System.out.println("EOF:" + e);
        } catch (IOException e) {
            System.out.println("IO:" + e);
        }

    }


}
