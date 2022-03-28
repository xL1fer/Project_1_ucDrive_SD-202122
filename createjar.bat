cd UcDriveServer
jar cmf MANIFEST.MF UcDriveServer.jar UcDriveServer.java UcDriveServer.class ClientHandler.java ClientHandler.class User.java User.class ClientAuth.java ClientAuth.class ServerUploadHandler.java ServerUploadHandler.class ServerDownloadHandler.java ServerDownloadHandler.class UDPHeartbeat.java UDPHeartbeat.class UDPPortManager.java UDPPortManager.class UDPFileSender.java UDPFileSender.class UDPFileReceiver.java UDPFileReceiver.class FileTransferType.java FileTransferType.class
cd ..
cd Client
jar cmf MANIFEST.MF Client.jar Client.java Client.class ClientAuth.java ClientAuth.class ClientDownloadHandler.java ClientDownloadHandler.class ClientUploadHandler.java ClientUploadHandler.class
cd ..

pause