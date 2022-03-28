public class FileTransferType {
    private int opt;
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
