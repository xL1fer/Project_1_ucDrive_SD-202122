cd UcDriveServer
javac UcDriveServer.java ClientHandler.java User.java ClientAuth.java ServerUploadHandler.java ServerDownloadHandler.java UDPHeartbeat.java UDPPortManager.java UDPFileSender.java UDPFileReceiver.java FileTransferType.java
cd ..
cd Client
javac Client.java ClientAuth.java ClientDownloadHandler.java ClientUploadHandler.java
cd ..

pause