import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class RUDPSource {
    private InetAddress serverIP; // The IP address of the server
    private int serverPort; // The port number of the server
    private DatagramSocket socket; // The socket used for communication
    private static final int TIMEOUT = 3000; // Timeout interval in milliseconds

    public RUDPSource(String serverIP, int serverPort) throws Exception {
        try {
        this.serverIP = InetAddress.getByName(serverIP);
        this.serverPort = serverPort;
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(TIMEOUT); // Set the timeout interval
        } catch (Exception e) {
            System.err.println("Error setting up client: " + e.getMessage());
        }
    }

    // Send packets to server
    public void sendPacketToServer(byte[] buffer, int start, int length) throws Exception {
        try {
            DatagramPacket packet = new DatagramPacket(buffer, length, serverIP, serverPort);
            while (true) {
                socket.send(packet);
                System.out.println("[DATA TRANSMISSION]: " + start + " | " + length);
                try {
                    // Wait for ACK
                    byte[] ackBuffer = new byte[1024];
                    DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                    socket.receive(ackPacket);
                    String ack = new String(ackPacket.getData(), 0, ackPacket.getLength());
                    if (ack.equals("ACK")) {
                        System.out.println("ACK received for packet starting at " + start);
                        break; // Exit the loop if ACK is received
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("Timeout waiting for ACK, retransmitting packet starting at " + start);
                }
            }
        } catch (Exception e) {
            System.err.println("Error sending packet: " + e.getMessage());
        }
    }

    // Send file to server
    public void sendFile(String fileName) throws Exception {
        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        int bytesRead;
        int offset = 0;
        while ((bytesRead = fis.read(buffer)) != -1) {
            sendPacketToServer(buffer, offset, bytesRead);
            offset += bytesRead;
        }
        fis.close();
    }

    // Send file name to server
    public void sendFileName(String fileName) throws Exception {
        byte[] buffer = fileName.getBytes();
        sendPacketToServer(buffer, 0, buffer.length);
    }

    public void start(String fileName) throws Exception {
        try {
            // Send the file name
            sendFileName(fileName);

            // send file
            sendFile(fileName);

            //start receiving packets
            sendPacketToServer("END".getBytes(),0,"END".getBytes().length);

        } catch (InterruptedException e) {
            System.err.println("Error during packet sending loop: "+ e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        String recvHost = null;
        int recvPort = 0;
        String fileName = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-r":
                    String[] hostPort = args[++i].split(":");
                    recvHost = hostPort[0];
                    recvPort = Integer.parseInt(hostPort[1]);
                    break;
                case "-f":
                    fileName = args[++i];
                    break;
                default:
                    System.err.println("Usage: java RUDPSource -r <recvHost>:<recvPort> -f <fileName>");
                    return;
            }
        }

        if (recvHost == null || recvPort == 0 || fileName == null) {
            System.err.println("Usage: java RUDPSource -r <recvHost>:<recvPort> -f <fileName>");
            return;
        }

        // Create the client with the server's IP address and port number
        RUDPSource client = new RUDPSource(recvHost, recvPort);

        // Start the client to send and receive packets
        client.start(fileName);
    }
}
