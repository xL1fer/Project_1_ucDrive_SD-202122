/*
 *  "FileTransferType.java"
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
 * File Transfer class to help handle
 * server replications
 */
public class FileTransferType {
    private int opt;            // indicates if we need to: 1 - create a file; 2 - make a directory; 3 - delete a directory
    private String filePath;
    private String fileName;

    public FileTransferType(int opt, String filePath, String fileName) {
        this.setOpt(opt);
        this.setFilePath(filePath);
        this.setFileName(fileName);
    }

    public int getOpt() {
        return opt;
    }

    public void setOpt(int opt) {
        this.opt = opt;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
