import java.io.EOFException;
import java.io.IOException;
import java.util.*;

public class ClientListenerThread implements Runnable {

    private MSocket mSocket = null;
    private Queue<MPacket> incomingQueue = null;
    String name = null;
    public ClientListenerThread(Queue<MPacket> incomingQueue, MSocket mSocket) {

        this.incomingQueue = incomingQueue;
        this.mSocket = mSocket;

        //if (Debug.debug) System.out.println("Instatiating ClientListenerThread");
    }

    public void run() {

        if (Debug.debug) System.out.println("Starting ClientListenerThread");


        while (true) {
            try {


                MPacket received = (MPacket) mSocket.readObject();
                if(name == null){
                    name = received.name;
                }
                /*f(received.type == MPacket.QUITMESSAGE){
                    incomingQueue.add(received);
                    return;
                }*/

                //System.out.println("listening: " + received.toString());
                if (!incomingQueue.offer(received)) {
                    assert false;
                }

            } catch (IOException e) {
                System.out.println("debug: player crasshed, going to deals with it");
                Mazewar.reportCrash(name);
                Thread.currentThread().interrupt();
            } catch (ClassNotFoundException e) {
                System.out.println("debug: player crasshed, going to deals with it");
                e.printStackTrace();
                Mazewar.reportCrash(name);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.out.println("debug:");
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

    }
}
