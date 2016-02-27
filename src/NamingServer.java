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
    static Map<Socket, ObjectOutputStream> mSocketTable = new Hashtable<>();

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
                new NamingServerHandlingThread(serverSocket.accept(), mClientTable, mSocketTable).start();
            }
        }
        serverSocket.close();
    }

    private static class NamingServerHandlingThread extends Thread{
        private Socket socket = null;
        Map clientMap = null;
        Map socketTable;
        public NamingServerHandlingThread(Socket socket, Map clientMap, Map socketTable){
            this.socket = socket;
            this.clientMap = clientMap;
            this.socketTable = socketTable;
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

                //new client
                Map newclientMap = new Hashtable<>();
                newclientMap.put(name, Ip);

                //broadcast to all except this socket about this new player
                for (Map.Entry<Socket, ObjectOutputStream> entry : mSocketTable.entrySet()) {
                    ObjectOutputStream oos  = entry.getValue();
                    oos.writeObject(new IpBroadCastPacket(newclientMap));
                }
                mSocketTable.put(socket, toClient);//add to broadcast list after broadcast

                //new client receive all other players' ip
                toClient.writeObject(new IpBroadCastPacket(clientMap));

                clientMap.put(name, Ip);//put new client after send all others


            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }


    }




}


