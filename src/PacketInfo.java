/**
 * Created by jiayuhe on 2016-02-25.
 */
public class PacketInfo {
    public MPacket Packet;
    public int releasedNumber;
    public boolean isAck;
    public boolean isReleased;
    public boolean isConfirmed;
    public int confirmMsgSequenceNum;

    public PacketInfo(MPacket Packet){
        //intililization
        this.Packet = Packet;
        this.isAck = false;
        this.isReleased = false;
        this.isConfirmed = false;
        this.releasedNumber = 0;
        this.confirmMsgSequenceNum = 0;
    }
}
