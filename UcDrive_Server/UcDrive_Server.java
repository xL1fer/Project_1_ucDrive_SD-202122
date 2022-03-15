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
import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;

// suppress unchecked casts warning
@SuppressWarnings("unchecked")

/**
 * Server class responsible for handling
 * the server application
 */
public class UcDrive_Server {
    private static int serverPort = 6000;
    private static boolean isPrimary = false;
    private static String secServerIp = "localhost";
    private static int secServerPort = 7000;

    protected static ArrayList<User> users;     // protected - visible by same package
    //private static int fileTransferPort = 7000;   // might not be used again
    //public static ArrayList<Integer> ports;       // might not be used again

    public static void main(String args[]) {
        clearTerminal();

        System.out.println("              ________         __              ");
        System.out.println(" __ __   ____ \\______ \\_______|__|__  __ ____  ");
        System.out.println("|  |  \\_/ ___\\ |    |  \\_  __ \\  \\  \\/ // __ \\ ");
        System.out.println("|  |  /\\  \\___ |    `   \\  | \\/  |\\   /\\  ___/ ");
        System.out.println("|____/  \\___  >_______  /__|  |__| \\_/  \\___  >");
        System.out.println("            \\/        \\/                    \\/");
        System.out.println("\n ucDrive v0.01\n Server Application\n\n====================\n");

        Scanner sc = new Scanner(System.in);
        //ports = new ArrayList<>();

        users = new ArrayList<>();

        loadUsers();

        System.out.println("> Do you want to manage \"user.data\" file?");
        String opt = sc.nextLine();

        // TODO: make a thread responsible to handling the user data while the other is already running the server
        if(opt.equals("yes") || opt.equals("y"))
            manageUserData();

        saveUsers();

        while(!isPrimary)
            checkPrimaryServer();

        new UDPServer("localhost", serverPort);

        
        try (ServerSocket listenSocket = new ServerSocket(serverPort)) {
            System.out.println("\n:: Listening on port 6000 ::");
            System.out.println("LISTEN SOCKET=" + listenSocket);
            while (true) {
                Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
                System.out.println("CLIENT_SOCKET (created at accept())="+clientSocket);
                new ClientHandler(clientSocket);
            }
        } catch (IOException e) {
            System.out.println("Listen:" + e.getMessage());
        }

        //saveUsers();
        sc.close();
    }

    private static void manageUserData() {
        Scanner sc = new Scanner(System.in);
        String opt;
        //outer:
        while(true){
            System.out.println("\nTo add clients type: \"add [username] [password]\"");
            System.out.println("To remove clients type: \"remove [username]\"");
            System.out.println("To list clients and passwords: \"list\"");
            System.out.println("To leave this menu, type: \"quit\"");
            opt = sc.nextLine();

            String words[] = opt.split(" ");

            switch(words[0]){
                // quit
                case "quit":
                    sc.close();
                    return;
                // add user
                case "add":
                    if (words.length < 3) {
                        System.out.println("Too few arguments.");
                        break;
                    }
                    addUser(words);
                    break;
                // remove user
                case "remove":
                    // TODO: delete user directory aswell
                    if (words.length < 2) {
                        System.out.println("Too few arguments.");
                        break;
                    }
                    removeUser(words);
                    break;
                // list users and corresponding passwords
                case "list":
                    for (User u : users ){
                        u.createDirectory();
                        System.out.println(u.getClientData().toString());
                    }
                    break;
                default:
                    System.out.println("Invalid format.");
                    break;
            }
        }
    }

    private static void saveUsers() {
        try {
            FileOutputStream fos = new FileOutputStream("users.data");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(users);

            fos.close();
        } catch (FileNotFoundException e) {
            System.out.println("FileNotFound:" + e.getMessage());
        } catch (IOException e){
            System.out.println("IOException:" + e.getMessage());
        }
    }

    private static void loadUsers() {
        try {
            FileInputStream fis = new FileInputStream("users.data");
            ObjectInputStream ois = new ObjectInputStream(fis);
            users = (ArrayList<User>) ois.readObject();

            fis.close();
        } catch (FileNotFoundException e) {
            System.out.println("FileNotFound:" + e.getMessage());
        } catch (IOException e){
            System.out.println("IOException:" + e.getMessage());
        } catch (ClassNotFoundException e){
            System.out.println("ClassNotFound:" + e.getMessage());
        } catch (ClassCastException e){
            System.out.println("ClassCast:" + e.getMessage());
        }
    }

    private static void clearTerminal(){
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (InterruptedException e) {
            System.out.println("InterruptedException:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException:" + e.getMessage());
        }
    }

    private static void addUser(String words[]) {
        //checks if client with username already exists
        for(User u : users){
            if(u.getClientData().getUsername().equals(words[1])){
                System.out.println("Username already registed.");
                return;
            }
        }

        //client can be added
        ClientAuth newClientAuth = new ClientAuth(words[1], words[2]);
        users.add(new User(newClientAuth));
        System.out.println("Added user " + words[1]);
    }

    private static void removeUser(String words[]) {
        //checks if client with username exists
        for(User u : users){
            if(u.getClientData().getUsername().equals(words[1])){
                users.remove(u);
                System.out.println("User " + words[1] + " successfully removed.");
                return;
            }
        }

        //client not found
        System.out.println("User " + words[1] + " not found.");
    }

    private static void checkPrimaryServer(){

        int heartbeat = 0;

        DatagramSocket aSocket;
        try{
            aSocket = new DatagramSocket();   
            aSocket.setSoTimeout(1000);
        } catch(SocketException e){
            System.out.println("Socket: " + e.getMessage());
            return;
        }
        
        byte buffer[] = new byte[1];
        buffer[0] = (byte)0xAA;
        
        while(true){
            try{
                InetAddress aHost = InetAddress.getByName(secServerIp);
                DatagramPacket request = new DatagramPacket(buffer,buffer.length,aHost,serverPort);
                aSocket.send(request);
                
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length);	
                aSocket.receive(reply);
                System.out.println("Received heartbeat.");
                heartbeat = 0;
            } catch(SocketTimeoutException e){
                System.out.println("Heartbeat failed.");
                heartbeat++;
                if(heartbeat > 4){
                    isPrimary = true;
                    System.out.println("This is now the primary server.");
                    break;
                }

            } catch(IOException e){
                System.out.println("IOException: " + e.getMessage());
            }
        }

        aSocket.close();
    }
    
}
