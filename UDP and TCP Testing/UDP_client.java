import java.io.*;
import java.net.*;

class UDPClient
{
   public static void main(String args[]) throws Exception
   {
      while(true){
         // BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
         DatagramSocket clientSocket = new DatagramSocket();
         InetAddress IPAddress = InetAddress.getByName("hw1-b00");
         byte[] sendData = new byte[512];
         byte[] receiveData = new byte[512];
         String sentence = "Alessandro Galli, FEP2, 10.27.110.70"; //inFromUser.readLine();

         sendData = sentence.getBytes();
         DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 17);
         clientSocket.send(sendPacket);

         /* Receiving confirmation of arrival from the server with the message*/
         DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
         clientSocket.receive(receivePacket);
         String modifiedSentence = new String(receivePacket.getData());
         System.out.println("FROM SERVER:" + modifiedSentence);


         clientSocket.close();
         break;
      }
   }
}