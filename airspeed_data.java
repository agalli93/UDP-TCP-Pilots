import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.FileSystemException;
import java.nio.file.StandardOpenOption;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.*;
import java.text.*;
import java.util.Scanner;

class airspeed_data
{
   public static void main(String args[]) throws Exception
   {
      //Create a new file to store the data taken in by the server
      Path file_path = Paths.get("C:\\users\\gallia\\dropbox\\linux\\Sim_data_dir\\Sim_data.txt");
      int file_num = 0;
      while (Files.exists(file_path)){
         file_path = Paths.get("C:\\users\\gallia\\dropbox\\linux\\Sim_data_dir\\Sim_data" + ++file_num + ".txt");
      }
      Files.createFile(file_path); // Once the next available file name has been found, create it

      //EDIT: Number of data streams
      int num_data_streams = 4;
      String header = "#true_air_speed,ground_speed,wind_speed,wind_angle,air_angle,ground_angle";
      int output_port = 6789;
      int input_port = 9876;
      //end EDIT

      //Date creation
      Date date = new Date();
      SimpleDateFormat ft = new SimpleDateFormat (":yyyy-MM-dd hhmmssSSSZ:");

      //Verify that data is ready to be transmitted to pilots
      Scanner in = new Scanner(System.in);
      System.out.println("Ready to transmit?");
      in.nextLine();

      //Send header to PILOTS
      Socket headerSocket = new Socket("localhost", output_port);
      DataOutputStream headerToServer = new DataOutputStream(headerSocket.getOutputStream());
      headerToServer.writeBytes( header + '\n');
      headerSocket.close();

      //Open socket to start receiving data
      DatagramSocket serverSocket = new DatagramSocket(input_port);

      //Begin Receiving Data
      while(true)
      {
         //Begin Reciving Data over UDP
         byte[] receiveData = new byte[41+36*(num_data_streams-1)]; //Number of bytes represents size of buffer in chars
         DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
         serverSocket.receive(receivePacket);
         //End Receivng data over UDP

         //Get the current date and time since the data has been received
         date = new Date();
         String output = ft.format(date);

         //Convert data from X-Plane format to PILOTS Format
         for (int data_group = 0; data_group<num_data_streams; ++data_group) //For each data stream
         {
            int xplane_index = receiveData[5+36*data_group];
            System.out.println("Index: " + xplane_index);
            for(int i=0; i<8; ++i)
            {
               if ((xplane_index==3 && (i==0 || i==3)) || (xplane_index == 5 && (i==4 || i == 5))||
                  (xplane_index == 18 && i==3) || (xplane_index == 19 && i == 3)){
                     byte[] float_bytes = {receiveData[4*i+36*data_group+9], receiveData[4*i+36*data_group+10],
                      receiveData[4*i+36*data_group+11], receiveData[4*i+36*data_group+12]};
                     //Testing
                        // byte[] test1_array = {(byte)0x00, 0x40, (byte)0xF6, (byte)0x42};
                        //AB, 67, 51, BF works for Little Endian
                        //00, 40, F6, 42 also works for little endian
                     float converted_number = ByteBuffer.wrap(float_bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                     output = (output + Float.toString(converted_number) + ",");
                     // System.out.println("Float: " + converted_number);
               }
            }
            //Testing
            // output = output + "-|";
            if (data_group+1 == num_data_streams) output = output.substring(0,output.length()-1);
         }
         //System and file outputs
         System.out.println(output);
         Files.write(file_path, output.getBytes(), StandardOpenOption.APPEND);

         //Begin Sending received and parsed data over TCP/IP
         Socket clientSocket = new Socket("localhost", output_port);
         DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
         outToServer.writeBytes(output + '\n');
         clientSocket.close();
         //End Sending data over TCP

      }
   }
}
/*
Current problem is that when the byte array is written into the file, all 1024 bytes are written, regardless of their value. Need to find a way
to determine how many bytes to actually record. X-plane might have a limit so we could just use that. For now, possibly binary searching the last
bit.
-Potentially Solved, limit is 41 bytes I seem to have found
-Potentially Not solved, limit could change with number of data outputs set in X-Plane, requires further testing

Input format is:
int Index //index of the list of variables
float data[8]//up to the 8 numbers output on the screen, not all 8 will be used

First 5 bytes are the Identifier (eg. DATA) (fifth bit is insignificant)
Second 4 bytes is the index as an int
After, 8 4-byte segments are the data outputs in floating point notation
int and float pattern will repeat for as many data outputs were set

Careful reading/sending data and converting it because it might need to be char 0 not int 0
*/