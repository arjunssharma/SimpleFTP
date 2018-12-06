import java.io.*;
import java.util.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

class udpDatagram implements Serializable
{

	private static final long serialVersionUID = 1L;
	public byte dataArray[];
    public long type=21845;
    public int seqNumber;
    public int checksum;
    public long sentTime;
    public long uid;

    public udpDatagram(int seqNumber, int checksum, short type, byte[] dataArray) {
        this.dataArray = dataArray;
        this.seqNumber = seqNumber;
        this.uid = 1L;
        this.checksum = checksum;
    }

}

public class Simple_ftp_server extends Thread {
	
	static DatagramSocket serverSocket = null;
	//Command line input arguments for Simple FTP Server
	public static final int SERVER_PORT = 7735;
	public static int port;
	String filename;
	public static float probability;
	
	//In order to fetch IP address later
	public static java.lang.String ipaddress;
	
	//For reading the file
	FileOutputStream fos;
	
	//Other attributes
    public static int checksum = 0;
	public static String type = "0101010101010101";
	public static int typeC = 21845;
    public static int len=0;
	public static int ack = -1;
	public static boolean receive = true;

	    public Simple_ftp_server(String filename, float probability) throws Exception {
	    	this.ack = -1;
			receive = true;
			this.filename = filename;
	        this.port = port;
	        Simple_ftp_server.probability = probability;
	        serverSocket = new DatagramSocket(this.port);
	        System.out.println("The probability for the packet loss is " + probability);
	    }    

		public Simple_ftp_server() throws SocketException {
			this.ack = -1;
			receive = true;
			this.filename = filename;
	        this.port = port;
	        Simple_ftp_server.probability = probability;
	        serverSocket = new DatagramSocket(this.port);
	        System.out.println("The probability for the packet loss is " + probability);
		}
     

	public static int goBackNARQ(BufferedOutputStream bufferedOutputStream,
			int count) throws Exception {
	    while (receive) {
	    	// Calculating the random probability for packet loss
			float r;
		    Random rand = new Random();
		    r = rand.nextFloat();
		    //Data receiving
		    byte[] receiveData = new byte[2048];
		    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		    serverSocket.receive(receivePacket);
		    byte[] db= new byte[receivePacket.getLength()-64];
		    System.arraycopy(receiveData,64, db,0,db.length);
            byte[] received=receivePacket.getData();
            int seqno= bintoDec(received,32);
            byte[] data_received= receivePacket.getData();
            ByteArrayInputStream byteInputStream = new ByteArrayInputStream(data_received);
		    ObjectInputStream outputStream = new ObjectInputStream(byteInputStream);
		    udpDatagram udpDatagram = (udpDatagram) outputStream.readObject();

		    if(udpDatagram == null) {
		        System.out.println("Packets Lost = " + count);
		        bufferedOutputStream.close();
		        serverSocket.close();
		        break;
		    }
		    // Calculating checksum
		    int checksum = 0;        
//	        if (udpDatagram.dataArray != null) {
//	        	len=udpDatagram.dataArray.length;
//	        	byte[] caldata = udpDatagram.dataArray;
//	        	checksum = calChecksum(len, caldata);
//	        }

	        if (udpDatagram.dataArray != null) {
	        	for(int i = 0; i < udpDatagram.dataArray.length; i++) {

	            	checksum = ((i % 2) == 0) ? (checksum + ((udpDatagram.dataArray[i] << 8) & 0xFF00)) 
	            			: (checksum + ((udpDatagram.dataArray[i]) & 0xFF));

		        	if((udpDatagram.dataArray.length % 2) != 0){
		        		checksum = checksum + 0xFF;
		        	}
		
		            while ((checksum >> 16) == 1){
		            	 checksum =  ((checksum & 0xFFFF) + (checksum >> 16));
		                 checksum =  ~checksum;
		            }
	        	} 
	        }
	        
	        if(ack == udpDatagram.seqNumber) {
	        	 int sequenceNum = udpDatagram.seqNumber;
	        	 udpDatagram ackDatagram = new udpDatagram(sequenceNum, 0, (short)43690, null);
	             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	             ObjectOutputStream outStream = new ObjectOutputStream(byteArrayOutputStream);
	             outStream.writeObject(ackDatagram);
	             byte[] data = byteArrayOutputStream.toByteArray();
	             DatagramPacket acknowledgementPacket = new DatagramPacket(data, data.length,receivePacket.getAddress(), receivePacket.getPort());
	             serverSocket.send(acknowledgementPacket);
	        }
		    else {
				if(r < probability) {
					count++;
					System.out.println("Packet loss, sequence number = " + udpDatagram.seqNumber);
				}
				else {
					 bufferedOutputStream.write(udpDatagram.dataArray);
					 bufferedOutputStream.flush();
					 int sequenceNum = udpDatagram.seqNumber;
		        	 udpDatagram ackDatagram = new udpDatagram(sequenceNum, 0, (short)43690, null);
		             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		             ObjectOutputStream outStream = new ObjectOutputStream(byteArrayOutputStream);
		             outStream.writeObject(ackDatagram);
		             byte[] data = byteArrayOutputStream.toByteArray();
		             DatagramPacket acknowledgementPacket = new DatagramPacket(data, data.length,receivePacket.getAddress(), receivePacket.getPort());
		             serverSocket.send(acknowledgementPacket);
					 ack++;
				}
		    }
		}

		return count;
	}
	
	private static int calChecksum(int len, byte[] datagram) {
    	for(int i = 0; i < len; i++) {

        	checksum = ((i % 2) == 0) ? (checksum + ((datagram[i] << 8) & 0xFF00)) : (checksum + ((datagram[i]) & 0xFF));

        	if((len % 2) != 0){
        		checksum+= 0xFF;
        	}

            while ((checksum >> 16) == 1){
            	 checksum =  ((checksum & 0xFFFF) + (checksum >> 16));
                 checksum =  ~checksum;
            }
    	}
    	return checksum;
	}
	
	   private static int bintoDec(byte[] st, int n) {
           String str=new String(Arrays.copyOfRange(st, 0, 32)); 
           //System.out.println(str);
          double j=0;
          for(int i=0;i<str.length();i++){
              if(str.charAt(i)== '1'){
               j=j+ Math.pow(2,str.length()-1-i);
           }

          }
          //System.out.println("bintodeci"+(int)j);
          return (int)j;
      }

	private static Object String(long type2) {
		// TODO Auto-generated method stub
		return null;
	}

	 public static void main(String[] args) throws SocketException { 
	    	Simple_ftp_server sfs=new Simple_ftp_server();
	        sfs.filename=args[1];  
	        int currenttot=0;
	        //To maintain packet loss count
	        int count = 0;
	    	try {
	    		if (args.length == 3) {
	    			if(SERVER_PORT != Integer.parseInt(args[0]))
	    	        	port = Integer.parseInt(args[0]);
	    	        else
	    	        	port = SERVER_PORT;
	    	        String filename = args[1];
	    	        
	    	        if(Float.parseFloat(args[2])>0 && Float.parseFloat(args[2])<=1)
	    	        	probability=Float.parseFloat(args[2]);
	    	        else
	    	        {
	    	        	System.out.println("Probability should be between 0 and 1");
	    	            return;
	    	        }
	    	    	System.out.println("Waiting to receive packets"); 
	    	    	Simple_ftp_server sf= new Simple_ftp_server( filename, probability);
	    	        sf.start();
	    	        ipaddress= InetAddress.getLocalHost().getHostAddress();
	    	        int portN=serverSocket.getLocalPort();
	    	        System.out.println("The Simple FTP Server is started.");
	    	        System.out.println("It is running on " + ipaddress + " on port " + portN);
	    	        try {
	    		        File file = new File(filename);
	    		    	FileOutputStream fOS = new FileOutputStream(file);
	    		        BufferedOutputStream outputStream = new BufferedOutputStream(fOS);
	    		        count = goBackNARQ(outputStream, count);
	    			}
	    			catch(Exception e) {
	    				receive=false;
	    			}
		        }
	    		else {
		         System.out.println("Incorrect format");
	             System.out.println("Correct format is java simpleFTPServer portNumber file-name probability");
	             return;
	    		}
	    	}
	    	catch(Exception e) {
	    		System.out.println("Error");
	    		e.printStackTrace();
	    	}
	    }


}