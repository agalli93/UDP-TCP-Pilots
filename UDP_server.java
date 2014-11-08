import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.FileSystemException;
import java.nio.file.StandardOpenOption;
import java.net.*;


class UDPServer
{
   public static void main(String args[]) throws Exception
   {
      //Create a new file to store the data taken in by the server
      Path file_path = Paths.get("C:\\users\\gallia\\dropbox\\linux\\Sim_data_dir\\Sim_data.txt");
      boolean test = false;
      int file_num = 0;
      while (Files.exists(file_path)){
         file_path = Paths.get("C:\\users\\gallia\\dropbox\\linux\\Sim_data_dir\\Sim_data" + ++file_num + ".txt");
      }
      Files.createFile(file_path); // Once the next available file name has been found, create it

      DatagramSocket serverSocket = new DatagramSocket(9876);
      while(true)
      {
         byte[] receiveData = new byte[1024]; //Number of bytes represents size of buffer in chars
         byte[] sendData = new byte[1024];
         DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
         serverSocket.receive(receivePacket);
         String sentence = new String( receivePacket.getData());
         System.out.println("RECEIVED: " + sentence);
         Files.write(file_path, receiveData, StandardOpenOption.APPEND);
         InetAddress IPAddress = receivePacket.getAddress();
         int port = receivePacket.getPort();
         sendData = sentence.getBytes();
         DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
         serverSocket.send(sendPacket);
      }
   }
}
/*
Current problem is that when the byte array is written into the file, all 1024 bytes are written, regardless of their value. Need to find a way
to determine how many bytes to actually record. X-plane might have a limit so we could just use that. For now, possibly binary searching the last
bit. */