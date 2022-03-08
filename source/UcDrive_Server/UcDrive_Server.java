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

package source.UcDrive_Server;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;
import source.ClientAuth;

/**
 * Server class responsible for handling
 * the server application
 */
public class UcDrive_Server {
    private static int serverPort = 6000;
    public static ArrayList<User> users;
    public static void main(String args[]) {
        clearTerminal();

        Scanner sc = new Scanner(System.in);

        users = new ArrayList<>();

        loadUsers();

        System.out.println("> Do you want to add more users?");
        String opt = sc.nextLine();

        if(opt.equals("yes") || opt.equals("y"))
            addUsers();

        saveUsers();
        //print users
        for(User u : users){
            u.createDirectory();
            System.out.println(u.getClientData().toString());
        }
        
        
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


        //saveUsers();
        sc.close();
    }

    private static void addUsers() {
        Scanner sc = new Scanner(System.in);
        String opt;
        outer:
        while(true){
            System.out.println("To add clients type: \"add [username] [password]\" ");
            System.out.println("To leave this menu, type: \"quit\"");
            opt = sc.nextLine();
            if(opt.equals("quit"))
                break;

            String words[] = opt.split(" ");

            //splits input
            if(!words[0].equals("add")){
                System.out.println("Invalid format.");
                continue;
            }

            //checks if client with username already exists
            for(User u : users){
                if(u.getClientData().getUsername().equals(words[1])){
                    System.out.println("Username already registed.");
                    continue outer;
                }
            }

            //client can be added
            ClientAuth newClientAuth = new ClientAuth(words[1], words[2]);
            users.add(new User(newClientAuth));
            System.out.println("Added user " + words[1]);
        }

        sc.close();
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
}
