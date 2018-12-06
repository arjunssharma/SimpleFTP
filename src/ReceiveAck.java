import java.io.*;
import java.util.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ReceiveAck extends Thread {
    public DatagramSocket datagramSocket;
    private volatile boolean transfer;

    public ReceiveAck(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
        transfer = true;
    }
    
    public void setTransfer() {
        transfer = false;
    }

    public void run() {
        try {
            while(transfer) {
                    byte buffer[] = new byte[2084];
                    DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
                    boolean connection = datagramSocket.isClosed();
                if(!connection) {
                    datagramSocket.receive(datagram);
                    ByteArrayInputStream byteInputStream = new ByteArrayInputStream(datagram.getData());
                    ObjectInputStream outputStream = new ObjectInputStream(byteInputStream);
                    udpDatagram packet = (udpDatagram) outputStream.readObject();
                    if (packet.type == (short)43690) { 
                    	Simple_ftp_client.ack = packet.seqNumber;
                    }
                }
            }
        }
        catch (Exception e) {
        	System.out.println("Error");
    		e.printStackTrace();
        }
    }
}