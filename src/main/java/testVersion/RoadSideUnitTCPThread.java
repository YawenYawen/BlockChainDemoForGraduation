package testVersion;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

public class RoadSideUnitTCPThread extends Thread {

    //自己作为服务器的ip
    private final String RSU_IP = "192.168.2.102";
    private final Integer RSU_PORT = 10005;
    private ServerSocket serverSocket;

    private int type;//0发送 1接收

    private RoadSideUnit roadSideUnit;

    public RoadSideUnitTCPThread(String name, int type) {
        super(name);
        this.type = type;
    }

    public static void main(String args[]) {
        int myId = Integer.parseInt(args[0]);
        String myName = args[1];
        int groupId = Integer.parseInt(args[2]);
        int myTransactionIndex = Integer.parseInt(args[3]);
        String myBlockHash = args[4];
        String privateKey = args[5];
        String publicKey = args[6];
        RoadSideUnit roadSideUnit = new RoadSideUnit(myId, myName, groupId, privateKey, publicKey, myBlockHash, myTransactionIndex);

        //同命令行交互的线程
        RoadSideUnitTCPThread thread1 = new RoadSideUnitTCPThread("RSUSend_" + myId, 0);
        thread1.setRoadSideUnit(roadSideUnit);
        roadSideUnit.setRoadSideUnitCMDThread(thread1);
        thread1.start();

        //作为服务器的线程，专门用于接受tcp连接
        RoadSideUnitTCPThread thread2 = new RoadSideUnitTCPThread("RSUReceive_" + myId, 1);
        thread2.setRoadSideUnit(roadSideUnit);
        thread2.start();
    }

    @Override
    public void run() {
        System.out.println("Thread: " + getName() + " START!!!");
        try {
            if (type == 0) {
                while (true) {
                    try {
                        Scanner scanner = new Scanner(System.in);
                        int cons = scanner.nextInt();
                        System.out.println("Construction: " + cons);
                        roadSideUnit.sendConstructions(cons);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (type == 1) {
                //本线程作为服务器不断接受车辆请求，但是交给其他线程处理
                serverSocket = new ServerSocket(RSU_PORT);
                while (true) {
                    try {
                        receive();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void send(Socket socket, byte[] data) throws Exception {
        OutputStream os = socket.getOutputStream();//字节输出流
        os.write(data);
        os.flush();
        socket.close();
//        System.out.println("ID: " + roadSideUnit.getId() + ", send message: " + data);
    }

    public Socket send(String ip, byte[] data) throws Exception {
        //初始化DatagramPacket
        Socket socket = new Socket(ip, 10005);
        OutputStream os = socket.getOutputStream();//字节输出流
        os.write(data);
        os.flush();
        socket.shutdownOutput();

        return socket;
//        System.out.println("ID: " + roadSideUnit.getId() + ", send message: " + data);
    }

    public void receive() throws Exception {
        //接收数据缓冲
        Socket socket = serverSocket.accept();
        InputStream is = socket.getInputStream();
        byte[] buf = new byte[409600];
        int len = is.read(buf);
        byte[] data = Arrays.copyOf(buf, len);
        roadSideUnit.receiveFromOthers(socket, data);
//        System.out.println("ID: " + vehicle.getId() + ", receive message: " + data);
    }

    public void receive(Socket socket) throws Exception {
        InputStream is = socket.getInputStream();
        byte[] buf = new byte[409600];
        int len = is.read(buf);
        if (len != -1) {
            byte[] data = Arrays.copyOf(buf, len);
            roadSideUnit.receiveFromOthers(socket, data);
        }
//        System.out.println("ID: " + vehicle.getId() + ", receive message: " + data);
    }

    public RoadSideUnit getRoadSideUnit() {
        return roadSideUnit;
    }

    public void setRoadSideUnit(RoadSideUnit roadSideUnit) {
        this.roadSideUnit = roadSideUnit;
    }
}
