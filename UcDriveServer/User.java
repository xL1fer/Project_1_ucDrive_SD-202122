/*
 *  "User.java"
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

 /**
 * User server-sided class
 */
import java.io.File;
import java.io.Serializable;

public class User implements Serializable {
    private ClientAuth clientData;
    private String curPath;
    private boolean isLogged;
    private String department;          // client department
    private String phoneNumber;         // client phone
    private String address;             // client address
    private String identification;      // client identification
    private String idExpiratonDate;     // client identification expiration date

    /**
     * Creates a new User with the given information.
     * @param auth user's authentication
     * @param department user's department
     * @param phoneNumber user's phone number
     * @param address user's address
     * @param identification user's identification
     * @param idExpiratonDate user's ID expirationDate
     */
    public User(ClientAuth auth, String department, String phoneNumber, String address, String identification, String idExpiratonDate) {
        clientData = auth;
        curPath = "storage\\Users\\" + clientData.getUsername();
        isLogged = false;
        this.department = department;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.identification = identification;
        this.idExpiratonDate = idExpiratonDate;
        createDirectory();
    }

    /**
     * Compares the given authentication to this.
     * @param auth authentication to be compared
     * @return
     */
    public boolean compareAuth(ClientAuth auth) {
        if (clientData.getUsername().equals(auth.getUsername()) && clientData.getPassword().equals(auth.getPassword()))
            return true;
        return false;
    }

    /**
     * Creates this user's directory.
     */
    public void createDirectory() {
        File f = new File("storage\\Users\\"+ clientData.getUsername());
        if (f.exists() == false) {
            f.mkdirs();
        }
    }

    public ClientAuth getClientData() {
        return clientData;
    }

    public String getCurPath() {
        return curPath;
    }

    public void setCurPath(String newPath) {
        this.curPath = newPath;
    }

    public String getClientPath() {
        return curPath.substring(14);
    }


    public String getIdExpiratonDate() {
        return idExpiratonDate;
    }

    public void setIdExpiratonDate(String idExpiratonDate) {
        this.idExpiratonDate = idExpiratonDate;
    }

    public String getIdentification() {
        return identification;
    }

    public void setIdentification(String identification) {
        this.identification = identification;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public boolean getLogged(){
        return isLogged;
    }
    
    public void setLogged(boolean status){
        isLogged = status;
    }

    public String toString() {
        return "User: " + clientData.getUsername() + ", Password: " + clientData.getPassword() + ", Department: " + this.department + ", Phone Number: " + this.phoneNumber + ", Address: " + this.address + ", Identification: " + this.identification + ", Id Expiraton Date: " + this.idExpiratonDate;
    }
}
