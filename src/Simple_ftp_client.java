import java.util.*;
import java.io.*;
import java.net.*;

// Simple-FTP client acts as sender
public class Simple_ftp_client extends Thread {

	public static DatagramSocket clientSocket = null;
	public static udpDatagram udpBuffer[]=null;
	
	//Command line input arguments for Simple FTP Server
	public static String serverHostName;
	public String filename;
    public static int port;
	public static int N;
	public static int MSS;
	
	//In order to fetch IP address later
	public static java.lang.String ipaddress;
	public static final int SERVER_PORT = 7735;
    InetAddress ip;

   //Other attributes
    public static int transCount = -1;
	public volatile static int ack = -1;
	public static int timeout = 1000;
	public static int packetCount;
	public static int lastPacket;
	private udpDatagram packet;
	public static int lostPacketCount = 0;
    public static boolean nextPacket = true;
    public String serverName;
    public int serverPort;

    public Simple_ftp_client(String serverHostName, String name, int N, int maxSegSize) throws Exception {
    	this.serverName = serverHostName;
    	this.serverPort = Simple_ftp_server.SERVER_PORT;
    	this.filename = name;
    	this.N = N;
    	this.MSS = maxSegSize;
    	Random rand = new Random();
    	int port = rand.nextInt(2000) + 2000;	
    	clientSocket = new DatagramSocket(port);
    	clientSocket.connect(InetAddress.getByName(serverName), serverPort);
    	udpBuffer = new udpDatagram[this.N];
        String ipaddress = InetAddress.getLocalHost().getHostAddress();
        int localPort = clientSocket.getLocalPort();
        System.out.println("The Simple FTP client is started.");
        System.out.println("It is running on " + ipaddress);
	}
    
    public static void sendPacket(udpDatagram packet) throws Exception {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOutputStream);
        objectOutputStream.writeObject(packet);
        byte[] data = byteOutputStream.toByteArray();
        DatagramPacket datagram = new DatagramPacket(data, data.length,
        		InetAddress.getByName(serverHostName), port);
        if(packet != null) {
        	packet.sentTime = System.currentTimeMillis();
        }
        clientSocket.send(datagram);
    }
    
	public static void main(String[] args) throws Exception {
    	try {
    		if(args.length == 5 ) {
    			if(SERVER_PORT != Integer.parseInt(args[1]))
    				port = Integer.parseInt(args[1]);
    				else
    					port = SERVER_PORT;
    			String serverHostName= args[0];
    			String filename = args[2];
    			int N = Integer.parseInt(args[3]);
    			int maxSeg = (int) (Long.parseLong(args[4]));
    			Simple_ftp_client sc = new Simple_ftp_client(serverHostName, filename, N, maxSeg);
    			sc.start();
    			System.out.println("Sending packets"); 
    		        boolean open = true;
    		        long start;
    		        File file;
    		        try {
    		        	if(open) {
    			            start = System.currentTimeMillis();
    			            file = new File(filename);
    		                boolean fileExists = file.exists();

    		                if(fileExists) {
    			            	int size = (int) file.length();
    		                    // Total number of packets
    			  	            packetCount = size / MSS;
    			  	            // Finding the last packet
    		                    lastPacket = size % MSS;
    			  	            byte data[] = new byte[MSS];
    			  	            ReceiveAck newpacket = null;
    			  	            FileInputStream fileInputStream = new FileInputStream(file);
    			  	            while(fileInputStream.read(data) > -1){
    			  	                
    		                        // Incrementing no. of packets lost counter if timeout occurs
    		                        while(transCount - ack == N) {
    		                            // If timeout occurs
    		                            if(System.currentTimeMillis() - udpBuffer[(ack +1) % N].sentTime > timeout) {
    		                                int tempAck = ack;

    		                                for(int i = 0; i < (transCount - ack); i++) {
    		                                    System.out.println("Timeout occured for sequence number: " + udpBuffer[(tempAck + 1 + i) % N].seqNumber);
    		                                    lostPacketCount++;
    		                                    sendPacket(udpBuffer[(tempAck+1+i) % N]);
    		                                }
    		                            }
    		                        }

    		                        byte datagram1[] = new byte[MSS];
    		                    	byte datagram2[] = new byte[lastPacket];
    		                        datagram1 = data;
    		                        int checksum = 0;
    		                        int length = datagram1.length;
    		                        transCount++;
    		                        if(transCount == packetCount) {
    		                        	for(int i = 0; i < lastPacket; i++){
    		                        		datagram2[i] = datagram1[i];
    		                        	}
    		                        	datagram1 = null;
    		                        	datagram1 = datagram2;
    		                        }
    		                        
    		                        if(datagram1 != null) {
    		                            for(int i = 0; i < length; i++) {

    		                                checksum = ((i % 2) == 0) ? (checksum + ((datagram1[i] << 8) & 0xFF00)) : (checksum + ((datagram1[i]) & 0xFF));

    		                                if((datagram1.length % 2) != 0){
    		                                    checksum = checksum + 0xFF;
    		                                }
    		                    
    		                                while((checksum >> 16) == 1) {
    		                                     checksum = ((checksum & 0xFFFF) + (checksum >> 16));

    		                                     checksum = ~checksum;
    		                                }
    		                                
    		                            }
    		                        }
    		                        udpDatagram packet = new udpDatagram(transCount, checksum, (short) 21845, datagram1);
    		                        int index = (transCount % N);
    		                        udpBuffer[index] = packet;
    		                        sendPacket(packet) ;
    			  	                if(nextPacket) {
    			  	                    newpacket = new ReceiveAck(clientSocket);
    			  	                    newpacket.start();
    		                            nextPacket = false;
    			  	                }
    			  	                int ackd = ack;

    		                        while(ackd != transCount) {
    		                            int difference = (int) (System.currentTimeMillis() - udpBuffer[(ackd+1) % N].sentTime);
    		                            if(difference > timeout){
    		                                int j = 0;
    		                                while(j < (transCount - ackd)) {
    		                                    System.out.println("Timeout, sequence number=" + udpBuffer[(ackd + 1 + j) % N].seqNumber);
    		                                    lostPacketCount++;
    		                                    sendPacket(udpBuffer[(ackd + 1 + j) % N]);
    		                                    j++;
    		                                }
    		                            }
    		                            ackd = ack;
    		                        }

    			  	            }
    		                    fileInputStream.close();
    		                    long end = System.currentTimeMillis();  
    		                    sendPacket(null);
    		                    newpacket.setTransfer();
    		                    open = false;
    		                    clientSocket.close();  		                   
    			            }
    		        	}
    		        } catch(Exception e) {
    		        	System.out.println("Error");
    		    		e.printStackTrace();
    		        }
    		    }
		else {
		System.out.println("Incorrect format");
		System.out.println("Correct format is java SimpleFTPClient server-host-name server-port file-name N MSS");
		return;
      }
    }
    	catch(Exception e) {
    		System.out.println("Error");
    		e.printStackTrace();
    	}
}
}
