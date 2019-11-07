//package testVersion;
//
//import java.net.DatagramPacket;
//import java.net.InetAddress;
//import java.net.MulticastSocket;
//import java.util.Arrays;
//import java.util.Scanner;
//
//public class RoadSideUnitThread extends Thread {
//
//    private final String GROUP_IP = "239.193.129.14";//224.0.0.0 ~ 239.255.255.255
//    private final Integer GROUP_PORT = 9876;
//    private InetAddress group;
//    private MulticastSocket socket;
//
//    private int type;//0发送 1接收
//
//    private RoadSideUnit roadSideUnit;
//
//    public RoadSideUnitThread(String name, int type) {
//        super(name);
//        this.type = type;
//    }
//
//    public static void main(String args[]) {
//        int myId = Integer.parseInt(args[0]);
//        String myName = args[1];
//        int groupId = Integer.parseInt(args[2]);
//        int myTransactionIndex = Integer.parseInt(args[3]);
//        String myBlockHash = args[4];
//        String privateKey = args[5];
//        String publicKey = args[6];
//        RoadSideUnit roadSideUnit = new RoadSideUnit(myId, myName, groupId, privateKey, publicKey, myBlockHash, myTransactionIndex);
//
//        RoadSideUnitThread thread1 = new RoadSideUnitThread("RSUSend_" + myId, 0);
//        thread1.setRoadSideUnit(roadSideUnit);
//        roadSideUnit.setRoadSideUnitCMDThread(thread1);
//        thread1.start();
//
//        RoadSideUnitBusinessThread businessThread = new RoadSideUnitBusinessThread("RSUBusiness_" + myId, roadSideUnit);
//        roadSideUnit.setRoadSideUnitBusinessThread(businessThread);
////        businessThread.start();
//
//        RoadSideUnitThread thread2 = new RoadSideUnitThread("RSUReceive_" + myId, 1);
//        thread2.setRoadSideUnit(roadSideUnit);
//        roadSideUnit.setRoadSideUnitReceiveThread(thread2);
//        thread2.start();
//    }
//
//    @Override
//    public void run() {
//        System.out.println("Thread: " + getName() + " START!!!");
//        try {
//            //创建多播组地址
//            group = InetAddress.getByName(GROUP_IP);
//            socket = createMulticastGroupAndJoin();
//
//            if (type == 0) {
//                while (true) {
//                    try {
//                        Scanner scanner = new Scanner(System.in);
//                        int cons = scanner.nextInt();
//                        System.out.println("Construction: " + cons);
//                        roadSideUnit.sendConstructions(cons);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//            if (type == 1) {
//                while (true) {
//                    try {
//                        receive();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private MulticastSocket createMulticastGroupAndJoin() throws Exception {
//        //创建多播组socket
//        MulticastSocket socket = new MulticastSocket(GROUP_PORT);
//        //加入多播组
//        socket.joinGroup(group);
//        socket.setTimeToLive(1);//设置组播数据报的发送范围为本地网络
//        socket.setLoopbackMode(false);//必须是false才能开启广播功能！！
//
//        return socket;
//    }
//
//    public void send(byte[] data) throws Exception {
//        //初始化DatagramPacket
//        DatagramPacket packet = new DatagramPacket(data, 0, data.length, group, GROUP_PORT);
//        socket.send(packet);
////        System.out.println("ID: " + roadSideUnit.getId() + ", send message: " + data);
//    }
//
//    private void receive() throws Exception {
//        //接收数据缓冲
//        byte[] buf = new byte[409600];
//        //初始化DatagramPacket
//        DatagramPacket datagramPacket = new DatagramPacket(buf, 0, buf.length, group, GROUP_PORT);
//        socket.receive(datagramPacket); //接收数据
//        byte[] data = Arrays.copyOf(buf, datagramPacket.getLength());
//        roadSideUnit.receiveFromOthers(data);
////        System.out.println("ID: " + roadSideUnit.getId() + ", receive message: " + data);
//    }
//
//    private void leaveGroupAndClose() throws Exception {
//        socket.leaveGroup(group);
//        socket.close();
//    }
//
//    public RoadSideUnit getRoadSideUnit() {
//        return roadSideUnit;
//    }
//
//    public void setRoadSideUnit(RoadSideUnit roadSideUnit) {
//        this.roadSideUnit = roadSideUnit;
//    }
//}
