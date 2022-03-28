/*
 *  "UcDriveServer.java"
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
import java.util.Arrays;
import java.util.Scanner;
import java.io.*;

// suppress unchecked casts warning
@SuppressWarnings("unchecked")

/**
 * Server class responsible for handling
 * the server application
 */
public class UcDriveServer {
    protected static String myServerIp;                 // server Ip of this server instance
    protected static String otherServerIp;              // server Ip of other server instance
    protected static String myServerPort;               // server port of this server instance
    protected static String otherServerPort;            // server port of other server instance
    protected static int maxFailedHearbeats = 4;        // maximum failed heartbeats with the other server (will be +1 because of the condition used)
    protected static int heartbeatDelay = 1000;         // delay between each heartbeat
    protected static boolean otherServerUp;             // variable to indicate if other server is running
    private static Scanner sc;                          // scanner "global" variable

    protected static int portManager = 9000;            // port for server replication purposes
    protected static ArrayList<User> users;             // users array list (protected - visible by same package)

    // main function
    public static void main(String args[]) {
        clearTerminal();

        System.out.println("              ________         __              ");
        System.out.println(" __ __   ____ \\______ \\_______|__|__  __ ____  ");
        System.out.println("|  |  \\_/ ___\\ |    |  \\_  __ \\  \\  \\/ // __ \\ ");
        System.out.println("|  |  /\\  \\___ |    `   \\  | \\/  |\\   /\\  ___/ ");
        System.out.println("|____/  \\___  >_______  /__|  |__| \\_/  \\___  >");
        System.out.println("            \\/        \\/                    \\/");
        System.out.println("\n ucDrive v0.01\n Server Application\n\n====================\n");

        /*
        *   Start by asking to edit users config file
        */
        sc = new Scanner(System.in);
        users = new ArrayList<>();
        loadUsers();

        System.out.println("> Do you want to manage \"user.data\" file?");
        String opt = sc.nextLine();

        if (opt.equals("yes") || opt.equals("y"))
            manageUserData();

        // save "user.data" file
        saveUsers();

        /*
        *   Get each server information
        */
        System.out.print("> My Server IP [localhost]: ");
        myServerIp = sc.nextLine();
        if (myServerIp.equals("")) myServerIp = "localhost";

        System.out.print("> My Server Port [6000]: ");
        myServerPort = sc.nextLine();
        if (myServerPort.equals("")) myServerPort = "6000";

        // using regex to determine wether or not the serverPort string is an Integer
        if (!myServerPort.matches("-?\\d+")) {
            System.out.println("> Server Port must be an integer.");
            sc.close();
            return;
        }

        System.out.print("> Other Server IP [localhost]: ");
        otherServerIp = sc.nextLine();
        if (otherServerIp.equals("")) otherServerIp = "localhost";

        System.out.print("> Other Server Port [7000]: ");
        otherServerPort = sc.nextLine();
        if (otherServerPort.equals("")) otherServerPort = "7000";

        // using regex to determine wether or not the serverPort string is an Integer
        if (!otherServerPort.matches("-?\\d+")) {
            System.out.println("> Server Port must be an integer.");
            sc.close();
            return;
        }

        /*
        *   Start initializing server with corresponding functionalities (primary or secondary)
        */
        // create UDPPortManager instance for secondary server (will be listening for any file changes)
        UDPPortManager udpPortReceiver = new UDPPortManager(otherServerIp, portManager, false);

        // check if the other server is up
        if (!checkServer(otherServerIp, Integer.parseInt(otherServerPort))) {
            System.out.println("<UcDriveServer> Unexpected error");
            udpPortReceiver.interrupt();
            sc.close();
            return;
        }

        // this is now the primary server, we can close udpPortReceiver
        udpPortReceiver.interrupt();

        // heartbeat UDP socket (opened in the primary server)
        new UDPHeartbeat(myServerIp, Integer.parseInt(myServerPort), heartbeatDelay);
        
        // open socket for clients communication
        try (ServerSocket listenSocket = new ServerSocket(Integer.parseInt(myServerPort))) {
            System.out.println("\n:: Listening on port " + myServerPort + " ::");
            System.out.println("LISTEN SOCKET=" + listenSocket);
            while (true) {
                Socket clientSocket = listenSocket.accept();
                System.out.println("CLIENT_SOCKET (created at accept())="+clientSocket);
                new ClientHandler(clientSocket);
            }
        } catch (IOException e) {
            System.out.println("<UcDriveServer> Listen: " + e.getMessage());
        }

        //TODO: more saves for the users file (:
        //saveUsers();
        sc.close();
    }

    // function to manage users configuration file
    private static void manageUserData() {
        String opt;

        while (true) {
            System.out.println("\nAdd client: \"add [username] [password]\"");
            System.out.println("Remove client: \"remove [username]\"");
            System.out.println("List clients and passwords: \"list\"");
            System.out.println("Leave menu: \"quit\"");
            opt = sc.nextLine();

            String words[] = opt.split(" ");

            switch (words[0]) {
                // quit
                case "quit":
                    return;
                // add user
                case "add":
                    if (words.length < 3) {
                        System.out.println("> Too few arguments.");
                        break;
                    }
                    addUser(words);
                    break;
                // remove user
                case "remove":
                    // TODO: delete user directory aswell and remove user and user directory from secondary server
                    if (words.length < 2) {
                        System.out.println("> Too few arguments.");
                        break;
                    }
                    removeUser(words);
                    break;
                // list users and corresponding passwords
                case "list":
                    for (User u : users ) {
                        u.createDirectory();
                        System.out.println(u.getClientData().toString());
                    }
                    break;
                default:
                    System.out.println("> Invalid format.");
                    break;
            }
        }
    }

    // save users configuration file (syncronized method to prevent threads concurrency)
    private static synchronized void saveUsers() {
        try {
            FileOutputStream fos = new FileOutputStream("storage\\users.data");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(users);

            fos.close();
        } catch (FileNotFoundException e) {
            //System.out.println("FileNotFound:" + e.getMessage());
            System.out.println("<UcDriveServer> FileNotFound: \"user.data\" not found");
        } catch (IOException e){
            System.out.println("<UcDriveServer> IO: " + e.getMessage());
        }
    }

    // load users to memory (syncronized method to prevent threads concurrency)
    private static synchronized void loadUsers() {
        try {
            FileInputStream fis = new FileInputStream("storage\\users.data");
            ObjectInputStream ois = new ObjectInputStream(fis);
            users = (ArrayList<User>) ois.readObject();

            fis.close();
        } catch (FileNotFoundException e) {
            //System.out.println("FileNotFound:" + e.getMessage());
            System.out.println("<UcDriveServer> FileNotFound: \"user.data\" not found");
        } catch (IOException e){
            System.out.println("<UcDriveServer> IO: " + e.getMessage());
        } catch (ClassNotFoundException e){
            System.out.println("<UcDriveServer> ClassNotFound: " + e.getMessage());
        } catch (ClassCastException e){
            System.out.println("<UcDriveServer> ClassCast: " + e.getMessage());
        }
    }

    // function to clear terminal
    private static void clearTerminal(){
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (InterruptedException e) {
            System.out.println("<UcDriveServer> Interrupted: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("<UcDriveServer> IO: " + e.getMessage());
        }
    }

    // add user to "global" array list
    private static void addUser(String words[]) {
        // checks if client with username already exists
        for (User u : users) {
            if (u.getClientData().getUsername().equals(words[1])) {
                System.out.println("> Username already registed.");
                return;
            }
        }

        // client can be added
        ClientAuth newClientAuth = new ClientAuth(words[1], words[2]);
        users.add(new User(newClientAuth));
        System.out.println("> Added user " + words[1]);
    }

    // remove user from "global" array list
    private static void removeUser(String words[]) {
        // checks if client with username exists
        for (User u : users) {
            if (u.getClientData().getUsername().equals(words[1])) {
                users.remove(u);
                System.out.println("> User " + words[1] + " successfully removed.");
                return;
            }
        }

        // client not found
        System.out.println("> User " + words[1] + " not found.");
    }

    // TODO: config variable for heartbet time and timeout
    // function to check while "other server" is running
    private static boolean checkServer(String serverIp, int serverPort) {
        int heartbeat = 0;

        // open socket to try to communicate with primary server
        DatagramSocket aSocket;
        try {
            aSocket = new DatagramSocket();
            aSocket.setSoTimeout(heartbeatDelay);
        } catch(SocketException e) {
            System.out.println("<UcDriveServer> Socket: " + e.getMessage());
            return false;
        }
        
        byte buffer[] = new byte[1];
        buffer[0] = (byte)0xAA;
        
        // send heartbeats to primary server
        while (true) {
            try {
                InetAddress aHost = InetAddress.getByName(serverIp);
                DatagramPacket request = new DatagramPacket(buffer, buffer.length, aHost, serverPort);
                aSocket.send(request);
                
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(reply);

                heartbeat = 0;
                System.out.println("<UcDriveServer> Received heartbeat");
            } catch(SocketTimeoutException e) {
                System.out.println("<UcDriveServer> Heartbeat failed");
                heartbeat++;
                if (heartbeat > maxFailedHearbeats) {
                    System.out.println("<UcDriveServer> Server Ip \"" + serverIp + "\" with port \"" + serverPort + "\" is down");
                    break;
                }

            } catch(IOException e) {
                System.out.println("<UcDriveServer> IO: " + e.getMessage());
                aSocket.close();
                return false;
            }
        }

        aSocket.close();

        return true;
    }

    // replicate all files from the primary server to the secondary server
    protected static void replicateFiles(File file) {
        // start directory
        if (file == null) {
            file = new File("storage");
        }

        for (File fileEntry : file.listFiles()) {
            // if file is a folder
            if (fileEntry.isDirectory()) {
                UDPPortManager.addFileTransfer(2, fileEntry.toString(), "");
                replicateFiles(fileEntry);
            }
            // if file is a file
            else {
                String fileNames[] = fileEntry.toString().split("\\\\");
                String fileName = fileNames[fileNames.length-1];
                String filePath = "";
                for (int i = 0; i < fileNames.length - 1; i++) {
                    filePath += fileNames[i];
                    filePath += "\\";
                }

                UDPPortManager.addFileTransfer(1, filePath, fileName);
            }
        }

    }
    
}
