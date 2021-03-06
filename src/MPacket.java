import java.io.Serializable;
import java.util.*;

public class MPacket implements Serializable {

    /*The following are the type of events*/

    public static final int ACTION = 200;
	public static final int RECEIVED = 300;
	public static final int RELEASED = 400;
	public static final int CONFIRMATION = 500;
    public static final int QUITMESSAGE = 600;
    /*The following are the specific action 
    for each type*/


    /*Action*/
    public static final int UP = 201;
    public static final int DOWN = 202;
    public static final int LEFT = 203;
    public static final int RIGHT = 204;
    public static final int FIRE = 205;
    public static final int DIE = 206;
    public static final int MOVE_BULLET = 207;
    public static final int QUIT = 208;
    public static final int PACK = 209;
    
    
    //These fields characterize the event  
    public int type;
    public int event; 
    public List<MPacket> eventList;

    //The name determines the client that initiated the event
    public String name;
    
    //The sequence number of the event
    public int sequenceNumber;
    public int toAckNumber;
    public int toConfrimSequenceNumber;

    //These are used to initialize the board
    public Player[] players;

    //projectile
    //public int projectile;

	//
	public int timestamp;

    public MPacket(int type, int event){
        this.type = type;
        this.event = event;
    }
    
    public MPacket(String name, int type, int event){
        this.name = name;
        this.type = type;
        this.event = event;
    }

	public MPacket(String name, int type, int event, int timestamp){
        this.name = name;
        this.type = type;
        this.event = event;
		this.timestamp = timestamp;
    }
	
    public String toString(){
        String typeStr;
        String eventStr;
        
        switch(type){
            case 100:
                typeStr = "HELLO";
                break;
            case 200:
                typeStr = "ACTION";
                break;
			case 300:
				typeStr = "RECEIVED";
				break;
			case 400:
				typeStr = "RELEASED";
				break;
			case 500:
				typeStr = "CONFIRMATION";
				break;
            case 600:
                typeStr = "QUITMESSAGE";
            default:
                typeStr = "ERROR";
                break;        
        }
        switch(event){
            case 101:
                eventStr = "HELLO_INIT";
                break;
            case 102:
                eventStr = "HELLO_RESP";
                break;
            case 201:
                eventStr = "UP";
                break;
            case 202:
                eventStr = "DOWN";
                break;
            case 203:
                eventStr = "LEFT";
                break;
            case 204:
                eventStr = "RIGHT";
                break;
            case 205:
                eventStr = "FIRE";
                break;
            case 206:
                eventStr = "DIE";
                break;
            case 207:
                eventStr = "MOVE_BULLET";
                break;
            case 208:
                eventStr = "QUIT";
                break;
            case 209:
                eventStr = "PACK";
                break;
            case 0:
                eventStr = "DEFAULT";
                break;
            default:
                eventStr = "ERROR";
                break;        
        }
        //MPACKET(NAME: name, <typestr: eventStr>, SEQNUM: sequenceNumber)

        String retString = String.format("MPACKET(NAME: %s, <%s: %s>, SEQNUM: %s, ACK: %d, toConfirm: %d)", name,
            typeStr, eventStr, sequenceNumber, toAckNumber, toConfrimSequenceNumber);

        return retString;
    }

}
