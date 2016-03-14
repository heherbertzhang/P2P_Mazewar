import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by herbert on 2016-02-25.
 */
public class IncomingMessageHandleThread extends Thread {

    private PriorityBlockingQueue<PacketInfo> receivedQueue = null;
    private AtomicInteger actionHoldingCount = null;
    private Map<String, MSocket> neighbousSockets = null;
    private LinkedBlockingQueue<MPacket> incomingQueue = null;
    private AtomicInteger currentTimeStamp = null;
    private Map<Integer, SenderPacketInfo> resendQueue = null;
    private AvoidRepeatence avoidRepeatenceHelper = null;
    private Queue<MPacket> confirmationQueue = null;
    private AtomicInteger numOfPlayer = null;//has to be Atomic for dynamic change of the players
    private String selfName = null;
    private AtomicInteger curSequenceNum = null;
    private Mazewar mazewarAgent = null;

    public IncomingMessageHandleThread(Queue<MPacket> incoming, Queue<PacketInfo> receivedQueue, Map<Integer, SenderPacketInfo> resendQueue,
                                       Queue<MPacket> confirmationQueue, AtomicInteger actionHoldingCount,
                                       Map<String, MSocket> neighbours_socket, AtomicInteger currentTimeStamp,
                                       AvoidRepeatence avoidRepeatence, AtomicInteger numOfPlayer, String selfName, AtomicInteger sequenceNumber,
                                       Mazewar mazewarAgent) {
        this.receivedQueue = (PriorityBlockingQueue) receivedQueue;
        this.neighbousSockets = neighbours_socket;
        this.actionHoldingCount = actionHoldingCount;
        this.incomingQueue = (LinkedBlockingQueue<MPacket>) incoming;
        this.currentTimeStamp = currentTimeStamp;
        this.resendQueue = resendQueue;
        this.avoidRepeatenceHelper = avoidRepeatence;
        this.confirmationQueue = confirmationQueue;
        this.numOfPlayer = numOfPlayer;
        this.selfName = selfName;
        this.curSequenceNum = sequenceNumber;
        this.mazewarAgent = mazewarAgent;
    }

    public void run() {
        if (Debug.debug) System.out.println("Starting incoming queue handle thread :" + Thread.currentThread().getId());
        //start other threads
        //new ReceivedThread(receivedQueue, displayQueue, currentTimeStamp, neighbousSockets).start();
        //new DisplayThread(displayQueue, clientTable).start();

        while (true) {
            //get the head from incoming queue and then deals with it
            //check for the type of the message

            MPacket headMsg = null;
            try {
                headMsg = incomingQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("incoming message  : " + headMsg.toString());

            switch (headMsg.type) {
                case MPacket.ACTION:
                    //// TODO: 2016-02-27 to avoid bug the best we can do is to no check the action holding count
                    System.out.println("action incoming message: " + headMsg.toString());
                    if (headMsg.name.equals(selfName)) {
                        System.out.println("self action");
                        PacketInfo packetInfo = new PacketInfo(headMsg);
                        packetInfo.isAck = true;
                        packetInfo.isReleased = false;//cannot release yet!!!!!!!! must be head of received queue to release
                        //thats why we do not need fifo!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                        if (!receivedQueue.offer(packetInfo)) {
                            assert (1 == 0);
                        }
                        System.out.println("added to received queue directly: " + headMsg.toString());
                        //currentTimeStamp.set(Math.max(currentTimeStamp.get(), headMsg.timestamp) + 1);//update timestamp
                        //do not increase lamport clock as in lecture slides
                        continue;//change  to continue
                    }
                    MPacket replyMsg = new MPacket(0, 0);
                    replyMsg.name = selfName;
                    replyMsg.toAckNumber = headMsg.sequenceNumber;
                    //replyMsg.sequenceNumber = curSequenceNum.incrementAndGet();
                    currentTimeStamp.set(Math.max(currentTimeStamp.get(), headMsg.timestamp) + 1);//update currentTimeStamp
                    replyMsg.timestamp = currentTimeStamp.incrementAndGet();

                    boolean isDuplicated = avoidRepeatenceHelper.checkRepeatenceForProcess(headMsg.name, headMsg.sequenceNumber);
                    if (!isDuplicated) {

                        if (actionHoldingCount.get() == 0) {
                            //can send back release message
                            replyMsg.type = MPacket.RELEASED;

                        } else {
                            //send back ack message
                            replyMsg.type = MPacket.RECEIVED;
                        }
                    } else {
                        //duplicate message need to check for the head of the queue to determine to resend ack
                        //find the message first
                        //no need to release now since release must be received ensure by ack now

                        replyMsg.type = MPacket.RECEIVED;
                        //TODO: the following is more accurate method to check but may take more time and less internet trafffic?
                        /*for(Object receivedHeadPacketInfo : receivedQueue){
                            if(((PacketInfo) receivedHeadPacketInfo).Packet.sequenceNumber == headMsg.sequenceNumber){
                                if(((PacketInfo) receivedHeadPacketInfo).isReleased){
                                    continue;
                                }
                            }
                        }*/
                    }
                    MSocket mSocket = neighbousSockets.get(headMsg.name);
                    //System.out.println("get ack's sender "+ headMsg.name);


                    //System.out.println("sending ack back:" + replyMsg.toString());
                    //add to the received queue
                    if (!isDuplicated) {
                        currentTimeStamp.set(Math.max(currentTimeStamp.get(), headMsg.timestamp) + 1);//update timestamp
                        System.out.println("not duplicated action msg:" + headMsg.toString());
                        //no repeatence so that we can add to the queue
                        PacketInfo packetInfo = new PacketInfo(headMsg);
                        packetInfo.isAck = true;
                        if (replyMsg.type == MPacket.RELEASED) {
                            packetInfo.isReleased = true;

                            //add to resend queue for future use
                            replyMsg.sequenceNumber = curSequenceNum.incrementAndGet();
                            //Initlize the List for ack
                            Hashtable<String, Boolean> All_neighbour = new Hashtable<String, Boolean>();

                            All_neighbour.put(headMsg.name, false);

                            // Initlize time
                            long physicalTime = System.currentTimeMillis();
                            SenderPacketInfo info = new SenderPacketInfo(All_neighbour, physicalTime, replyMsg);
                            synchronized (resendQueue) {
                                resendQueue.put(replyMsg.sequenceNumber, info);//put to wait to resend queue
                            }
                        }

                        mSocket.writeObject(replyMsg); // write back reply after increment sequence number
                        System.out.println("sending reply message for action: " + replyMsg.toString() + " to " + headMsg.name);
                        receivedQueue.add(packetInfo);
                        //System.out.println("added to received queue: " + headMsg.toString());
                    }
                    break;
                case MPacket.RECEIVED:
                    //System.out.println("type is received!!!!!!!!!:" + headMsg.toString());
                    //find from the wait to resend queue and then make it get one acknowledged from the player
                    SenderPacketInfo senderPacketInfo = resendQueue.get(headMsg.toAckNumber);
                    if (senderPacketInfo != null) {
                        if (senderPacketInfo.packet.type == MPacket.RELEASED) {
                            //since release message only send to one we can safely remove it
                            synchronized (resendQueue) {
                                resendQueue.remove(headMsg.toAckNumber);
                                //System.out.println("!!!!!!!!!!!!!!!!!remove release");
                            }
                        }
                        //check if already acked, do not increase lamport clock TODO
                        else if (!senderPacketInfo.isAckedFrom(headMsg.name)) { //ACTION and confirmation and
                            currentTimeStamp.set(Math.max(currentTimeStamp.get(), headMsg.timestamp) + 1);
                            senderPacketInfo.acknowledgeReceivedFrom(headMsg.name);
                            if (senderPacketInfo.packet.type != MPacket.ACTION &&
                                    senderPacketInfo.ackFromAll.isEmpty()) {
                                synchronized (resendQueue) {
                                    resendQueue.remove(headMsg.toAckNumber);
                                }

                            }
                        }
                    }
                    break;

                case MPacket.RELEASED:
                    //send back ack
                    if(!headMsg.name.equals(selfName)) {
                        sendBackAck(headMsg, MPacket.RECEIVED);
                    }

                    SenderPacketInfo senderPacketInfo2 = resendQueue.get(headMsg.toAckNumber);

                    if (senderPacketInfo2 != null) {
                        //check if already released, if so do not increase lamport clock TODO!!!!? shoule we????
                        //System.out.println("now have released1: " + senderPacketInfo2.getReleasedCount);
                        if (!senderPacketInfo2.isGotRleasedFrom(headMsg.name)) {
                            //currentTimeStamp.set(Math.max(currentTimeStamp.get(), headMsg.timestamp) + 1);
                            senderPacketInfo2.getReleasedFrom(headMsg.name);
                            System.out.println("now have released: " + senderPacketInfo2.getReleasedCount);
                            synchronized (senderPacketInfo2) {
                                if (senderPacketInfo2.getReleasedCount == numOfPlayer.get()) {

                                    synchronized (resendQueue) {
                                        //remove from the resendqueue
                                        System.out.println("remove resend queue");
                                        resendQueue.remove(headMsg.toAckNumber);
                                    }

                                    //if (senderPacketInfo2.packet.type == MPacket.ACTION) {
                                    MPacket event = senderPacketInfo2.packet;
                                    //System.out.println("adding to confirmation queue: " + event.toString());
                                    MPacket toConfirm = new MPacket(event.name, MPacket.CONFIRMATION, event.event
                                            /*no need to know event*/, currentTimeStamp.incrementAndGet());
                                    toConfirm.toConfrimSequenceNumber = event.sequenceNumber; //itself's sequence number will be determine by the confirmation thread
                                    confirmationQueue.add(toConfirm);
                                    //}
                                }
                            }
                        }

                    }
                    break;
                case MPacket.CONFIRMATION:
                    //set the message to confirmed on the received queue by finding it first
                    //but will not remove it from the queue since only the head of the queue can be removed and
                    //add to the display queue
                    System.out.println("confirmation incoming:" + headMsg.toString() + " confirm " + headMsg.toConfrimSequenceNumber);
                    if (headMsg.name.equals(selfName)) {
                        //check to see if we can remove the confirmation msg from the resend queue
                        SenderPacketInfo senderPacketInfo3 = resendQueue.get(headMsg.sequenceNumber);
                        if(senderPacketInfo3 != null) {
                            if (senderPacketInfo3.getReleasedCount == numOfPlayer.get()) {

                                synchronized (resendQueue) {
                                    //remove from the resendqueue
                                    System.out.println("remove resend queue when self confirm");
                                    resendQueue.remove(headMsg.toAckNumber);
                                }
                            }
                        }
                        setConfirmed(headMsg);
                        break;
                    }
                    //send back ack first always!!!!!!

                    // TODO: 2016-03-02 cinfirmation does not need to compare time so no need lamport clock!? or it broadcast so need?
                    //currentTimeStamp.set(Math.max(currentTimeStamp.get(), headMsg.timestamp) + 1);//update currentTimeStamp
                    sendBackAck(headMsg, MPacket.RECEIVED);

                    if (!avoidRepeatenceHelper.checkRepeatenceForProcess(headMsg.name, headMsg.sequenceNumber)) {
                        //not a duplicate message so we can do something
                        setConfirmed(headMsg);
                    }
                    break;
            }
        }

    }

    public void sendBackAck(MPacket headMsg, int type) {
        System.out.println("release msg replying the headmsg is : " + headMsg.toString());
        MSocket socket = neighbousSockets.get(headMsg.name);
        MPacket replymsg = new MPacket(type, 0);
        replymsg.name = selfName;
        assert headMsg.sequenceNumber != 0;

        replymsg.toAckNumber = headMsg.sequenceNumber;
        if (type == MPacket.RELEASED) {
            replymsg.sequenceNumber = curSequenceNum.incrementAndGet();
        }
        replymsg.timestamp = currentTimeStamp.incrementAndGet();
        socket.writeObject(replymsg);
        System.out.println("reply to release:" + replymsg.toString());
    }

    public void setConfirmed(MPacket headMsg) {
        for (Object p : receivedQueue) {
            if (((PacketInfo) p).Packet.name.equals(headMsg.name) &&
                    ((PacketInfo) p).Packet.sequenceNumber == headMsg.toConfrimSequenceNumber) {
                ((PacketInfo) p).confirmMsgSequenceNum = headMsg.sequenceNumber;
                ((PacketInfo) p).isConfirmed = true;
                System.out.println("set confirmed for:" + ((PacketInfo) p).Packet.toString());
                break;
            }
        }
    }

}

class ReceivedThread extends Thread {
    BlockingQueue receivedQueue = null;
    Queue<MPacket> displayQueue = null;
    AtomicInteger currentTimeStamp = null;
    Map<String, MSocket> neighbourSockets = null;
    List<String> localPlayers = null;
    AtomicInteger actionHoldingCount = null;
    String selfName;
    AtomicInteger curSequenceNum = null;
    Map resendQueue;
    AtomicInteger numOfPlayers = null;
    Queue incomingQueue= null;

    public ReceivedThread(Queue receivedQueue, Queue<MPacket> displayQueue, Map resendQueue, Queue incomingQueue, AtomicInteger curTimeStamp, Map<String, MSocket> neighbourSockets, List<String> localPlayers,
                          AtomicInteger actionHoldingCount, String selfName, AtomicInteger curSequenceNum, AtomicInteger numOfPlayers) {
        this.receivedQueue = (BlockingQueue) receivedQueue;
        this.displayQueue = displayQueue;
        this.currentTimeStamp = curTimeStamp;
        this.neighbourSockets = neighbourSockets;
        this.localPlayers = localPlayers;
        this.actionHoldingCount = actionHoldingCount;
        this.selfName = selfName;
        this.curSequenceNum = curSequenceNum;
        this.resendQueue = resendQueue;
        this.numOfPlayers = numOfPlayers;
        this.incomingQueue = incomingQueue;
    }

    @Override
    public void run() {
        //get the head of received queue and check whether it is released or not
        //if it is released then don't do anything otherwise send back release message
        if (Debug.debug) System.out.println("Starting received queue thread: "  + Thread.currentThread().getId());
        while (true) {
            PacketInfo peek = (PacketInfo) receivedQueue.peek();
            if (peek == null) {
                continue;
            }

            if (!peek.isReleased) {
                //if not released yet we need to release it when it's head and send back release message

                if(peek.Packet.name.equals(selfName)){
                    //send release to itself: !!!!!!!

                    MPacket reply = new MPacket(MPacket.RELEASED, 0);
                    reply.name = selfName;
                    reply.toAckNumber = peek.Packet.sequenceNumber;
                    reply.sequenceNumber = curSequenceNum.incrementAndGet();

                    incomingQueue.add(reply);
                    peek.isReleased = true;
                    /*
                    SenderPacketInfo senderPacketInfo = (SenderPacketInfo) resendQueue.get(peek.Packet.sequenceNumber);
                    if(senderPacketInfo != null) {

                        senderPacketInfo.getReleasedFrom(selfName);
                        System.out.println("r thread now have released:" + senderPacketInfo.getReleasedCount);
                        synchronized (senderPacketInfo) {
                            if (senderPacketInfo.getReleasedCount == numOfPlayers.get()) {

                                synchronized (resendQueue) {
                                    //remove from the resendqueue
                                    resendQueue.remove(peek.Packet.toAckNumber);
                                }
                            }

                        }
                    }*/
                }
                else {
                    MPacket reply = new MPacket(0, 0);
                    reply.name = selfName;
                    reply.toAckNumber = peek.Packet.sequenceNumber;
                    reply.sequenceNumber = curSequenceNum.incrementAndGet();
                    reply.type = MPacket.RELEASED;//since remove the confirmation directly after received all

                    MSocket mSocket = neighbourSockets.get(peek.Packet.name);
                    mSocket.writeObject(reply);
                    System.out.println("sending release at head!!!!!" + reply.toString() + " reply to " + peek.Packet.toString());


                    peek.isReleased = true;
                    System.out.println("received queue head : " + peek.Packet.toString() + " isreleased " + peek.isReleased);

                    //add to resend queue for future use
                    reply.sequenceNumber = curSequenceNum.incrementAndGet();
                    //Initlize the List for ack
                    Hashtable<String, Boolean> All_neighbour = new Hashtable<String, Boolean>();

                    All_neighbour.put(peek.Packet.name, false);

                    // Initlize time
                    long physicalTime = System.currentTimeMillis();
                    SenderPacketInfo info = new SenderPacketInfo(All_neighbour, physicalTime, reply);
                    synchronized (resendQueue) {
                        resendQueue.put(reply.sequenceNumber, info);//put to wait to resend queue
                    }
                }
            }
            if (peek.isConfirmed) {
                //confrimed so we can remove the msg
                //remove and add to display queue
                PacketInfo removed = (PacketInfo) receivedQueue.poll();
                displayQueue.add(removed.Packet);
                System.out.println("adding to display queue: " + removed.Packet.toString());

                //decrease the action holding count
                if (localPlayers.contains(removed.Packet.name)) {
                    actionHoldingCount.decrementAndGet();
                }
            }
        }

    }
}


class DisplayThread extends Thread {
    private BlockingQueue<MPacket> displayQueue;
    private Map<String, Client> clientTable;

    public DisplayThread(Queue<MPacket> displayQueue, Map<String, Client> clientTable) {
        this.displayQueue = (BlockingQueue<MPacket>) displayQueue;
        this.clientTable = clientTable;
    }

    public void run() {
        if (Debug.debug) System.out.println("Starting display queue thread: "  + Thread.currentThread().getId());
        Client client = null;

        while (true) {
            try {
                MPacket poll = null;
                while (poll == null) {
                    poll = displayQueue.take();
                }

                if (Debug.debug) System.out.println("ready to take action !: " + poll.toString());
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
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            } catch (NullPointerException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

    }
}