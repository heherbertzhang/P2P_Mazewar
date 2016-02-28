/*
Copyright (C) 2004 Geoffrey Alan Washburn
    
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
    
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
    
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
USA.
*/

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An abstract class for {@link Client}s in a {@link Maze} that local to the 
 * computer the game is running upon. You may choose to implement some of 
 * your code for communicating with other implementations by overriding 
 * methods in {@link Client} here to intercept upcalls by {@link GUIClient} and 
 * {@link RobotClient} and generate the appropriate network events.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: LocalClient.java 343 2004-01-24 03:43:45Z geoffw $
 */


public abstract class LocalClient extends Client {

    /**
     * Create a {@link Client} local to this machine.
     */

    private BlockingQueue<MPacket> eventQueue = null;
    public AtomicInteger actionCount;

    public LocalClient(String name, BlockingQueue<MPacket> eventQueue, AtomicInteger actionCount) {
        super(name);
        this.eventQueue = eventQueue;
        this.actionCount = actionCount;
    }


    public void addActionEvent(int action) throws InterruptedException {
        actionCount.incrementAndGet();
        eventQueue.put(new MPacket(getName(), MPacket.ACTION, action));
    }

    public void sendKillClient(Player player, Player sourcePlayer) {
        try {
            MPacket packet = new MPacket(getName(), MPacket.ACTION, MPacket.DIE);
            //System.out.println(getName() + " die ==" + player.name);
            packet.players = new Player[2];
            packet.players[0] = player;//store the die info into the player and in packet
            packet.players[1] = sourcePlayer;
            eventQueue.put(packet);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    public void sendMoveProjectile(int prj) {
        MPacket packet = new MPacket(getName(), MPacket.ACTION, MPacket.MOVE_BULLET);
        //packet.projectile = prj;
        try {
            eventQueue.put(packet);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }

    }
}
