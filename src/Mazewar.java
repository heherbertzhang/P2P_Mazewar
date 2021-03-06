/*
Copyright (C) 2004 Geoffrey Alan Washburn
   
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
   
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
   
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
USA.
*/

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTable;
import javax.swing.JOptionPane;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.BorderFactory;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The entry point and glue code for the game.  It also contains some helpful
 * global utility methods.
 *
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Mazewar.java 371 2004-02-10 21:55:32Z geoffw $
 */

class PacketINFOComparator implements Comparator<PacketInfo> {
    @Override
    public int compare(PacketInfo x, PacketInfo y) {
        if (x.Packet.timestamp < y.Packet.timestamp) {
            return -1;
        } else if (x.Packet.timestamp > y.Packet.timestamp) {
            return 1;
        } else {
            return x.Packet.name.compareTo(y.Packet.name);
        }
    }
}


public class Mazewar extends JFrame {
    public MServerSocket serverSocket;
    public Queue<PacketInfo> receivedQueue;
    public Queue<MPacket> displayQueue;
    public Queue<MPacket> incomingQueue;
    protected List<String> localPlayers = null;
    public Map<String, IpLocation> neighbours;
    public Map<String, MSocket> socketsForBroadcast;
    protected AtomicInteger actionHoldingCount;
    public static AtomicInteger numberOfPlayers;
    protected AtomicInteger curTimeStamp;
    protected AtomicInteger sequenceNumber;
    public Hashtable<Integer, SenderPacketInfo> waitToResendQueue;
    public BlockingQueue<MPacket> confirmationQueue;
    public AvoidRepeatence avoidRepeatenceHelper;
    public Map<String, Long> timeout;
    public AtomicInteger DeriRTT;
    public static String playerName;
    public AtomicBoolean startRender;

    static String namingServerHost;
    static int namingServerPort;
    static int selfPort;
    ObjectOutputStream toNamingServer;
    ObjectInputStream fromNamingServer;

    public void quit_player(String name) {
        numberOfPlayers.decrementAndGet();
        neighbours.remove(name);
        socketsForBroadcast.remove(name);
        waitToResendQueue.clear();
        /*
        for (Map.Entry<Integer, SenderPacketInfo> e : waitToResendQueue.entrySet()) {
            SenderPacketInfo waitingItem  = e.getValue();
            waitingItem.ackFromAll.remove(name);
            waitingItem.releasedReceicedMap.remove(name);
        }*/

    }

    public void remove_ClientTable(String name) {
        clientTable.remove(name);
    }

    public void addNeighbours(String name, IpLocation neighbours) {
        this.neighbours.put(name, neighbours);
    }

    public void setNeighbours(Map<String, IpLocation> neighbours) {
        this.neighbours = neighbours;
    }

    public void set_neighbour_sockets_list_for_sender(Map<String, MSocket> newlist) {
        socketsForBroadcast = newlist;
    }

    public void add_neighbour_socket_for_sender(String name, MSocket socket) {
        socketsForBroadcast.put(name, socket);
        numberOfPlayers.incrementAndGet(); // TODO: 2016-02-27 need to decrement when dynamic disconnect
        avoidRepeatenceHelper.addProccess(name);
    }


    /**
     * The default width of the {@link Maze}.
     */
    public static final int mazeWidth = 20;

    /**
     * The default height of the {@link Maze}.
     */
    public static final int mazeHeight = 10;

    /**
     * The default random seed for the {@link Maze}.
     * All implementations of the same protocol must use
     * the same seed value, or your mazes will be different.
     */
    public static final int mazeSeed = 42;

    /**
     * The {@link Maze} that the game uses.
     */
    protected Maze maze = null;

    /**
     * The Mazewar instance itself.
     */
    public Mazewar mazewar = null;
    public MSocket mSocket = null;
    public ObjectOutputStream out = null;
    public ObjectInputStream in = null;

    /**
     * The {@link GUIClient} for the game.
     */
    public GUIClient guiClient = null;


    /**
     * A map of {@link Client} clients to client name.
     */
    protected Hashtable<String, Client> clientTable = null;

    /**
     * A queue of events.
     */
    protected BlockingQueue<MPacket> eventQueue = null;

    /**
     * The panel that displays the {@link Maze}.
     */
    public OverheadMazePanel overheadPanel = null;

    /**
     * The table the displays the scores.
     */
    public JTable scoreTable = null;

    /**
     * Create the textpane statically so that we can
     * write to it globally using
     * the static consolePrint methods
     */
    public static final JTextPane console = new JTextPane();

    /**
     * Write a message to the console followed by a newline.
     *
     * @param msg The {@link String} to print.
     */
    public static synchronized void consolePrintLn(String msg) {
        console.setText(console.getText() + msg + "\n");
    }

    /**
     * Write a message to the console.
     *
     * @param msg The {@link String} to print.
     */
    public static synchronized void consolePrint(String msg) {
        console.setText(console.getText() + msg);
    }

    /**
     * Clear the console.
     */
    public static synchronized void clearConsole() {
        console.setText("");
    }


    /**
     * Static method for performing cleanup before exiting the game.
     */
    public static void quit() {
        // Put any network clean-up code you might have here.
        // (inform other implementations on the network that you have
        //  left, etc.)
        try {
            IpPacket QuitPackat = new IpPacket(true, playerName);
            Socket NamingServer = new Socket(namingServerHost, namingServerPort);
            ObjectOutputStream toNS = new ObjectOutputStream(NamingServer.getOutputStream()); // cannot use that already create one!
            System.out.println("I am writting to the outputstream");
            toNS.writeObject(QuitPackat);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static void reportCrash(String name) {
        // Put any network clean-up code you might have here.
        // (inform other implementations on the network that you have
        //  left, etc.)
        try {
            IpPacket QuitPackat = new IpPacket(true, name);
            Socket NamingServer = new Socket(namingServerHost, namingServerPort);
            ObjectOutputStream toNS = new ObjectOutputStream(NamingServer.getOutputStream()); // cannot use that already create one!
            System.out.println("I am writting to the naming server");
            toNS.writeObject(QuitPackat);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * The place where all the pieces are put together.
     */
    public Mazewar(String namingServerHost, int namingServerPort, int selfPort) throws IOException {
        super("ECE419 Mazewar");
        consolePrintLn("ECE419 Mazewar started!");

        /*
        * instantiate all of the needed structures
        * */
        this.namingServerHost = namingServerHost;
        this.namingServerPort = namingServerPort;
        this.selfPort = selfPort;
        this.waitToResendQueue = new Hashtable<Integer, SenderPacketInfo>();
        this.serverSocket = new MServerSocket(selfPort);
        this.receivedQueue = new PriorityBlockingQueue<PacketInfo>(100, new PacketINFOComparator());
        this.displayQueue = new LinkedBlockingQueue<MPacket>(50);
        this.incomingQueue = new LinkedBlockingQueue<MPacket>(100);
        this.actionHoldingCount = new AtomicInteger(0);
        this.localPlayers = new LinkedList<String>();
        this.numberOfPlayers = new AtomicInteger(1);
        this.curTimeStamp = new AtomicInteger(0);
        this.sequenceNumber = new AtomicInteger(0);
        this.neighbours = new Hashtable<String, IpLocation>();
        this.socketsForBroadcast = new Hashtable<String, MSocket>();
        this.confirmationQueue = new LinkedBlockingQueue<MPacket>();
        this.timeout = new Hashtable<String, Long>();
        this.DeriRTT = new AtomicInteger(5);
        this.avoidRepeatenceHelper = new AvoidRepeatence();
        //Initialize queue of events
        this.eventQueue = new LinkedBlockingQueue<MPacket>();
        //Initialize hash table of clients to client name
        this.clientTable = new Hashtable<String, Client>();
        this.startRender = new AtomicBoolean(false);
        // Create the maze
        maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed);
        assert (maze != null);

        // Have the ScoreTableModel listen to the maze to find
        // out how to adjust scores.
        ScoreTableModel scoreModel = new ScoreTableModel();
        assert (scoreModel != null);
        maze.addMazeListener(scoreModel);

        // Throw up a dialog to get the GUIClient name.
        String name = JOptionPane.showInputDialog("Enter your name");
        if ((name == null) || (name.length() == 0)) {
            Mazewar.quit();
        }
        //set the name
        playerName = name;


        //old code
        // mSocket = new MSocket(serverHost, serverPort);
        //Send hello packet to server
        //register the player to the server
        /*MPacket hello = new MPacket(name, MPacket.HELLO, MPacket.HELLO_INIT);
        hello.mazeWidth = mazeWidth;
        hello.mazeHeight = mazeHeight;

        if (Debug.debug) System.out.println("Sending hello");
        mSocket.writeObject(hello);
        if (Debug.debug) System.out.println("hello sent");
        
		//Receive response from server
        MPacket resp = (MPacket) mSocket.readObject();
        if (Debug.debug) System.out.println("Received response from server");
        */


        // Use braces to force constructors not to be called at the beginning of the
        // constructor.
                /*
                {
                        maze.addClient(new RobotClient("Norby"));
                        //localPlayer.add("Norby")
                        maze.addClient(new RobotClient("Robbie"));
                        maze.addClient(new RobotClient("Clango"));
                        maze.addClient(new RobotClient("Marvin"));
                }
                */


        //start naming server
        new ServerSocketHandleThread(serverSocket, this, incomingQueue).start(); //has to start this before naming server
        /* register the naming server*/

        IpPacket ipPacket = null;
        try {
            Random randomGen = new Random(Mazewar.mazeSeed);
            Point point = new Point(randomGen.nextInt(Mazewar.mazeWidth), randomGen.nextInt(Mazewar.mazeHeight));
            Player player = new Player(playerName, point, Player.North);
            Socket toNamingServerSocket = new Socket(namingServerHost, namingServerPort);
            ipPacket = new IpPacket(playerName, InetAddress.getLocalHost().getHostAddress(), selfPort, player);
            toNamingServer = new ObjectOutputStream(toNamingServerSocket.getOutputStream());
            fromNamingServer = new ObjectInputStream(toNamingServerSocket.getInputStream());
            toNamingServer.writeObject(ipPacket);
            new NamingServerListenerThread(toNamingServerSocket, this).start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //wait till naming server get the guiclient then start the display
        while (!startRender.get()) {

        }

        // Create the panel that will display the maze.
        overheadPanel = new OverheadMazePanel(maze, guiClient);
        assert (overheadPanel != null);
        maze.addMazeListener(overheadPanel);

        // Don't allow editing the console from the GUI
        console.setEditable(false);
        console.setFocusable(false);
        console.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));

        // Allow the console to scroll by putting it in a scrollpane
        JScrollPane consoleScrollPane = new JScrollPane(console);
        assert (consoleScrollPane != null);
        consoleScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Console"));

        // Create the score table
        scoreTable = new JTable(scoreModel);
        assert (scoreTable != null);
        scoreTable.setFocusable(false);
        scoreTable.setRowSelectionAllowed(false);

        // Allow the score table to scroll too.
        JScrollPane scoreScrollPane = new JScrollPane(scoreTable);
        assert (scoreScrollPane != null);
        scoreScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Scores"));

        // Create the layout manager
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        getContentPane().setLayout(layout);

        // Define the constraints on the components.
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 3.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        layout.setConstraints(overheadPanel, c);
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.weightx = 2.0;
        c.weighty = 1.0;
        layout.setConstraints(consoleScrollPane, c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        layout.setConstraints(scoreScrollPane, c);

        // Add the components
        getContentPane().add(overheadPanel);
        getContentPane().add(consoleScrollPane);
        getContentPane().add(scoreScrollPane);

        // Pack everything neatly.
        pack();

        // Let the magic begin.
        setVisible(true);
        overheadPanel.repaint();
        this.requestFocusInWindow();
    }

    /*
    *Starts the ClientSenderThread, which is
     responsible for sending events
     and the ClientListenerThread which is responsible for
     listening for events
    */
    public void startThreads() {

        //Start a new sender thread
        new Thread(new ClientSenderThread(sequenceNumber, eventQueue, socketsForBroadcast, incomingQueue, curTimeStamp, waitToResendQueue)).start();
        //Start a new listener thread
        //new Thread(new ClientListenerThread(socketsForBroadcast, clientTable,receivedQueue,displayQueue, incomingQueue,actionHoldingCount)).start();
        new ConfirmationBroadcast(sequenceNumber, confirmationQueue, socketsForBroadcast, waitToResendQueue, (BlockingQueue) incomingQueue).start();
        new ResendThread(150, timeout, waitToResendQueue, socketsForBroadcast).start();

        new IncomingMessageHandleThread(incomingQueue, receivedQueue, waitToResendQueue, confirmationQueue,
                actionHoldingCount, socketsForBroadcast, curTimeStamp, avoidRepeatenceHelper, numberOfPlayers, playerName, sequenceNumber, this).start();
        new ReceivedThread(receivedQueue, displayQueue, waitToResendQueue, incomingQueue, curTimeStamp, socketsForBroadcast,
                localPlayers, actionHoldingCount, playerName, sequenceNumber, numberOfPlayers).start();
        new DisplayThread(displayQueue, clientTable).start();
        new BulletSender(eventQueue).start();


    }


    /**
     * Entry point for the game.
     *
     * @param args Command-line arguments.
     */
    public static void main(String args[]) throws IOException,
            ClassNotFoundException {

        String namingServerhost = args[0];
        int namingServerPort = Integer.parseInt(args[1]);
        int selfport = Integer.parseInt(args[2]);
             /* Create the GUI */
        Mazewar mazewar = new Mazewar(namingServerhost, namingServerPort, selfport);
        mazewar.startThreads();

    }
}

class ServerSocketHandleThread extends Thread {
    public MServerSocket serverSocket = null;
    Mazewar mazewarClient = null;
    Queue<MPacket> incomingQueue = null;

    public ServerSocketHandleThread(MServerSocket serverSocket, Mazewar mazewarClient, Queue<MPacket> incomingQueue) {
        this.serverSocket = serverSocket;
        this.mazewarClient = mazewarClient;
        this.incomingQueue = incomingQueue;
    }

    @Override
    public void run() {
        System.out.println("starting server socket handle thread");
        while (true) {
            try {
                /*
                * start new listener for each new player connection request
                * */
                MSocket receivedSocket = serverSocket.accept();
                new Thread(new ClientListenerThread(incomingQueue, receivedSocket)).start();
                //mazewarClient.add_server_Neighbours_socket(receivedSocket);
            } catch (IOException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }
}

class NamingServerListenerThread extends Thread {
    Socket socket = null;
    Mazewar mazewarClient = null;

    NamingServerListenerThread(Socket socket, Mazewar mazewarClient) {
        this.socket = socket;
        this.mazewarClient = mazewarClient;
    }

    @Override
    public void run() {
        System.out.println("starting naming server listener thread");
        try {
            ObjectInputStream objectInputStream = mazewarClient.fromNamingServer;
            ObjectOutputStream objectOutputStream = mazewarClient.toNamingServer;

            while (true) {
                IpBroadCastPacket result = (IpBroadCastPacket) objectInputStream.readObject();
                System.out.println(result);
                if (result.isQuit == true) {
                    // this mean the client recieve itself remote clint's quitting message
                    Client quitClient = mazewarClient.clientTable.get(result.quitPlayer);
                    if(quitClient != null) {
                        System.out.println("test result: " + result.toString());

                        System.out.println("quit player is :" + result.quitPlayer);
                        System.out.println("Client table is" + mazewarClient.clientTable.toString());
                        System.out.println("quit clients is:" + quitClient.toString());
                        mazewarClient.quit_player(result.quitPlayer);
                        mazewarClient.maze.removeClient(quitClient);

                        System.out.println("maze now have #:" + mazewarClient.numberOfPlayers.get());
                        mazewarClient.remove_ClientTable(result.quitPlayer);
                        System.out.println("Unregister Maze of player!" + result.quitPlayer);
                    }
                }
                else if(result.type ==6){
                    //heart beat do nothing
                }
                else if (result.type == 1) {
                    //adding the client
                    Map<String, IpLocation> clientTable = result.mClientTable;
                    for (Map.Entry<String, IpLocation> e : clientTable.entrySet()) {
                        if (!e.getKey().equals(mazewarClient.playerName)) {
                            mazewarClient.addNeighbours(e.getKey(), e.getValue());
                            mazewarClient.add_neighbour_socket_for_sender(e.getKey(), new MSocket((e.getValue()).hostAddress, (e.getValue()).port));
                            System.out.println("add neighbour socket!");
                        }
                    }
                    List<Player> players = result.players;

                    //// TODO: 2016-02-28 how to dynamic add the player
                    // Create the GUIClient and connect it to the KeyListener queue
                    //RemoteClient remoteClient = null;
                    for (Player player : players) {
                        if (player.name.equals(mazewarClient.playerName)) {
                            System.out.println("Adding guiClient: " + player.toString());
                            //create new client for current player
                            mazewarClient.guiClient = new GUIClient(mazewarClient.playerName, mazewarClient.eventQueue, mazewarClient.actionHoldingCount);

                            //put for the local player list
                            mazewarClient.localPlayers.add(mazewarClient.guiClient.getName());

                            //register maze
                            mazewarClient.maze.addClientAt(mazewarClient.guiClient, player.point, player.direction);
                            mazewarClient.addKeyListener(mazewarClient.guiClient);
                            mazewarClient.clientTable.put(player.name, mazewarClient.guiClient);

                        } else {
                            System.out.println("Adding remoteClient: " + player.toString());
                            RemoteClient remoteClient = new RemoteClient(player.name);
                            //register maze
                            mazewarClient.maze.addClientAt(remoteClient, player.point, player.direction);
                            mazewarClient.clientTable.put(player.name, remoteClient);
                        }
                    }

                    /*
                    if (mazewarClient.numberOfPlayers.get() == 2) {
                        mazewarClient.startRender.set(true);//can start to display
                        System.out.println("start to render");
                    }*/
                    mazewarClient.startRender.set(true);
                } else if (result.type == 2){
                    // for existing players and want to add the new player into the meachine
                    //type is 2
                    Player player = result.players.get(0);
                    System.out.println("Adding remoteClient: " + player.toString());
                    RemoteClient remoteClient = new RemoteClient(player.name);
                    //register maze
                    mazewarClient.maze.addClientAt(remoteClient, player.point, player.direction);
                    mazewarClient.clientTable.put(player.name, remoteClient);

                    Map<String, IpLocation> clientTable = result.mClientTable;
                    for (Map.Entry<String, IpLocation> e : clientTable.entrySet()) {

                        mazewarClient.addNeighbours(e.getKey(), e.getValue());
                        mazewarClient.add_neighbour_socket_for_sender(e.getKey(), new MSocket((e.getValue()).hostAddress, (e.getValue()).port));
                        System.out.println("add neighbour socket!");

                    }
                    
                }
                else{
                    while (true){
                        if(!mazewarClient.displayQueue.isEmpty()){
                            System.out.println("display queue");
                            System.out.println(mazewarClient.displayQueue.peek().toString());
                        }
                        if(!mazewarClient.waitToResendQueue.isEmpty()){
                            System.out.println("resendqueue");
                            for(Map.Entry<Integer, SenderPacketInfo> e : mazewarClient.waitToResendQueue.entrySet()){
                                System.out.println(e.getKey() + " : " +e.getValue().packet.toString());
                            }
                            System.out.println("resend queueu end");
                        }
                        if (!mazewarClient.receivedQueue.isEmpty()){
                            System.out.println("received queue");
                            System.out.println(mazewarClient.receivedQueue.peek().toString());
                        }
                        if (mazewarClient.displayQueue.isEmpty() && mazewarClient.waitToResendQueue.isEmpty() && mazewarClient.receivedQueue.isEmpty()
                                && mazewarClient.incomingQueue.isEmpty()){

                            System.out.println("orientation!!!!!!:" + mazewarClient.guiClient.getOrientation().toString() + ": " + mazewarClient.guiClient.getOrientation().getDirection());
                            int direction;
                            if (mazewarClient.guiClient.getOrientation().getDirection() == 2 ){
                                direction = 1;
                            }
                            else if (mazewarClient.guiClient.getOrientation().getDirection() == 1){
                                direction = 2;
                            }
                            else{
                                direction =mazewarClient.guiClient.getOrientation().getDirection();
                            }
                            Player player = new Player(mazewarClient.guiClient.getName(), mazewarClient.guiClient.getPoint(), direction);
                            IpPacket toServer = new IpPacket("0","0",0,player);
                            System.out.println("about to reply to server");
                            objectOutputStream.writeObject(toServer);
                            System.out.println("reply to server succesfully");
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}
