cd UcDriveServer
jar cmf MANIFEST.MF UcDriveServer.jar *.java *.class
cd ..
cd Client
jar cmf MANIFEST.MF Client.jar *.java *.class
cd ..

pause