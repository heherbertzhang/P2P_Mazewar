import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by herbert on 2016-03-13.
 */
public class BulletSender extends Thread {
    private BlockingQueue eventQueue;
    static AtomicInteger bulletMoveCount = new AtomicInteger(0);;
    //sendMoveProjectile

    public BulletSender(BlockingQueue eventQueue) {
        this.eventQueue = eventQueue;
    }

    static public void sendNewBullet(){
        bulletMoveCount.set(12);
    }


    @Override
    public void run() {
        while (true){

            if(bulletMoveCount.get()>0){
                MPacket packet = new MPacket(getName(), MPacket.ACTION, MPacket.MOVE_BULLET);
                bulletMoveCount.decrementAndGet();
                eventQueue.add(packet);
                try {
                    sleep(80);
                } catch (InterruptedException e) {
                    System.out.println("debug:");
                    e.printStackTrace();
                }
            }
        }
    }
}