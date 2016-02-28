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

class PacketComparator implements Comparator<MPacket>{
    @Override
    public int compare(MPacket x, MPacket y){
        if(x.timestamp < y.timestamp){
            return -1;
        }
        else if(x.timestamp > y.timestamp){
            return 1;
        }
		else{
			return x.name.compareTo(y.name);
		}
    }
}


public class Mazewar extends JFrame {
    private MServerSocket serverSocket;
	private Queue<MPacket> receivedQueue;
	private Queue<MPacket> displayQueue;
    private Queue<MPacket> incomingQueue;
    protected List<String> localPlayers = null;
    private Map<String, IpLocation> neighbours;
    private Map<String, MSocket> socketsForBroadcast;
    protected AtomicInteger actionHoldingCount;
    protected AtomicInteger numberOfPlayers;
    protected AtomicInteger curTimeStamp;
    protected AtomicInteger sequenceNumber;
    private Hashtable<Integer,SenderPacketInfo> waitToResendQueue;
    private BlockingQueue<MPacket> confirmationQueue;
    private AvoidRepeatence avoidRepeatenceHelper;
    private long timeout;
    public String playerName;
    public AtomicBoolean  startRender;


    public void addNeighbours(String name, IpLocation neighbours) {
        this.neighbours.put(name, neighbours);
    }

    public void setNeighbours(Map<String, IpLocation> neighbours) {
        this.neighbours = neighbours;
    }
    public void set_neighbour_sockets_list_for_sender(Map<String, MSocket> newlist){
        socketsForBroadcast = newlist;
    }
    public void add_neighbour_socket_for_sender( String name, MSocket socket){
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
    private Mazewar mazewar = null;
    private MSocket mSocket = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

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
    private OverheadMazePanel overheadPanel = null;

    /**
     * The table the displays the scores.
     */
    private JTable scoreTable = null;

    /**
     * Create the textpane statically so that we can
     * write to it globally using
     * the static consolePrint methods
     */
    private static final JTextPane console = new JTextPane();

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


        System.exit(0);
    }

    String namingServerHost;
    int namingServerPort;
    int selfPort;
    /**
     * The place where all the pieces are put together.
     */
    public Mazewar(String namingServerHost, int namingServerPort, int selfPort) throws IOException,
            ClassNotFoundException, InterruptedException {
        super("ECE419 Mazewar");
        consolePrintLn("ECE419 Mazewar started!");

        /*
        * instantiate all of the needed structures
        * */
        this.namingServerHost = namingServerHost;
        this.namingServerPort = namingServerPort;
        this.selfPort = selfPort;
        this.waitToResendQueue = new Hashtable<Integer,SenderPacketInfo>();
        this.serverSocket = new MServerSocket(selfPort);
		this.receivedQueue = new PriorityBlockingQueue<MPacket>(50, new PacketComparator()) ;
		this.displayQueue = new LinkedBlockingQueue<MPacket>(50);
        this.incomingQueue =  new LinkedList<MPacket>();
        this.actionHoldingCount = new AtomicInteger(0);
        this.localPlayers = new LinkedList<String>();
        this.numberOfPlayers = new AtomicInteger(0);
        this.curTimeStamp = new AtomicInteger(0);
        this.sequenceNumber = new AtomicInteger(0);
        this.neighbours = new Hashtable<String, IpLocation>();
        this.socketsForBroadcast = new Hashtable<String, MSocket>();
        this.confirmationQueue= new LinkedBlockingQueue <MPacket>();
        this.timeout = 10000;
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

        //wait till naming server get the guiclient then start the display
        while (!startRender.get()) {
            wait(100000);
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
    private void startThreads() {

        new ServerSocketHandleThread(serverSocket, this, incomingQueue).start();

        //start naming server
        /* register the naming server*/

        IpPacket ipPacket = null;
        try {
            Socket toNamingServerSocket = new Socket(namingServerHost, namingServerPort);
            ipPacket = new IpPacket(playerName, InetAddress.getLocalHost().getHostName(), selfPort);
            ObjectOutputStream toNamingServer = new ObjectOutputStream(toNamingServerSocket.getOutputStream());
            toNamingServer.writeObject(ipPacket);
            new NamingServerListenerThread(toNamingServerSocket, this).start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Start a new sender thread
        new Thread(new ClientSenderThread(sequenceNumber,eventQueue, socketsForBroadcast, receivedQueue, curTimeStamp, waitToResendQueue)).start();
        //Start a new listener thread
        //new Thread(new ClientListenerThread(socketsForBroadcast, clientTable,receivedQueue,displayQueue, incomingQueue,actionHoldingCount)).start();
        new ConfirmationBroadcast(sequenceNumber, confirmationQueue, socketsForBroadcast, waitToResendQueue);
        new ResendThread(timeout,waitToResendQueue,socketsForBroadcast);

        new IncomingMessageHandleThread(incomingQueue, receivedQueue, waitToResendQueue, confirmationQueue,
                actionHoldingCount, socketsForBroadcast, curTimeStamp, avoidRepeatenceHelper, numberOfPlayers);
        new ReceivedThread(receivedQueue, displayQueue, curTimeStamp, socketsForBroadcast,
                localPlayers, actionHoldingCount);
        new DisplayThread(displayQueue, clientTable);



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

class ServerSocketHandleThread extends Thread{
    private MServerSocket serverSocket = null;
    Mazewar mazewarClient = null;
    Queue<MPacket> incomingQueue = null;

    public ServerSocketHandleThread(MServerSocket serverSocket, Mazewar mazewarClient, Queue<MPacket> incomingQueue) {
        this.serverSocket = serverSocket;
        this.mazewarClient = mazewarClient;
        this.incomingQueue = incomingQueue;
    }

    @Override
    public void run() {
        while(true){
            try {
                /*
                * start new listener for each new player connection request
                * */
                MSocket receivedSocket = serverSocket.accept();
                new Thread(new ClientListenerThread(incomingQueue, receivedSocket)).start();
                //mazewarClient.add_server_Neighbours_socket(receivedSocket);
            } catch (IOException e) {
                e.printStackTrace();
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
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            while (true) {
                IpBroadCastPacket result = (IpBroadCastPacket) objectInputStream.readObject();
                Map<String, IpLocation> clientTable = result.mClientTable;
                List<Player> players = result.players;

                //// TODO: 2016-02-28 how to dynamic add the player
                // Create the GUIClient and connect it to the KeyListener queue
                //RemoteClient remoteClient = null;
                for (Player player : players) {
                    if (player.name.equals(mazewarClient.playerName)) {
                        if (Debug.debug) System.out.println("Adding guiClient: " + player.toString());
                        //create new client for current player
                        mazewarClient.guiClient = new GUIClient(mazewarClient.playerName, mazewarClient.eventQueue, mazewarClient.actionHoldingCount);

                        //put for the local player list
                        mazewarClient.localPlayers.add(mazewarClient.guiClient.getName());

                        //register maze
                        mazewarClient.maze.addClientAt(mazewarClient.guiClient, player.point, player.direction);
                        mazewarClient.addKeyListener(mazewarClient.guiClient);
                        mazewarClient.clientTable.put(player.name, mazewarClient.guiClient);

                    } else {
                        if (Debug.debug) System.out.println("Adding remoteClient: " + player.toString());
                        RemoteClient remoteClient = new RemoteClient(player.name);
                        //register maze
                        mazewarClient.maze.addClientAt(remoteClient, player.point, player.direction);
                        mazewarClient.clientTable.put(player.name, remoteClient);
                    }
                }


                for (Map.Entry<String, IpLocation> e: clientTable.entrySet()){
                    System.out.println(e.getKey());
                    System.out.println(e.getValue().hostAddress);

                    mazewarClient.addNeighbours(e.getKey(), e.getValue());
                    mazewarClient.add_neighbour_socket_for_sender(e.getKey(), new MSocket((e.getValue()).hostAddress,(e.getValue()).port));
                }

                if(mazewarClient.numberOfPlayers.get() == 2){
                    mazewarClient.startRender.set(true);//can start to display
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
