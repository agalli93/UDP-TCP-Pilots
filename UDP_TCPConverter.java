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
   private class Pair<T>
   {
      public Pair() { first = null; second = null; }
      public Pair(T first, T second) { this.first = first;  this.second = second; }

      public T getFirst() { return first; }
      public T getSecond() { return second; }

      public void setFirst(T newValue) { first = newValue; }
      public void setSecond(T newValue) { second = newValue; }

      private T first;
      private T second;
   }

   private static class Initializations
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
   }

   public static String convertInputData(byte[] receiveData, int numDataStreams, String output, Initializations config)
   {
      for (int dataGroup = 0; dataGroup<numDataStreams; ++dataGroup) //For each data stream
      {
         int xPlaneIndex = receiveData[5+36*dataGroup];
         config.debugOut("Index: " + xPlaneIndex);
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
            config.debugOut("Float "+dataGroup+","+dataIndex+": " + convertedNumber);
         }
      }
      return output;
   }

   //Maybe make function to choose whether or not to read the init file
      //We'll pass in the dictionary by reference.
   //Dictionary{string dataStreamName: (int dataGroup,int index)} dataInformation
   //Add to debug function with levels
   //Add feature to disable TCP output
   //Create Preconfigured datastreams.ini files for users who want certain data streams
   //Read in dictionary of data Groups and Indexes
   //**Need to get map of dataGroups

   //Initializes the program with the config file and reads in the list of data groups
   private static void init(Initializations config) throws Exception
   {
      //Read in the config file
      Properties prop = new Properties();
      String fileName = "config.ini";
      InputStream is = new FileInputStream(fileName);

      if (is != null) prop.load(is);
      else throw new FileNotFoundException("Config File, " + fileName + ", not found in current directory");

      is.close();

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

   private Integer readInUserStreams(String header, Map<String, Pair<Integer> > dataGroups, Vector<Pair<Integer> > streamVector, Set<Integer> dataGroupNums) throws Exception
   {
      //Load user selected data stream file
      Properties userStreams = new Properties();
      String fileName = "userSelections.ini"; //**Might want to make this a command line arg or put it in the config file
      InputStream is = new FileInputStream(fileName);
      //Error check to make sure file exists
      if (is != null) userStreams.load(is);
      else throw new FileNotFoundException("Data Stream Selections file, " +fileName + ", not found in current directory");
      is.close();

      // Declare set of strings that are the dataStreamUser names and a set of Integers to keep track of the unique dataGroups that will
      // need to be requested from X-Plane.
      Set<String> userKeys = userStreams.stringPropertyNames();
      Iterator<String> userKeysItr = userKeys.iterator();

      // Get the keys from the file, put them in the header, then get their dataGroup/dataIndex and store them
      while (userKeysItr.hasNext()) {
         String key = userKeysItr.next();
         //Add the key to the header file
         header = (header + key + ',');
         //Retreive the dG/dI pair, store the data group to track of unique dataGroups needed
         Pair<Integer> groupIndexPair = dataGroups.get(userStreams.getProperty(key));
         dataGroupNums.add(groupIndexPair.getFirst());
         streamVector.add(groupIndexPair);
      }
      header = header.substring(0,header.length()-2); // Removes hanging comma
      //**Need to pass string by reference, maybe use replaceAll() *********
      Integer numDataStreams = dataGroupNums.size();
      return numDataStreams;
   }

   //**private void readInXplaneDataStreams


   public static void main(String args[]) throws Exception
   {
      //Dictionary{string dataStreamName: (int dataGroup,int index)} dataInformation
      Initializations config = new Initializations();
      init(config);//, dataInformation);

      //Create a new file to store the data taken in by the server
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


      //**Call function to retreive data groups.

      // Parsing of data Streams
      Integer numDataStreams = new Integer(5); //**Remove instantiation as it will be taken care of by the readIn function below
      String header = new String(); //Header is going to be autogenerated by parsing the XML file
      Vector<Pair<Integer> > streamVector = new Vector<Pair<Integer> >(); // Vector of dG/dI's requested by user
      Set<Integer> dataGroupNums = new HashSet<Integer>(); // Set keeping record of unique data groups needed
      // numDataStreams = readInUserStreams(header, dataGroups, streamVector, dataGroupNums);


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
         output = convertInputData(receiveData, numDataStreams, output, config);

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