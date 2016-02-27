import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by herbert on 2016-02-25.
 */
public class IncomingMessageHandleThread extends Thread {

    private Hashtable<String, Client> clientTable = null;
    private Queue receivedQueue = null;
    private AtomicInteger actionHoldingCount = null;
    private Map<String, MSocket> neighbousSockets = null;
    private Queue<MPacket> incomingQueue = null;
    private AtomicInteger currentTimeStamp = null;

    public IncomingMessageHandleThread(Queue<MPacket> incoming, Queue receivedQueue, AtomicInteger actionHoldingCount, Map<String, MSocket> neighbours_socket, AtomicInteger currentTimeStamp) {
        this.receivedQueue = receivedQueue;
        this.neighbousSockets = neighbours_socket;
        this.actionHoldingCount = actionHoldingCount;
        this.incomingQueue = incoming;
        this.currentTimeStamp = currentTimeStamp;
    }

    public void run() {
        if (Debug.debug) System.out.println("Starting incoming queue handle thread");
        //start other threads
        //new ReceivedThread(receivedQueue, displayQueue, currentTimeStamp, neighbousSockets).start();
        //new DisplayThread(displayQueue, clientTable).start();

        while (true) {
            //get the head from incoming queue and then deals with it
            //check for the type of the message
            MPacket headMsg = incomingQueue.poll();
            switch (headMsg.type) {
                case MPacket.ACTION:
                    MPacket replyMsg = new MPacket(0, 0);
                    replyMsg.sequenceNumber = headMsg.sequenceNumber;
                    replyMsg.timestamp = Math.max(currentTimeStamp.get(), headMsg.timestamp) + 1;
                    if (actionHoldingCount.get() == 0) {
                        //can send back release message
                        replyMsg.type = MPacket.RELEASED;

                    } else {
                        //send back ack message
                        replyMsg.type = MPacket.RECEIVED;
                    }
                    MSocket mSocket = neighbousSockets.get(headMsg.name);
                    mSocket.writeObject(replyMsg);
                    //add to the received queue
                    PacketInfo packetInfo = new PacketInfo(headMsg);
                    packetInfo.isAck = true;
                    if (replyMsg.type == MPacket.RELEASED) {
                        packetInfo.isReleased = true;
                    }
                    receivedQueue.add(packetInfo);

                    break;
                case MPacket.RECEIVED://TODO
                    break;
                case MPacket.RELEASED://TODO
                    break;
                case MPacket.CONFIRMATION:
                    //set the message to confirmed on the received queue by finding it first
                    //but will not remove it from the queue since only the head of the queue can be removed and
                    //add to the display queue
                    for (Object p : receivedQueue) {
                        if (((PacketInfo) p).Packet.name.equals(headMsg.name) &&
                                ((PacketInfo) p).Packet.sequenceNumber == headMsg.toConfrimSequenceNumber) {
                            ((PacketInfo) p).confirmMsgSequenceNum = headMsg.sequenceNumber;
                            ((PacketInfo) p).isConfirmed = true;
                            break;
                        }
                    }
                    break;
            }
        }
    }

}

class ReceivedThread extends Thread {
    Queue receivedQueue = null;
    Queue<MPacket> displayQueue = null;
    AtomicInteger currentTimeStamp = null;
    Map<String, MSocket> neighbourSockets = null;
    List<String> localPlayers = null;
    AtomicInteger actionHoldingCount = null;

    public ReceivedThread(Queue receivedQueue, Queue<MPacket> displayQueue, AtomicInteger curTimeStamp, Map<String, MSocket> neighbourSockets, List<String> localPlayers,
                          AtomicInteger actionHoldingCount) {
        this.receivedQueue = receivedQueue;
        this.displayQueue = displayQueue;
        this.currentTimeStamp = curTimeStamp;
        this.neighbourSockets = neighbourSockets;
        this.localPlayers = localPlayers;
        this.actionHoldingCount = actionHoldingCount;
    }

    @Override
    public void run() {
        //get the head of received queue and check whether it is released or not
        //if it is released then don't do anything otherwise send back release message
        if (Debug.debug) System.out.println("Starting received queue thread");
        while (true) {
            PacketInfo peek = (PacketInfo) receivedQueue.peek();
            if (peek == null) {
                continue;
            }
            if (!peek.isReleased) {
                MPacket replyMsg = new MPacket(MPacket.RELEASED, 0);
                replyMsg.sequenceNumber = peek.Packet.sequenceNumber;
                replyMsg.timestamp = Math.max(currentTimeStamp.get(), peek.Packet.timestamp) + 1;
                MSocket mSocket = neighbourSockets.get(peek.Packet.name);
                mSocket.writeObject(replyMsg);
            }
            if (peek.isConfirmed) {
                //confrimed so we can remove the msg

                MPacket replyMsg = new MPacket(MPacket.RECEIVED, 0);
                replyMsg.sequenceNumber = peek.confirmMsgSequenceNum;
                replyMsg.timestamp = Math.max(currentTimeStamp.get(), peek.Packet.timestamp) + 1;
                MSocket mSocket = neighbourSockets.get(peek.Packet.name);
                mSocket.writeObject(replyMsg);

                //remove and add to display queue
                PacketInfo removed = (PacketInfo) receivedQueue.poll();
                displayQueue.add(removed.Packet);

                //decrease the action holding count
                if (localPlayers.contains(removed.Packet.name)) {
                    actionHoldingCount.decrementAndGet();
                }
            }
        }
    }
}


class DisplayThread extends Thread {
    private Queue<MPacket> displayQueue;
    private Map<String, Client> clientTable;

    public DisplayThread(Queue<MPacket> displayQueue, Map<String, Client> clientTable) {
        this.displayQueue = displayQueue;
        this.clientTable = clientTable;
    }

    public void run() {
        if (Debug.debug) System.out.println("Starting display queue thread");
        Client client = null;
        while (true) {
            MPacket poll = displayQueue.poll();


            if (Debug.debug) System.out.println("ready to take action");
            client = clientTable.get(poll.name);
            if (poll.event == MPacket.UP) {
                client.forward();
            } else if (poll.event == MPacket.DOWN) {
                client.backup();
            } else if (poll.event == MPacket.LEFT) {
                client.turnLeft();
            } else if (poll.event == MPacket.RIGHT) {
                client.turnRight();
            } else if (poll.event == MPacket.FIRE) {
                System.out.println(client.getName() + " about to call fire()");
                client.fire();
            } else if (poll.event == MPacket.DIE) {
                Player newPosition = poll.players[0];
                Client sourceClient = clientTable.get(poll.players[1].name);
                //Client destClient = clientTable.get(newPosition.name);
                client.die(sourceClient, newPosition);
            } else if (poll.event == MPacket.MOVE_BULLET) {
                //int prj = received.projectile;
                String prj = poll.name;
                client.bullet_move(prj);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }
}