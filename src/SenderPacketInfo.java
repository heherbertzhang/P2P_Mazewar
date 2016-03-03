import java.util.Hashtable;
import java.util.Map;

/**
 * Created by jiayuhe on 2016-02-27.
 */
public class SenderPacketInfo {
    public MPacket packet;
    public Map<String, Boolean> ackFromAll;
    public int getReleasedCount;
    public Map<String, Boolean> releasedReceicedMap;
    public long physicalTime;
    public SenderPacketInfo(Map<String, Boolean> All_neighbour, long physicalTime, MPacket packet){
        this.ackFromAll = All_neighbour;
        this.packet = packet;
        this.getReleasedCount = 0;
        this.physicalTime = physicalTime;
        this.releasedReceicedMap = new Hashtable<>(All_neighbour.size());
    }
    public boolean isAckedFrom(String name){
        return (ackFromAll.get(name) == null);
    }
    public void acknowledgeReceivedFrom(String name){
        ackFromAll.remove(name);
    }
    public void getReleasedFrom(String name){

        ackFromAll.remove(name);
        if(releasedReceicedMap.put(name, true)== null) {
            getReleasedCount += 1;
        }
    }

    public boolean isGotRleasedFrom(String name){
        return (releasedReceicedMap.get(name) != null);
    }

    public Map<String, Boolean> whoToResend(){
        return ackFromAll;
    }

    public MPacket whatToResend(){
        return packet;
    }
}
