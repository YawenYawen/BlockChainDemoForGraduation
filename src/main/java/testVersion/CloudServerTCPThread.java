package testVersion;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

public class CloudServerTCPThread extends Thread {

    //自己的ip
    private final String CS_IP = "192.168.2.102";//224.0.0.0 ~ 239.255.255.255
    private final Integer CS_PORT = 10005;
    private ServerSocket serverSocket;

    private CloudServer cloudServer;

    private int type;//0发送 1接收


    public CloudServerTCPThread(String name, int type) {
        super(name);
        this.type = type;
    }

    public static void main(String args[]) {
        int myId = Integer.parseInt(args[0]);
        String myName = args[1];
        CloudServer cloudServer = new CloudServer(myId, myName);

        //用于发送数据
        CloudServerTCPThread thread1 = new CloudServerTCPThread("CloudServerSend_" + myId, 0);
        thread1.setCloudServer(cloudServer);
        cloudServer.setCloudServerSendThread(thread1);
        thread1.start();

        //用于接收命令行命令
        CloudServerTCPThread thread2 = new CloudServerTCPThread("CloudServerReceive_" + myId, 1);
        thread2.setCloudServer(cloudServer);
        cloudServer.setCloudServerReceiveThread(thread2);
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
                        cloudServer.sendConstructions(cons);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (type == 1) {
                serverSocket = new ServerSocket(CS_PORT);
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

    public void send(String ip, byte[] data) throws Exception {
        //初始化DatagramPacket
        Socket socket = new Socket(ip, 10005);
        OutputStream os = socket.getOutputStream();//字节输出流
        os.write(data);
        os.flush();
        socket.shutdownOutput();

        //这个函数只有云服务器主动发消息的时候才会调用，这个时候路侧节点可能有回复
        receive(socket);
//        System.out.println("ID: " + roadSideUnit.getId() + ", send message: " + data);
    }

    private void receive() throws Exception {
        Socket socket = serverSocket.accept();
        InputStream is = socket.getInputStream();
        byte[] buf = new byte[409600];
        int len = is.read(buf);
        byte[] data = Arrays.copyOf(buf, len);
        cloudServer.receiveFromOthers(socket, data);
//        System.out.println("ID: " + vehicle.getId() + ", receive message: " + data);
    }

    private void receive(Socket socket) throws Exception {
        InputStream is = socket.getInputStream();
        byte[] buf = new byte[409600];
        int len = is.read(buf);
        if (len != -1) {
            byte[] data = Arrays.copyOf(buf, len);
            cloudServer.receiveFromOthers(socket, data);
        }
//        System.out.println("ID: " + vehicle.getId() + ", receive message: " + data);
    }

    public void setCloudServer(CloudServer cloudServer) {
        this.cloudServer = cloudServer;
    }
}
