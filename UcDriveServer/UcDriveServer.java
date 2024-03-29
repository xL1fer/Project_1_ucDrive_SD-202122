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
import java.nio.file.Files;

// suppress unchecked casts warning
@SuppressWarnings("unchecked")

/**
 * Server class responsible for handling the server application.
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

        // check if the other server is up
        if (!checkServer(otherServerIp, Integer.parseInt(otherServerPort))) {
            System.out.println("<UcDriveServer> Unexpected error");
            sc.close();
            return;
        }

        //add hook to catch SIGINT
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                //save users
                System.out.println("\n<UcDriveServer> Server closing...");
                saveUsers();
                sc.close();
            }
        }));

        // heartbeat UDP socket (opened in the primary server)
        new UDPHeartbeat(myServerIp, Integer.parseInt(myServerPort), heartbeatDelay);
        loadUsers();

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

        sc.close();
    }

    /**
     * Manages user configuration.
     */
    private static void manageUserData() {
        String opt;

        while (true) {
            System.out.println("\nAdd client: \"add [username] [password] [department] [phone number] [address] [identification] [id expiraton date]\"");
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
                    if (words.length < 8) {
                        System.out.println("> Too few arguments.");
                        break;
                    }
                    addUser(words);
                    break;
                // remove user
                case "remove":
                    if (words.length < 2) {
                        System.out.println("> Too few arguments.");
                        break;
                    }

                    User delUser = removeUser(words);
                    if(delUser != null){
                        File toDel = new File(delUser.getCurPath());
                        deleteDir(toDel);
                    }
                    break;
                // list users and corresponding passwords
                case "list":
                    for (User u : users ) {
                        u.createDirectory();
                        System.out.println(u.toString());
                    }
                    break;
                default:
                    System.out.println("> Invalid format.");
                    break;
            }
        }
    }

    /**
     * Saves the users array into a file.
     */
    public static synchronized void saveUsers() {
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

            for(User u : users){
                u.setLogged(false);
            }

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

     /**
     * Cleans the terminal. 
     * May only work for Windows.
     */
    private static void clearTerminal(){
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (InterruptedException e) {
            System.out.println("<UcDriveServer> Interrupted: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("<UcDriveServer> IO: " + e.getMessage());
        }
    }

    /**
     * Adds a user to the users list.
     * @param words string array containing information about the new user
     */
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
        users.add(new User(newClientAuth, words[3], words[4], words[5], words[6], words[7]));
        System.out.println("> Added user " + words[1]);
    }

    /**
     * Removes a user from the users list.
     * @param words string array containing information about the user to remove
     * @return the removed user or null if no user was found
     */
    private static User removeUser(String words[]) {
        // checks if client with username exists
        for (User u : users) {
            if (u.getClientData().getUsername().equals(words[1])) {
                users.remove(u);
                System.out.println("> User " + words[1] + " successfully removed.");
                return u;
            }
        }

        // client not found
        System.out.println("> User " + words[1] + " not found.");
        return null;
    }

    /**
     * Checks if the other server is turned on.
     * This will block this server until the other server is unreachable.
     * @param serverIp server IP
     * @param serverPort server port
     * @return true if the other server is turned off, false otherwise
     */
    private static boolean checkServer(String serverIp, int serverPort) {
        int heartbeat = 0;
        otherServerUp = false;
        UDPPortManager udpPortReceiver = null;
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

                //if the other server was down and turned on now
                if(otherServerUp == false){
                    /*
                    *   Start initializing server with corresponding functionalities (primary or secondary)
                    */
                    // create UDPPortManager instance for secondary server (will be listening for any file changes)
                    udpPortReceiver = new UDPPortManager(otherServerIp, portManager, false);
                }
                
                otherServerUp = true;

                heartbeat = 0;
                System.out.println("<UcDriveServer> Received heartbeat");
            } catch(SocketTimeoutException e) {
                System.out.println("<UcDriveServer> Heartbeat failed");
                heartbeat++;
                if (heartbeat > maxFailedHearbeats) {
                    otherServerUp = false;

                    //close the receiver
                    if(udpPortReceiver != null)
                        udpPortReceiver.interrupt();

                    System.out.println("<UcDriveServer> Server Ip \"" + serverIp + "\" with port \"" + serverPort + "\" is down");
                    break;
                }

            } catch(IOException e) {
                System.out.println("<UcDriveServer> IO: " + e.getMessage());

                //close the receiver
                if(udpPortReceiver != null)
                    udpPortReceiver.interrupt();

                aSocket.close();
                return false;
            }
        }

        aSocket.close();

        return true;
    }

     /**
     * Deletes a directory.
     * @param file name of the directory to be deleted
     */
    private static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (!Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        if (!file.delete())
            System.out.println("> Could not delete file \"" + file + "\".");
    }

    /**
     * Replicates all the files from this server to the secondary server.
     * This uses recursion.
     * @param file name of the file to be sent, null if all
     */
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
                    if(i != fileNames.length - 2)
                        filePath += "\\";
                }
                //add file to the transfer list
                UDPPortManager.addFileTransfer(1, filePath, fileName);
            }
        }

    }
    
}
