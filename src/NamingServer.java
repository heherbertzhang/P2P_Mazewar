import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Created by herbert on 2016-02-04.
 */
public class NamingServer {
    static Map<String, IpLocation> mClientTable = new Hashtable<>();
    static Map<Socket, ObjectOutputStream> mSocketTable = new Hashtable<>();
    static List<Player> playerList = new LinkedList<>();
    static Random randomGen = new Random(Mazewar.mazeSeed);

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
        Map<Socket, ObjectOutputStream> socketTable;
        public NamingServerHandlingThread(Socket socket, Map clientMap, Map<Socket, ObjectOutputStream> socketTable){
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

                //Get a random location for player
                Point point =
                        new Point(randomGen.nextInt(Mazewar.mazeWidth),
                                randomGen.nextInt(Mazewar.mazeHeight));
                //Start them all facing North
                Player player = new Player(packet.hostName, point, Player.North);
                List newPlayer= new LinkedList<>();
                newPlayer.add(player);
                //broadcast to all except this socket about this new player
                for (Map.Entry<Socket, ObjectOutputStream> entry : mSocketTable.entrySet()) {
                    ObjectOutputStream oos  = entry.getValue();
                    oos.writeObject(new IpBroadCastPacket(newclientMap, newPlayer));
                }
                mSocketTable.put(socket, toClient);//add to broadcast list after broadcast

                //new client receive all other players' ip
                playerList.add(player);
                clientMap.put(name, Ip);//put new client before send all others since we need our self
                toClient.writeObject(new IpBroadCastPacket(clientMap, playerList));



            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }


    }

}


