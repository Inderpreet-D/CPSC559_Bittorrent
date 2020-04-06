package Network.GlobalTracker;

import Network.NetworkStatics;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class HeartbeatThread<T extends Pulsable> extends Thread {

    private T gt;
    private boolean running;
    private DatagramSocket socket;

    HeartbeatThread(T gt) {
        this.gt = gt;
        this.running = false;
        try {
            this.socket = new DatagramSocket();
            this.socket.setSoTimeout(1500);
        } catch (SocketException se) {
            se.printStackTrace();
        }
    }

    @Override
    public void run() {
        this.running = true;
        while (this.running) {
            String[] nodes = this.gt.getConnectedNodes();
            for (String node : nodes) {
                System.out.println(">> Checking " + node);
                boolean alive = false;
                for (int i = 1; i <= 5; i++) {
                    try {
                        byte[] cmd = ByteBuffer.allocate(4).putInt(0).array();
                        DatagramPacket outPacket = new DatagramPacket(cmd, cmd.length, InetAddress.getByName(node), NetworkStatics.SERVER_CONTROL_RECEIVE);
                        socket.send(outPacket);

                        byte[] inMsg = new byte[NetworkStatics.MAX_PACKET_SIZE];
                        DatagramPacket inPacket = new DatagramPacket(inMsg, inMsg.length);
                        socket.receive(inPacket);
                        int inCmd = NetworkStatics.byteArrayToInt(Arrays.copyOfRange(inMsg, 0, 4));

                        if (inCmd == 1) {
                            alive = true;
                            System.out.println("<< Alive " + node);
                            break;
                        }
                    } catch (IOException ioe) {
                        System.out.println(String.format("!! Timeout %d %s", i, node));
                    }
                }
                if (!alive) {
                    System.out.println(">> Removed node " + node);
                    this.gt.deleteNode(node);
                }
            }
            try {
                System.out.println(">> Heartbeat sleeping");
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void finish() {
        this.running = false;
    }
}
