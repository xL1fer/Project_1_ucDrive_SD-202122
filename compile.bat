cd UcDrive_Server
javac UcDrive_Server.java ClientHandler.java User.java ClientAuth.java ServerUploadHandler.java ServerDownloadHandler.java UDPHeartbeat.java UDPPortManager.java UDPFileSender.java UDPFileReceiver.java
cd ..
cd Client
javac Client.java ClientAuth.java ClientDownloadHandler.java ClientUploadHandler.java
cd ..

pause