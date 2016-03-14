

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Created by herbert on 2016-02-04.
 */
class InOut {
    ObjectOutputStream oos;
    ObjectInputStream ooi;

    public InOut(ObjectOutputStream oos, ObjectInputStream ooi) {
        this.oos = oos;
        this.ooi = ooi;
    }
}

public class NamingServer {
    static Map<String, IpLocation> mClientTable = new Hashtable<>();
    static Map<Socket, InOut> mSocketTable = new Hashtable<>();
    static Map<String, Socket> nameSocketTable = new Hashtable<>();
    static Random randomGen = new Random(Mazewar.mazeSeed);

    public static void main(String[] args) throws IOException {

        boolean listening = true;
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(Integer.parseInt(args[0]));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Naming server running at: " + InetAddress.getLocalHost().getHostAddress());
        while (listening) {
            if (serverSocket != null) {
                new NamingServerHandlingThread(serverSocket.accept(), mClientTable, mSocketTable, nameSocketTable).start();
            }
        }
        serverSocket.close();
    }

    private static class NamingServerHandlingThread extends Thread {
        private Socket socket = null;
        Map clientMap = null;
        Map<Socket, InOut> socketTable;
        Map<String, Socket> nameSocketTable;

        public NamingServerHandlingThread(Socket socket, Map clientMap, Map<Socket, InOut> socketTable, Map<String, Socket> nameSocketTable) {
            this.socket = socket;
            this.clientMap = clientMap;
            this.socketTable = socketTable;
            this.nameSocketTable = nameSocketTable;
            System.out.println("new client accepted");
        }

        @Override
        public void run() {

            try {
                ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());

                IpPacket packet = (IpPacket) fromClient.readObject();
                while (packet == null) {
                    packet = (IpPacket) fromClient.readObject();
                }

                if (packet.isQuit == true) {
                    System.out.println("I received a quitting message");
                    // mean someone told server that it need to quit
                    clientMap.remove(packet.quitPlayer);
                    Socket quitClintSocket = nameSocketTable.get(packet.quitPlayer);
                    socketTable.remove(quitClintSocket);
                    nameSocketTable.remove(packet.quitPlayer);

                    //broadcast to everyone
                    for (Map.Entry<Socket, InOut> entry : mSocketTable.entrySet()) {
                        //System.out.println("socket: " + socket.toString());
                        ObjectOutputStream oos = entry.getValue().oos;
                        oos.writeObject(new IpBroadCastPacket(true, packet.quitPlayer));
                    }
                } else {
                    // receive someone join
                    String name = packet.hostName;
                    IpLocation Ip = packet.Ip;
                    Player player = packet.player;

                    //new client
                    Map newclientMap = new Hashtable<>();
                    newclientMap.put(name, Ip);

                    //Get a random location for player

                    Point point =
                            new Point(randomGen.nextInt(Mazewar.mazeWidth),
                                    randomGen.nextInt(Mazewar.mazeHeight));
                    //Start them all facing North
                    player.point = point;


                    //broadcast to all except this socket about this new player
                    System.out.println("st size : " + mSocketTable.size());
                    for (Map.Entry<Socket, InOut> entry : mSocketTable.entrySet()) {
                        System.out.println("socket: " + socket.toString());
                        ObjectOutputStream oos = entry.getValue().oos;
                        oos.writeObject(new IpBroadCastPacket(-1));
                    }

                    List<Player> playerList = new LinkedList<>();

                    //waiting for all other exiting players acks and get their location
                    System.out.println("ready to wait");
                    for (Map.Entry<Socket, InOut> entry : mSocketTable.entrySet()) {
                        Socket socket = entry.getKey();
                        ObjectInputStream inputStream = entry.getValue().ooi;
                        System.out.println("wait for client");
                        IpPacket acks = (IpPacket) inputStream.readObject();
                        System.out.println("get the wait response");
                        playerList.add(acks.player);
                    }


                    boolean Error = false;
                    while (true) {
                        Error = false;
                        for (Player p : playerList) {
                            if (p.point.checkEqual(player.point)) {
                                player.point = new Point(randomGen.nextInt(Mazewar.mazeWidth),
                                        randomGen.nextInt(Mazewar.mazeHeight));
                                Error = true;
                            }
                        }
                        if (!Error) {
                            break;
                        }
                    }

                    List newPlayer = new LinkedList<>();
                    newPlayer.add(player);
                    //broadcast to all except this socket about this new player
                    System.out.println("st size : " + mSocketTable.size());
                    for (Map.Entry<Socket, InOut> entry : mSocketTable.entrySet()) {
                        System.out.println("socket: " + socket.toString());
                        ObjectOutputStream oos = entry.getValue().oos;
                        oos.writeObject(new IpBroadCastPacket(newclientMap, newPlayer, 2));
                    }

                    mSocketTable.put(socket, new InOut(toClient, fromClient));//add to broadcast list after broadcast

                    playerList.add(player);


                    //new client receive all other players' ip
                    clientMap.put(name, Ip);//put new client before send all others since we need our self
                    nameSocketTable.put(name, socket);
                    System.out.println("cm size: " + clientMap.size());
                    toClient.writeObject(new IpBroadCastPacket(clientMap, playerList, 1));
                    //fromClient.close();
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }


    }

}


