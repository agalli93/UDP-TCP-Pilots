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

class Pair<T>
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

class UDP_TCPConverter
{
   static boolean debug = false;
   private static void debugOut(String output) {if (debug) System.out.println(output);}

   private static class Initializations
   {
      //File Recording Options
      public boolean recordData;
      public Path filePath;

      //Network Configuration
      public int inputPort;
      public InetAddress outputIP;
      public int outputPort;
      public boolean writeToTCP;

      //Debug options set globally for ease of programming
   }

   //Finished
   public static String convertInputData(byte[] receiveData, int numDataStreams, String output, Initializations config)
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

   //Function to read in the list of data groups from the file specified by the user
   private static void readInGroupsList(Properties prop, Map<String, Pair<Integer> > dataGroups) throws Exception
   {
      //Read in the list of source data groups and indexes
      String dataSourceNames = prop.getProperty("dict.source");
      InputStream is = new FileInputStream(dataSourceNames);

      //Error check file existance
      if (is == null) throw new FileNotFoundException("Source Names file, " + dataSourceNames + ", not found in current directory");

      DataInputStream dis = new DataInputStream(is);
      dis.readChar(); dis.readChar(); //Clears the first carriage return and linefeed
      int intDataGroup = 0;
      int intDataIndex = 0;
      String dataStreamName = new String();
      //While there are still chracters available
      while (dis.available()>0)
      {
         char c = dis.readChar();
         //If delineator is reached, take that data group name and <dG,dI> and insert it into the map
         if (c == '|')
         {
            dataStreamName.trim();// trim the whitespace
            Pair<Integer> groupAndIndex = new Pair<Integer>(intDataGroup,intDataIndex);
            dataGroups.put(dataStreamName, groupAndIndex);
            dataStreamName = new String();
            //If exceeded # of indexes in a group, increment group and reset index
            if (++intDataIndex == 8)
            {
               ++intDataGroup;
               intDataIndex = 0;
            }
            continue;
         }
         //If we've reached the end of the first line of data stream names, break
         else if (c == '\r') break;
         //If regular character, add it to the string and continue
         dataStreamName = dataStreamName + c;
      }
   }

   //Maybe make function to choose whether or not to read the init file
      //We'll pass in the dictionary by reference.
   //Dictionary{string dataStreamName: (int dataGroup,int index)} dataInformation
   //Add to debug function with levels
   //Create Preconfigured datastreams.ini files for users who want certain data streams
   //**Initializes the program with the config file and reads in the list of data groups
   private static void init(Initializations config, Map<String, Pair<Integer> > dataGroups) throws Exception
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

      if (prop.getProperty("debug.consoleOutput").equals("true")) debug = true;
      else debug = false;

      if (prop.getProperty("network.writeToTCP").equals("true")) config.writeToTCP = true;
      else config.writeToTCP = false;

      readInGroupsList(prop, dataGroups);
   }

   //Finished if the header formats properly
   private static Integer readInUserStreams(String header, Map<String, Pair<Integer> > dataGroups, Vector<Pair<Integer> > streamVector, Set<Integer> dataGroupNums) throws Exception
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
      debugOut("Header after creation in readInUserStreams: "+ header);
      header.replaceAll(header,header); //**This very well could break it, needs further testing.
      Integer numDataStreams = dataGroupNums.size();
      return numDataStreams;
   }

   public static void main(String args[]) throws Exception
   {
      Map<String, Pair<Integer> > dataGroups = new HashMap<String, Pair<Integer> >();
      Initializations config = new Initializations();
      init(config, dataGroups);

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
         debugOut("File Path:"+((config.filePath).toString()));
         Files.createFile(config.filePath); // Once the next available file name has been found, create it
      }

      // Parsing of user requested data streams
      Integer numDataStreams; //**Remove instantiation after function finished as it will be taken
      // care of by the readIn function below
      String header = new String(); //Instantiated for the function
      Vector<Pair<Integer> > streamVector = new Vector<Pair<Integer> >(); // Vector of <dG,dI>'s requested by user
      Set<Integer> dataGroupNums = new HashSet<Integer>(); // a Set keeping record of unique data groups needed to pull
      numDataStreams = readInUserStreams(header, dataGroups, streamVector, dataGroupNums);
      debugOut("Header Test (should be the actual header if the user file is set):: " + header); //**Needs testing

      //Date creation
      Date date = new Date();
      SimpleDateFormat ft = new SimpleDateFormat (":yyyy-MM-dd hhmmssSSSZ:");

      //Verify that data is ready to be transmitted to pilots
      Scanner in = new Scanner(System.in);
      System.out.println("Ready to transmit?");
      in.nextLine();

      DataOutputStream outToServer = null;

      //Send header to PILOTS and file if the user specifies output to said stream
      if (config.writeToTCP)
      {
         Socket clientSocket = new Socket(config.outputIP, config.outputPort);
         outToServer = new DataOutputStream(clientSocket.getOutputStream());
         outToServer.writeBytes( header + '\n');
      }
      if (config.recordData) Files.write(config.filePath, header.getBytes(), StandardOpenOption.APPEND);

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

         //Network, Debug, and file outputs
         debugOut(output);
         if (config.recordData) Files.write(config.filePath, output.getBytes(), StandardOpenOption.APPEND);

         if(config.writeToTCP) outToServer.writeBytes(output + '\n');
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