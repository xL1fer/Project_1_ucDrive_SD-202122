/*
 *  "UcDrive_Server.java"
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
 * Server class responsible for handling
 * the server application
 */
public class UcDrive_Server {
    private static int serverPort = 6000;

    public static void main(String args[]) {
        
        try (ServerSocket listenSocket = new ServerSocket(serverPort)) {
            System.out.println(":: Listening on port 6000 ::");
            System.out.println("LISTEN SOCKET=" + listenSocket);
            while (true) {
                Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
                System.out.println("CLIENT_SOCKET (created at accept())="+clientSocket);
                new ClientHandler(clientSocket);
            }
        } catch (IOException e) {
            System.out.println("Listen:" + e.getMessage());
        }
    }
}