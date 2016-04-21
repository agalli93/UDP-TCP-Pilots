Readme File

This program is the PILOTS UDP to TCP converter for use with xPlane 9.0 and other versions with modifications. It is designed to receive data from xPLane over UDP, convert that data into a PILOTS readable format, and then send it to PILOTS over TCP.

Current steps to use the software:
i) edit your config file as desired
1) Determine your local IP address. 
2) input that IP address into xPlane and the port which you specify 
3) write that same port from above into the "inputPort" field of the config.ini file.
4) select on xPLane which of the data streams you want to write into PILOTS
	a) Note the stream group # and the index position of the data stream within it.
//below doesn't work because of header ordering
5) write the header file in the userSelections.ini file in the order the data streams appear in PILOTS similar to the style (the data streams names at this time are irrelevant, what's relevant is the order is the same order as the xPlane data streams and the PILOTS variables are written as they appear in the PILOTS program)
6) Hard code into the convertInputData function which data streams you would like to use based on the stream group # and index positions found in 4a.
7) write the correct port for PILOTS into the config.ini file.
8) initialize PILOTS and run the UDP_TCPConverter. 

Future steps to use software:
1) Determine your local IP address. 
2) input that IP address into xPlane and the port which you specify 
3) write that same port from above into the "inputPort" field of the config.ini file.
4) write the PILOTS data streams equal to their xPlane counterpart in the userSelections.ini file.
5) initialize PILOTS and run the UDP_TCPConverter. 

Files: 

UDP_TCPConvterter.java

Contains all of the functions of the parser. 

future work: This probably should be broken into smaller files. 

userSelections.ini

This file contains the user's selections for datastreams to be used in PILOTS and the ones requested from xPlane. Care should be taken after the future work is implemented to ensure to the order that these are output since currently they're output in the order of the data stream as dictated by the for loop in convertInputData.

config.ini

This is the config file the user will use to specify certain frequently changing parameters. Details are shown in the config file. 

sourceNames.txt

This is the file used to tell the Parser what are the data stream names from xPlane and their dataGroup/dataIndex locations. This file will be a one time generation and never changed unless a different xPLane input is used. In addition, if "null" strings where there are gaps in the data are not inserted, the dataGroup/dataIndex locations will be off and not be correct for future use. 

Functions: 

public static String convertInputData(byte[] receiveData, int numDataStreams, String output, Initializations config)
	parameters: 
		byte[] receiveData: byte array with the recieved data
		int numDataStreams: the number of datastreams that are being sent by xPlane 
		String output: this is the string output that will go to PILOTS
		Initializations config: the passed in config file used only for  debugOut function here
	return: returns the string output to PILOTS

	This function is used to take the incoming data from xPlane and the convert it from a byte array representing a float (in little endian) to a float in the form of a string

	future work: select which data streams to write to output based on user selected input from userSelections.ini after being parsed. 

===

private static void readInGroupsList(Properties prop, Map<String, Pair<Integer> > dataGroups) throws Exception
	parameters: 
		Properties prop: properties list that is parsed from the config.ini file. 
		Map<String, Pair<Integer> > dataGroups: map of the dataGroups written by xPlane 
			String: data stream name 
			Pair<Integer>: data Group, data index location pair.

	This function is used to read in the data stream names and determine their data group/data index numbers. This requires a full dataGroups file without any gaps of streams, there can't be a missing data group inbetween since the parser relies on them being sequential to determine their index number. 

	future work: currently doesn't work since the sourceNames.txt file doesn't insert the gaps that are still sent over UDP. Gaps need to be inserted with "null" or another string. 

===

private static void init(Initializations config, Map<String, Pair<Integer> > dataGroups)
	parameters: 
		Initializations config: class with config data
		Map<String, Pair<Integer> > dataGroups: map of the dataGroups read in by "readInGroupsList" function

	This function is used to write the initializations set by the user in the config file into the initializations 
	class to be referenced by the entire program. 

===

private static Integer readInUserStreams(StringBuilder header, Map<String, Pair<Integer> > dataGroups, Vector<Pair<Integer> > streamVector, Set<Integer> dataGroupNums) throws Exception
	parameters: 
		StringBuilder header: header string that will be the first packet sent to PILOTS
		Map<String, Pair<Integer> > dataGroups: map of the dataGroups read in by "readInGroupsList" function
		Vector<Pair<Integer> > streamVector: vector of the dataGroup/dataIndex pairs, determined by the readIngroupsList, and selected in the userSelections.ini file.
		Set<Integer> dataGroupNums: set of the dataGroups that are required by the streamVector.
	return: 
		Integer numDataStreams: used to size the byte array for the incoming data from xPlane.

	This function takes in the userSelections.ini file and reads in the PILOTS specified data streams to create a header string of them, and the xPlane data streams they're equal to, to determine which dataGroup/dataIndex pairs are required to be read and written into PILOTS. Care should be taken after the future work is implemented to ensure to the order that these are output since currently they're output in the order of the data stream as dictated by the for loop in convertInputData. Currently it would be necessary to ensure that either: the header is reordered to reflect the dataGroup/dataIndex list numerically, or the user must write the userSelections.ini file in the order of the xPlane dataGroups/dataIndexes. 

	future work: use the dataGroupNums to select which data to request from xPlane (via Fred's code). Use the streamVector pairs to determine which data streams to keep and which to throw out. Change the order that the PILOTS variables are written to the header to reflect xPlane's data ordering since that is the order in which it is processed and written into the output string. 

===

public static void main(String args[]) throws Exception
	
	Executes the program as follows:
		Reads in the config.ini file and processes the configs
		Create a new file to store the data taken in by the server //currently not functioning on Linux.
		Build the pilots header
			Read in the user selected data streams
		Create the timestamp and verify the user has PILOTS set up to receive the data.
		If the user specified to write to TCP, set up the sockets
		If the user specified to record the data, write the header to the file
		while true:
			receive data from xPLane
			timestamp it
			convert the input data
			if record: write the data to file
			if write to TCP: send over TCP

===


Next Steps:

Create a full source names file with nulls included.

Break down the main file into smaller files. 

Use the real time from xPlane to feed the time into Pilots (since UDP isn't ordered, time stamp of the conversion program may artificially order the data)

Control the frequency of the data sent to PILOTS. Possibly by setting a data frequency in the config.ini file.

Implement Fred's data group requesting code. 

Change the order that the PILOTS variables are written to the header to reflect xPlane's data ordering since that is the order in which it is processed and written into the output string. 
Use the dataGroupNums to select which data streams to request from xPlane (via Fred's code) instead of having to do it manually. 
Use the streamVector pairs to determine which data streams to write to the output instead of having to currently hard code it. 



URLs Used
http://systembash.com/content/a-simple-java-udp-server-and-udp-client/
http://systembash.com/content/a-simple-java-tcp-server-and-tcp-client/
http://docs.oracle.com/javase/tutorial/essential/io/file.html#creating
http://docs.oracle.com/javase/8/docs/api/java/nio/file/StandardOpenOption.html
http://stackoverflow.com/questions/13469681/how-to-convert-4-bytes-array-to-float-in-java //converting function I used

http://www.nuclearprojects.com/xplane/info.shtml
http://www.jefflewis.net/XPlaneUDP_9.html
http://www.nuclearprojects.com/xplane/xplaneref.html
http://www.scadacore.com/field-applications/miscellaneous/online-hex-converter.html //testing of conversion with hex

Config File URLs
http://stackoverflow.com/questions/1925305/best-config-file-format
http://www.gnu.org/prep/standards/html_node/Configuration.html
