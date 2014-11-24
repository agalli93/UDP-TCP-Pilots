README UDP/TCP Pilots:

UDP/TCP Server with the intended use of receiving data from X-Plane and communicating it to PILOTS
UDP_TCP Converter will take data in from UDP and output it directly over TCP

Notes for future release:

Data request without having output set:

Now, say that you are writing an add-on or something for X-Plane and you want your motion-platform or cockpit to send in a request to X-Plane to send a bunch of data out like this, because you are getting tired of going into the data output screen and making selections of data to output all the time. In that case you will SEND a packet just like the one above to X-Plane, but the label will be "DSEL". The data will be a series of integers indicating which data output you want! (1 for the first in the list, 2 for the second, etc).

So "DSEL0456" would request that X-Plane send the fourth, fifth, and sixth items in the data output screen many times per second to the IP address listed in the Internet Settings screen. DSEL is in characters, but 4 5 6 are YOUR MACHINE-BYTE-ORDER integers.

SOUN //could be used for announcing certain failures (eg. master warning/airspeed failure)
DATA INPUT STRUCTURE:
struct soun_struct{ // play any sound
xflt freq,vol; xchr path[strDIM];};
Use this to simply play a WAV-file sound. Enter the path of the WAV file in the struct. The freq and volume scale 0.0 to 1.0. Easy!

FAIL //allows us to fail parts remotely
DATA INPUT STRUCTURE:
Fail a system, where the data will indicate which system to fail. The system to fail is sent as an ASCI STRING (ie: "145"), where the 0 is the first failure listed in the failure window in X-Plane (currently the vacuum system) and incremented by 1 from there.

RECO
DATA INPUT STRUCTURE:
Recover a system, where the data will be an integer indicating which system to recover. The system to recover is sent as an ASCI STRING (ie: "145"), where the 0 is the first failure listed in the failure window in X-Plane (currently the vacuum system) and incremented by 1 from there.

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
