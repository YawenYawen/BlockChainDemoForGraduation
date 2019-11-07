//package testVersion;
//
//import java.net.DatagramPacket;
//import java.net.InetAddress;
//import java.net.MulticastSocket;
//import java.util.Arrays;
//import java.util.Map;
//import java.util.Random;
//import java.util.Scanner;
//
//public class VehicleThread extends Thread {
//
//    private final String GROUP_IP = "239.193.129.14";//224.0.0.0 ~ 239.255.255.255
//    private final Integer GROUP_PORT = 9876;
//    private InetAddress group;
//    private MulticastSocket socket;
//
//    private Vehicle vehicle;
//
//    private int type;//0发送 1接收
//
//    public VehicleThread(String name, int type) {
//        super(name);
//        this.type = type;
//    }
//
//    public static void main(String args[]) {
//        //两个线程共用的vehicle
//        int myId = Integer.parseInt(args[0]);
//        String myName = args[1];
//        int myTransactionIndex = Integer.parseInt(args[2]);
//        String myBlockHash = args[3];
//        String privateKey = args[4];
//        String publicKey = args[5];
//        Vehicle publicVehicle = new Vehicle(myId, myName, privateKey, publicKey, myBlockHash, myTransactionIndex);
//
//        //发送线程
//        VehicleThread vehicleSendThread = new VehicleThread("VehicleSend_" + myId, 0);
//        publicVehicle.setVehicleSendThread(vehicleSendThread);
//        vehicleSendThread.setVehicle(publicVehicle);
////        vehicleSendThread.start();//命令行启动
//
//        VehicleThread vehicleReceiveThread = new VehicleThread("VehicleReceive_" + myId, 1);
//        publicVehicle.setVehicleReceiveThread(vehicleReceiveThread);
//        vehicleReceiveThread.setVehicle(publicVehicle);
//        vehicleReceiveThread.start();
//
//        VehicleThread cmd = new VehicleThread("VehicleCMD_" + myId, 3);
//        cmd.setVehicle(publicVehicle);
//        cmd.start();
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
//            //之后永远有一个线程不断监听组播通信的请求
//            if (type == 0) {
//                //测试代码：车辆不断发送消息
//                int WORD_SIZE = 10;
//                Random random = new Random();
//                String[] words = new String[4];
//                while (true) {
//                    try {
//                        for (int i = 0; i < 4; i++) {
//                            words[i] = String.valueOf((char) ('a' + random.nextInt(WORD_SIZE)));
//                        }
//                        vehicle.sendRequestToRSU(words);
//                        Thread.sleep(500);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//            if (type == 1) {
//                //测试代码：车辆不断接收消息
//                while (true) {
//                    try {
//                        receive();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//            if (type == 3) {
//                //接收命令行消息
//                Scanner scanner = new Scanner(System.in);
//                while (true) {
//                    try {
//                        int cons = scanner.nextInt();
//                        System.out.println("Construction: " + cons);
//                        vehicle.sendConstructions(cons);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
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
////        System.out.println("ID: " + vehicle.getId() + ", send message: " + data);
//    }
//
//    private void receive() throws Exception {
//        //接收数据缓冲
//        byte[] buf = new byte[409600];
//        //初始化DatagramPacket
//        DatagramPacket datagramPacket = new DatagramPacket(buf, 0, buf.length, group, GROUP_PORT);
//        socket.receive(datagramPacket); //接收数据
//        byte[] data = Arrays.copyOf(buf, datagramPacket.getLength());
//        vehicle.receiveFromOthers(data);
////        System.out.println("ID: " + vehicle.getId() + ", receive message: " + data);
//    }
//
//    private void leaveGroupAndClose() throws Exception {
//        socket.leaveGroup(group);
//        socket.close();
//    }
//
//    public Vehicle getVehicle() {
//        return vehicle;
//    }
//
//    public void setVehicle(Vehicle vehicle) {
//        this.vehicle = vehicle;
//    }
//}
