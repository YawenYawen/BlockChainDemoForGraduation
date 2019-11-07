package testVersion;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

/**
 * 车辆作为服务器的TCP接受端
 */
public class VehicleTCPThread extends Thread {

    //自己作为服务器的ip
    private final String VEHICLE_IP = "192.168.1.103";
    private final Integer VEHICLE_PORT = 10005;
    private ServerSocket serverSocket;

    //对应的rsu的ip
    private final String RSU_IP = "192.168.1.105";
    private final Integer RSU_PORT = 10005;

    //0是发送线程 1是接受线程
    private int type;

    private Vehicle vehicle;

    public VehicleTCPThread(String name, int type) {
        super(name);
        this.type = type;
    }

    public static void main(String args[]) {
        //两个线程共用的vehicle
        int myId = Integer.parseInt(args[0]);
        String myName = args[1];
        int myTransactionIndex = Integer.parseInt(args[2]);
        String myBlockHash = args[3];
        String privateKey = args[4];
        String publicKey = args[5];
        Vehicle publicVehicle = new Vehicle(myId, myName, privateKey, publicKey, myBlockHash, myTransactionIndex);

        //发送线程
        VehicleTCPThread vehicleSendThread = new VehicleTCPThread("VehicleTCPSend_" + myId, 0);
        publicVehicle.setVehicleSendThread(vehicleSendThread);
        vehicleSendThread.setVehicle(publicVehicle);
//        vehicleSendThread.start();//命令行启动

        VehicleTCPThread vehicleReceiveThread = new VehicleTCPThread("VehicleTCPReceive_" + myId, 1);
        publicVehicle.setVehicleReceiveThread(vehicleReceiveThread);
        vehicleReceiveThread.setVehicle(publicVehicle);
        vehicleReceiveThread.start();

        VehicleTCPThread cmd = new VehicleTCPThread("VehicleTCPCMD_" + myId, 3);
        cmd.setVehicle(publicVehicle);
        cmd.start();
    }

    @Override
    public void run() {
        System.out.println("Thread: " + getName() + " START!!!");
        try {

            //之后永远有一个线程不断监听组播通信的请求
            if (type == 0) {
                //测试代码：车辆不断发送消息
                int WORD_SIZE = 10;
                Random random = new Random();
                String[] words = new String[4];
                while (true) {
                    try {
                        for (int i = 0; i < 4; i++) {
                            words[i] = String.valueOf((char) ('a' + random.nextInt(WORD_SIZE)));
                        }
                        vehicle.sendRequestToRSU(words);
                        Thread.sleep(500);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (type == 1) {
                serverSocket = new ServerSocket(VEHICLE_PORT);
                //测试代码：车辆不断接收消息
                while (true) {
                    try {
                        receive();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (type == 3) {
                //接收命令行消息
                Scanner scanner = new Scanner(System.in);
                while (true) {
                    try {
                        int cons = scanner.nextInt();
                        System.out.println("Construction: " + cons);
                        vehicle.sendConstructions(cons);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 作为TCP客户端需要向RSU请求
     *
     * @param data
     * @throws Exception
     */
    public byte[] send(byte[] data, int flag) throws Exception {
        //初始化DatagramPacket
        Socket socket = new Socket(RSU_IP, RSU_PORT);
        OutputStream os = socket.getOutputStream();//字节输出流
        os.write(data);
        os.flush();
        socket.shutdownOutput();

        //同时利用这个socket接受返回
        InputStream is = socket.getInputStream();
        byte[] buf = new byte[409600];
        int len = is.read(buf);
        byte[] result = null;
        if (len != -1) {
            result = Arrays.copyOf(buf, len);
            try {
                result = ExpressUtils.inflate(result);
            } catch (Exception e) {
                System.out.println("解压失败");
                e.printStackTrace();
                return null;
            }
        }

        socket.close();
        return result;
//        System.out.println("ID: " + vehicle.getId() + ", send message: " + data);
    }

    public void send(byte[] data) throws Exception {
        //初始化DatagramPacket
        Socket socket = new Socket(RSU_IP, RSU_PORT);
        OutputStream os = socket.getOutputStream();//字节输出流
        os.write(data);
        os.flush();
        socket.shutdownOutput();

        //同时利用这个socket接受返回
        receive(socket);
//        System.out.println("ID: " + vehicle.getId() + ", send message: " + data);
    }

    private void receive() throws Exception {
        Socket socket = serverSocket.accept();
        InputStream is = socket.getInputStream();
        byte[] buf = new byte[409600];
        int len = is.read(buf);
        byte[] data = Arrays.copyOf(buf, len);
        vehicle.receiveFromOthers(data);
//        System.out.println("ID: " + vehicle.getId() + ", receive message: " + data);
    }

    private void receive(Socket socket) throws Exception {
        InputStream is = socket.getInputStream();
        byte[] buf = new byte[409600];
        int len = is.read(buf);
        if (len != -1) {
            byte[] data = Arrays.copyOf(buf, len);
            vehicle.receiveFromOthers(data);
        }

        socket.close();
//        System.out.println("ID: " + vehicle.getId() + ", receive message: " + data);
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }
}
