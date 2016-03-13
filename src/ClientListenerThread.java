import java.io.EOFException;
import java.io.IOException;
import java.util.*;

public class ClientListenerThread implements Runnable {

    private MSocket mSocket = null;
    private Queue<MPacket> incomingQueue = null;

    public ClientListenerThread(Queue<MPacket> incomingQueue, MSocket mSocket) {

        this.incomingQueue = incomingQueue;
        this.mSocket = mSocket;

        if (Debug.debug) System.out.println("Instatiating ClientListenerThread");
    }

    public void run() {

        if (Debug.debug) System.out.println("Starting ClientListenerThread");

        while (true) {
            try {


                MPacket received = (MPacket) mSocket.readObject();
                /*f(received.type == MPacket.QUITMESSAGE){
                    incomingQueue.add(received);
                    return;
                }*/

                //System.out.println("listening: " + received.toString());
                if (!incomingQueue.offer(received)) {
                    assert false;
                }

            } catch (IOException e) {
                Thread.currentThread().interrupt();
            } catch (ClassNotFoundException e) {
                System.out.println("debug:");
                e.printStackTrace();
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.out.println("debug:");
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

    }
}
