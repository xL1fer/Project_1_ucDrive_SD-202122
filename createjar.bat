cd UcDrive_Server
jar cmf MANIFEST.MF UcDrive_Server.jar UcDrive_Server.java UcDrive_Server.class ClientHandler.java ClientHandler.class User.java User.class ClientAuth.java ClientAuth.class ServerUploadHandler.java ServerUploadHandler.class ServerDownloadHandler.java ServerDownloadHandler.class UDPHeartbeat.java UDPHeartbeat.class UDPPortManager.java UDPPortManager.class UDPFileSender.java UDPFileSender.class UDPFileReceiver.java UDPFileReceiver.class
cd ..
cd Client
jar cmf MANIFEST.MF Client.jar Client.java Client.class ClientAuth.java ClientAuth.class ClientDownloadHandler.java ClientDownloadHandler.class ClientUploadHandler.java ClientUploadHandler.class
cd ..

pause