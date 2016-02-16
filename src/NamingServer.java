import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by herbert on 2016-02-04.
 */
public class NamingServer {
    static Map<String, IpLocation> mClientTable = new Hashtable<>();


    public static void main(String[] args) throws IOException {
        boolean listening = true;
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(Integer.parseInt(args[0]));
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (listening){
            if (serverSocket != null) {
                new NamingServerHandlingThread(serverSocket.accept(), mClientTable).start();
            }
        }
        serverSocket.close();
    }

    private static class NamingServerHandlingThread extends Thread{
        private Socket socket = null;
        Map clientMap = null;

        public NamingServerHandlingThread(Socket socket, Map clientMap){
            this.socket = socket;
            this.clientMap = clientMap;
            System.out.println("new client accepted");
        }

        @Override
        public void run(){
            try {
                ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());

                IpPacket packet = (IpPacket) fromClient.readObject();
                while(packet == null){
                    packet = (IpPacket) fromClient.readObject();
                }
                String name = packet.hostName;
                IpLocation Ip = packet.Ip;
                clientMap.put(name, Ip);
                IpPacket reply = new IpPacket(200);
                toClient.writeObject(reply);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }


    }




}


