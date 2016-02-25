/**
 * Created by jiayuhe on 2016-02-25.
 */
public class PacketInfo {
    public MPacket Packet;
    public int releasedNumber;
    public boolean isAck;

    public PacketInfo(MPacket Packet){
        //intililization
        this.Packet = Packet;
        this.isAck = false;
        this.releasedNumber = 0;
    }
}
