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
import java.util.Properties;
import java.lang.Math;

class UDP_TCPConverter
{
   public static class Initializations
   {
      //File Recording Options
      public boolean recordData;
      public Path filePath;

      //Network Configuration
      public int inputPort;
      public InetAddress outputIP;
      public int outputPort;

      //Debug options
      public boolean consoleOutput;

      public void debugOut(String output) {if (consoleOutput) System.out.println(output);}

      public String convertInputData(byte[] receiveData, int numDataStreams, String output)
      {
         for (int dataGroup = 0; dataGroup<numDataStreams; ++dataGroup) //For each data stream
         {
            int xPlaneIndex = receiveData[5+36*dataGroup];
            debugOut("Index: " + xPlaneIndex);
            for(int dataIndex=0; dataIndex<8; ++dataIndex)
            {
               byte[] floatBytes = {receiveData[(36*dataGroup)+(4*dataIndex)+9], //Offset by 9 because of 5 byte data and 4 byte xPlaneIndex
                  receiveData[(36*dataGroup)+(4*dataIndex)+10],
                  receiveData[(36*dataGroup)+(4*dataIndex)+11],
                  receiveData[(36*dataGroup)+(4*dataIndex)+12]};
               //Testing
                  // byte[] test1_array = {(byte)0x00, 0x40, (byte)0xF6, (byte)0x42};
                  //AB, 67, 51, BF works for Little Endian
                  //00, 40, F6, 42 also works for little endian
               float convertedNumber = ByteBuffer.wrap(floatBytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
               output = (output + Float.toString(convertedNumber) + ",");
               debugOut("Float "+dataGroup+","+dataIndex+": " + convertedNumber);
            }
         }
         return output;
      }

   }

   //Maybe make function to choose whether or not to read the init file
   //Initializes the program with the config file and reads in the list of data groups
      //We'll pass in the dictionary by reference.
   //Dictionary{string dataStreamName: (int dataGroup,int index)} dataInformation
   public static void init(Initializations config) throws Exception
   {
      //Read in the config file
      Properties prop = new Properties();
      String fileName = "config.ini";
      InputStream is = new FileInputStream(fileName);
      prop.load(is);

      if (prop.getProperty("file.recordData").equals("true")) config.recordData = true;
      else config.recordData = false;

      config.filePath = Paths.get((prop.getProperty("file.directory")).trim());

      config.inputPort = Integer.parseInt(prop.getProperty("network.inputPort"));
      config.outputIP = InetAddress.getByName(prop.getProperty("network.outputIP"));
      config.outputPort = Integer.parseInt(prop.getProperty("network.outputPort"));

      if (prop.getProperty("debug.consoleOutput").equals("true")) config.consoleOutput = true;
      else config.consoleOutput = false;
      //Read in the list of data groups and indexes
         //Create dictionary with {data stream name : (data group, index)}
   }

   public static void readInDataStreams(){} // from XML
   //Create vector with each index holding an array of tuples of the data group,index wanted.
   //Each index will hold only one data group (Sorted arbitrarily)

   public static void main(String args[]) throws Exception
   {
      //Dictionary{string dataStreamName: (int dataGroup,int index)} dataInformation
      Initializations config = new Initializations();
      init(config);//, dataInformation);

      //Create a new file to store the data taken in by the server
      // Path filePath = Paths.get("C:\\users\\gallia\\dropbox\\linux\\Sim_data_dir\\Sim_data.txt");
      if(config.recordData)
      {
         config.filePath = Paths.get((config.filePath).toString() + "\\Sim_data0.txt");
         int fileNum = 1;
         while (Files.exists(config.filePath)){
            int pathOffset = (config.filePath).toString().length()-5;
            double numOffset = -Math.floor(Math.log10(fileNum));
            String subStringFilePath = ((config.filePath).toString()).substring(0,pathOffset+(int)numOffset);
            config.filePath = Paths.get(subStringFilePath + ++fileNum + ".txt");
         }
         config.debugOut("File Path:"+((config.filePath).toString()));
         Files.createFile(config.filePath); // Once the next available file name has been found, create it
      }

      //EDIT: Number of data streams
      int numDataStreams = 1; //Will be removed due to config from user's selection of data streams in XML
      String header = "HEADER"; //Header is going to be autogenerated by parsing the XML file
      // int outputPort = 6789;
      // int inputPort = 9876;

      //end EDIT

      //Date creation
      Date date = new Date();
      SimpleDateFormat ft = new SimpleDateFormat (":yyyy-MM-dd hhmmssSSSZ:");

      //Verify that data is ready to be transmitted to pilots
      Scanner in = new Scanner(System.in);
      System.out.println("Ready to transmit?");
      in.nextLine();

      //Send header to PILOTS
      Socket clientSocket = new Socket(config.outputIP, config.outputPort);
      DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
      outToServer.writeBytes( header + '\n');

      //Open socket to start receiving data
      DatagramSocket serverSocket = new DatagramSocket(config.inputPort);

      //Begin Receiving Data
      while(true)
      {
         //Begin Reciving Data over UDP
         byte[] receiveData = new byte[41+36*(numDataStreams-1)]; //Number of bytes represents size of buffer in chars
         DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
         serverSocket.receive(receivePacket);
         //End Receivng data over UDP

         //Get the current date and time since the data has been received
         date = new Date();
         String output = ft.format(date);

         //Convert data from X-Plane format to PILOTS Format
         output = config.convertInputData(receiveData, numDataStreams, output);

         //System and file outputs
         config.debugOut(output);
         if (config.recordData) Files.write(config.filePath, output.getBytes(), StandardOpenOption.APPEND);

         //Begin Sending received and parsed data over TCP/IP
         outToServer.writeBytes(output + '\n');
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