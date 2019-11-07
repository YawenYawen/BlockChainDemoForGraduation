package testVersion;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;

/**
 * 专门处理车辆请求
 */
public class VehicleBusinessThread extends Thread {

    private Vehicle vehicle;

    private Map<String, Object> map;

    private ObjectMapper mapper = new ObjectMapper();

    public VehicleBusinessThread(String name, Vehicle vehicle, Map<String, Object> map) {
        super(name);
        this.vehicle = vehicle;
        this.map = map;
    }

    @Override
    public void run() {
//        while (true) {
        try {
            if (map != null && map.size() > 0) {
                String requestKey = (String) map.get("requestKey");
                String requestIndex = (String) map.get("requestIndex");
                long requestTime = Long.parseLong(requestIndex.substring((vehicle.getName() + vehicle.getId()).length()));
//                //TODO:车辆解析请求之后再写
                //记录间隔时间
                long current = System.currentTimeMillis();
                long interval = current - requestTime;
                vehicle.getResultMap().put("CAR_RESPONSE" + requestIndex, interval);
//                vehicle.receiveFromRSU(mapper.readValue((String) map.get("data"), ResponseForVehicle.class), (String) map.get("timestamp"));
                vehicle.receiveFromRSU(mapper.readValue((String) map.get("data"), SimpleResponseForVehicle.class), (String) map.get("timestamp"));
                //打印间隔时间
                System.out.println(interval + "/" + current + "/get response2:" + requestKey + ";" + requestIndex);
                if (vehicle.getResultMap().size() % 100 == 0) {//每100个就记录一下
                    ExperimentRecordUtils.printToExcel1(new ArrayList<>(vehicle.getResultMap().values()), "VehicleDelayed_" + current);
                }
                if (vehicle.getMinDisList().size() % 100 == 0) {//每100个就记录一下
                    ExperimentRecordUtils.printToExcel(vehicle.getMinDisList(), "VehicleMinDis_" + current);
                }
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
