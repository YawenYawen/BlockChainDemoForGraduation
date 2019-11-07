import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Main2 {

    private static final String GROUP_IP = "239.193.129.14";//224.0.0.0 ~ 239.255.255.255
    private static final Integer GROUP_PORT = 9876;

    public static void main(String args[]) throws Exception {
        new Thread(new Runnable() {
            public void run() {
                try {
                    //创建多播组地址
                    InetAddress group = InetAddress.getByName(GROUP_IP);
                    MulticastSocket socket = createMulticastGroupAndJoin(group);
                    while (true) {
//                        send(socket, group, getTitle(socket) + "send a message to test messy code problem");
                        receive(socket, group);
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        },"Amy").start();
    }

    public static MulticastSocket createMulticastGroupAndJoin(InetAddress group) throws Exception {
        //创建多播组socket
        MulticastSocket socket = new MulticastSocket(GROUP_PORT);
        //加入多播组
        socket.joinGroup(group);
        socket.setTimeToLive(1);//设置组播数据报的发送范围为本地网络
        socket.setLoopbackMode(false);//必须是false才能开启广播功能！！

        return socket;
    }

    public static void send(MulticastSocket socket, InetAddress group, String data) throws Exception {
        //初始化DatagramPacket
        DatagramPacket packet = new DatagramPacket(data.getBytes(), 0, data.length(), group, GROUP_PORT);
        socket.send(packet);
        System.out.println(getTitle(socket) + "***发送数据:" + data);
    }

    public static void receive(MulticastSocket socket, InetAddress group) throws Exception {
        //接收数据缓冲
        byte[] buf = new byte[4096];
        //初始化DatagramPacket
        DatagramPacket datagramPacket = new DatagramPacket(buf, 0, buf.length, group, GROUP_PORT);
        socket.receive(datagramPacket); //接收数据
        System.out.println(System.currentTimeMillis() + "  " + getTitle(socket) + "^^^接收数据:" + new String(buf, 0, datagramPacket.getLength()));
    }

    public static void leaveGroupAndClose(MulticastSocket socket, InetAddress group) throws Exception {
        socket.leaveGroup(group);
        socket.close();
    }

    public static String getTitle(MulticastSocket socket) {
        return "Bob ";
    }
}
