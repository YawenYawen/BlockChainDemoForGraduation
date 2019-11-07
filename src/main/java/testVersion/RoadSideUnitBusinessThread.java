package testVersion;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.Socket;
import java.util.Map;

/**
 * 专门处理车辆请求
 */
public class RoadSideUnitBusinessThread extends Thread {

    private RoadSideUnit roadSideUnit;
    private Socket socket;

    private ObjectMapper mapper = new ObjectMapper();

    public RoadSideUnitBusinessThread(String name, RoadSideUnit roadSideUnit, Socket socket) {
        super(name);
        this.roadSideUnit = roadSideUnit;
        this.socket = socket;
    }

    @Override
    public void run() {
        int interval = 0;
//        while (true) {
        try {
            byte[] data = roadSideUnit.getCarTaskQueue().poll();
            if (data != null && data.length > 0) {
                Map<String, Object> map = mapper.readValue(data, Map.class);
                String requestIndex = (String) map.get("requestIndex");
                System.out.println(System.currentTimeMillis() + "/" + Thread.currentThread().getName() + "/receive car request:" + "CAR_REQUEST" + ";" + requestIndex);
//                    String encryptData = roadSideUnit.receiveVehicleRequest((String) map.get("cardBlockHash"), (String) map.get("cardBlockHash1"), (Integer) map.get("carTransactionIndex"), (String) map.get("carTransactionIndex1"), (String) map.get("keywordsVector"), (String) map.get("timestamp"));
                String encryptData = roadSideUnit.receiveVehicleRequest((String) map.get("cardBlockHash"), null, (Integer) map.get("carTransactionIndex"), null, (String) map.get("keywordsVector"), (String) map.get("timestamp"));
                roadSideUnit.sendRequestToVehicle(socket, "CAR_RESPONSE", requestIndex, encryptData);
                System.out.println(System.currentTimeMillis() + "/" + Thread.currentThread().getName() + "/send car response" + "CAR_REQUEST" + ";" + requestIndex);
                interval = 0;
            } else {
//                    Thread.sleep(5000 + interval * interval * 100);//否则就是个死循环,所以要控制防止CPU占用过高
//                Thread.sleep(5);//否则就是个死循环,所以要控制防止CPU占用过高
//                interval++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        }
    }
}
