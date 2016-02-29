import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by herbert on 2016-02-25.
 */
public class IncomingMessageHandleThread extends Thread {

    private PriorityBlockingQueue receivedQueue = null;
    private AtomicInteger actionHoldingCount = null;
    private Map<String, MSocket> neighbousSockets = null;
    private LinkedBlockingQueue<MPacket> incomingQueue = null;
    private AtomicInteger currentTimeStamp = null;
    private Map<Integer, SenderPacketInfo> resendQueue = null;
    private AvoidRepeatence avoidRepeatenceHelper = null;
    private Queue<MPacket> confirmationQueue = null;
    private AtomicInteger numOfPlayer = null;//has to be Atomic for dynamic change of the players
    private String selfName = null;
    public IncomingMessageHandleThread(Queue<MPacket> incoming, Queue receivedQueue, Map<Integer, SenderPacketInfo> resendQueue,
                                       Queue<MPacket> confirmationQueue, AtomicInteger actionHoldingCount,
                                       Map<String, MSocket> neighbours_socket, AtomicInteger currentTimeStamp,
                                       AvoidRepeatence avoidRepeatence, AtomicInteger numOfPlayer, String selfName) {
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
    }

    public void run() {
        if (Debug.debug) System.out.println("Starting incoming queue handle thread");
        //start other threads
        //new ReceivedThread(receivedQueue, displayQueue, currentTimeStamp, neighbousSockets).start();
        //new DisplayThread(displayQueue, clientTable).start();
        try {
            while (true) {
                //get the head from incoming queue and then deals with it
                //check for the type of the message
                MPacket headMsg = null;
                while (headMsg == null) {
                    headMsg = incomingQueue.take();
                }
                //System.out.println("incoming message: " + headMsg.toString());
                switch (headMsg.type) {
                    case MPacket.ACTION:
                        //// TODO: 2016-02-27 to avoid bug the best we can do is to no check the action holding count
                        MPacket replyMsg = new MPacket(0, 0);
                        replyMsg.name = selfName;
                        replyMsg.sequenceNumber = headMsg.sequenceNumber;
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
                            //duplicate message need to check for the head of the queue to determine to resend ack or released msg
                            //find the message first
                            PacketInfo packetInfo = null;
                            while (packetInfo == null) {
                                packetInfo = (PacketInfo) receivedQueue.peek();
                                if(packetInfo == null) {
                                    System.out.println("something wrong since the received queue is null at this point");
                                }
                            }
                            if (packetInfo.Packet.sequenceNumber == headMsg.sequenceNumber) {
                                replyMsg.type = MPacket.RELEASED;
                            } else {
                                replyMsg.type = MPacket.RECEIVED;
                            }
                            //TODO: the following is more accurate method to check but may take more time?
                        /*for(Object packetInfo : receivedQueue){
                            if(((PacketInfo) packetInfo).Packet.sequenceNumber == headMsg.sequenceNumber){
                                if(((PacketInfo) packetInfo).isReleased){

                                }
                            }
                        }*/
                        }
                        MSocket mSocket = neighbousSockets.get(headMsg.name);
                        System.out.println("get ack's sender "+ headMsg.name);
                        mSocket.writeObject(replyMsg);
                        System.out.println("sending ack back");
                        //add to the received queue
                        if (!isDuplicated) {
                            System.out.println("not duplicated action msg:" + headMsg.toString());
                            //no repeatence so that we can add to the queue
                            PacketInfo packetInfo = new PacketInfo(headMsg);
                            packetInfo.isAck = true;
                            if (replyMsg.type == MPacket.RELEASED) {
                                packetInfo.isReleased = true;
                            }
                            receivedQueue.add(packetInfo);
                            System.out.println("added to received queue: " + headMsg.toString());
                        }
                        break;
                    case MPacket.RECEIVED:
                        //find from the wait to resend queue and then make it get one acknowledged from the player
                        SenderPacketInfo senderPacketInfo = resendQueue.get(headMsg.sequenceNumber);
                        if (senderPacketInfo != null) {
                            //check if already acked, do not increase lamport clock TODO
                            if (!senderPacketInfo.isAckedFrom(headMsg.name)) {
                                currentTimeStamp.set(Math.max(currentTimeStamp.get(), headMsg.timestamp) + 1);
                                senderPacketInfo.acknowledgeReceivedFrom(headMsg.name);
                            }
                        }
                        break;
                    case MPacket.RELEASED:
                        SenderPacketInfo senderPacketInfo2 = resendQueue.get(headMsg.sequenceNumber);
                        if (senderPacketInfo2 != null) {
                            //check if already released, if so do not increase lamport clock TODO
                            if (!senderPacketInfo2.isGotRleasedFrom(headMsg.name)) {
                                currentTimeStamp.set(Math.max(currentTimeStamp.get(), headMsg.timestamp)+1);
                                senderPacketInfo2.getReleasedFrom(headMsg.name);
                                if (senderPacketInfo2.getReleasedCount == numOfPlayer.get()) {

                                    senderPacketInfo2.releasedReceicedMap.clear();
                                    senderPacketInfo2.ackFromAll.clear();
                                    MPacket event = senderPacketInfo2.packet;
                                    System.out.println("adding to confirmation queue: " + event.toString());
                                    MPacket toConfirm = new MPacket(event.name, MPacket.CONFIRMATION, event.event
                                            /*no need to know event*/, currentTimeStamp.incrementAndGet());
                                    toConfirm.toConfrimSequenceNumber = event.sequenceNumber; //itself's sequence number will be determine by the confirmation thread
                                    confirmationQueue.add(toConfirm);
                                }
                            }

                        }
                        break;
                    case MPacket.CONFIRMATION:
                        //set the message to confirmed on the received queue by finding it first
                        //but will not remove it from the queue since only the head of the queue can be removed and
                        //add to the display queue

                        //send back ack first always!!!!!!
                        MPacket reply = new MPacket(0, 0);
                        reply.name = selfName;
                        reply.sequenceNumber = headMsg.sequenceNumber;
                        currentTimeStamp.set(Math.max(currentTimeStamp.get(), headMsg.timestamp) + 1);//update currentTimeStamp
                        reply.timestamp = currentTimeStamp.incrementAndGet();
                        reply.type = MPacket.RELEASED;//since remove the confirmation directly after received all
                        MSocket mSocket2 = neighbousSockets.get(headMsg.name);
                        mSocket2.writeObject(reply);

                        if (!avoidRepeatenceHelper.checkRepeatenceForProcess(headMsg.name, headMsg.sequenceNumber)) {
                            //not a duplicate message so we can do something
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
                        break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        catch (NullPointerException e){
            Thread.currentThread().interrupt();
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

    public ReceivedThread(Queue receivedQueue, Queue<MPacket> displayQueue, AtomicInteger curTimeStamp, Map<String, MSocket> neighbourSockets, List<String> localPlayers,
                          AtomicInteger actionHoldingCount, String selfName) {
        this.receivedQueue = (BlockingQueue) receivedQueue;
        this.displayQueue = displayQueue;
        this.currentTimeStamp = curTimeStamp;
        this.neighbourSockets = neighbourSockets;
        this.localPlayers = localPlayers;
        this.actionHoldingCount = actionHoldingCount;
        this.selfName = selfName;
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
                //if not released yet we need to release it when it's head and send back release message
                MPacket reply = new MPacket(0, 0);
                reply.name = selfName;
                reply.sequenceNumber = peek.Packet.sequenceNumber;
                reply.timestamp = currentTimeStamp.incrementAndGet();
                reply.type = MPacket.RELEASED;//since remove the confirmation directly after received all
                MSocket mSocket = neighbourSockets.get(peek.Packet.name);
                mSocket.writeObject(reply);
                System.out.println("sending release");
                peek.isReleased = true;
            }
            if (peek.isConfirmed) {
                //confrimed so we can remove the msg
                //remove and add to display queue
                PacketInfo removed = (PacketInfo) receivedQueue.poll();
                displayQueue.add(removed.Packet);
                System.out.println("adding to display queue: "+ removed.Packet.toString());

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
        if (Debug.debug) System.out.println("Starting display queue thread");
        Client client = null;
        try {
            while (true) {
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
            }
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }catch (NullPointerException e){
            Thread.currentThread().interrupt();
        }
    }
}