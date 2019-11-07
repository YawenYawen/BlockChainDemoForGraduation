package testVersion;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 两种链：身份信息链和数据链，且都有各自的区块头链
 */
public class Vehicle {

    private final Logger log = Logger.getLogger("Vehicle");

    //打印路径
    private final String ROOT_PATH = this.getClass().getResource("").getPath();
    //文件格式
    private final String SUFFIX = ".json";

    //下面的属性都与线程相同
    private Integer id;
    private String name;
    private String privateKey;
    private String publicKey;

    //自己的身份信息记录在哪里
    private String myBlockHash;//身份信息区块链hash值
    private Integer myTransactionIndex;//身份区块交易索引

    //数据验证链的区块头链:key是区块头，value是merkle树根
    private Map<String, String> dataVerifyHeadList = new HashMap<>();

    //身份信息验证链的区块头链
    private Map<String, String> identityVerifyHeadList = new HashMap<>();

    //每次请求的key和结果
    //没收到回复就是null，收到后放入间隔时间
    private Map<String, Long> resultMap = new HashMap<>();

    //路侧节点的发送线程池
    private ExecutorService pool = new ThreadPoolExecutor(4, 4, 1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());
    private static int threadIndex = 0;//路侧节点的发送线程池的编号

//
//    //车辆处理路侧节点回复的任务队列
//    private ConcurrentLinkedQueue<Map<String, Object>> rsuResponseQueue = new ConcurrentLinkedQueue<>();
//    //车辆处理路侧节点回复的线程池
//    private ExecutorService pool = new ThreadPoolExecutor(5, 8, 1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());
//    private static int threadIndex = 0;//车辆处理路侧节点回复的线程的编号


    private VehicleTCPThread vehicleSendThread;
    private VehicleTCPThread vehicleReceiveThread;

    private ObjectMapper mapper = new ObjectMapper();

    private static List<Double> minDisList = new LinkedList<>();

    //车辆本地会缓存一部分常用的路侧节点的公钥，原因是车辆通过相同路段的可能性很大
    //路侧节点id-路侧节点公钥
    private static Map<Long, String> rsuPublicKeyMap = new LinkedHashMap<>();

    public Vehicle(Integer id, String name, String privateKey, String publicKey, String myBlockHash, Integer myTransactionIndex) {
        this.id = id;
        this.name = name;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.myBlockHash = myBlockHash;
        this.myTransactionIndex = myTransactionIndex;

        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
    }


    /**
     * 接受命令行命令
     *
     * @param ins
     * @throws Exception
     */
    public void sendConstructions(int ins) throws Exception {
        switch (ins) {
            case -1://读出区块
                deserializeBlocks();
                System.out.println("-1 finished");
                break;
            case 0://打印区块，同时序列化区块
                printBlocks();
                serializeBlocks();
                System.out.println("0 finished");
                break;
            case 1://打印自己
                System.out.println(id);
                System.out.println(name);
                System.out.println(myBlockHash);
                System.out.println(publicKey);
                System.out.println(privateKey);
                System.out.println(myTransactionIndex);
                break;
            case 2://启动发送线程
                vehicleSendThread.run();
                System.out.println("2 finished");
                break;
            case 3://终止发送线程
                vehicleSendThread.stop();
                System.out.println("3 finished");
                break;
        }
    }

    private void printBlocks() throws Exception {
        System.out.println("Print Data Verify Blocks---------");
        List<String> list = new ArrayList<>(dataVerifyHeadList.size());
        for (Map.Entry block : dataVerifyHeadList.entrySet()) {
            list.add(mapper.writeValueAsString(block));
        }
        FileUtils.printToFileByline(ROOT_PATH + "/dataVerifyBlocks" + SUFFIX, list);

        System.out.println("Print Identity Verify Blocks---------");
        list = new ArrayList<>(identityVerifyHeadList.size());
        for (Map.Entry block : identityVerifyHeadList.entrySet()) {
            list.add(mapper.writeValueAsString(block));
        }
        FileUtils.printToFileByline(ROOT_PATH + "/identityVerifyBlocks" + SUFFIX, list);
    }

    /**
     * 将区块序列化写入文件
     *
     * @throws Exception
     */
    private void serializeBlocks() throws Exception {
        FileUtils.serialization(dataVerifyHeadList, ROOT_PATH + "/serial1");
        FileUtils.serialization(identityVerifyHeadList, ROOT_PATH + "/serial2");
        System.out.println("serialize blocks finished");
    }

    /**
     * 序列化区块读出来
     *
     * @throws Exception
     */
    private void deserializeBlocks() throws Exception {
        dataVerifyHeadList = (Map) FileUtils.deserialization(ROOT_PATH + "/serial1");
        identityVerifyHeadList = (Map) FileUtils.deserialization(ROOT_PATH + "/serial2");
        System.out.println("deserialize blocks finished");
    }

    /**
     * 请求数据
     *
     * @param words
     * @throws Exception
     */
    public void sendRequestToRSU(String[] words) throws Exception {
        String requestKey = "CAR_REQUEST";
        String requestIndex = name + id + System.currentTimeMillis();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("cardBlockHash", myBlockHash);
//        requestMap.put("cardBlockHash1", EncryptUtils.encryptByPrivateKey(privateKey, myBlockHash));
        requestMap.put("carTransactionIndex", myTransactionIndex);
//        requestMap.put("carTransactionIndex1", EncryptUtils.encryptByPrivateKey(privateKey, myTransactionIndex + ""));

        requestMap.put("requestKey", requestKey);
        requestMap.put("requestIndex", requestIndex);

        //由于加密的长度限制故只能分开加密
//        requestMap.put("keywordsVector", EncryptUtils.encryptByPrivateKey(privateKey, mapper.writeValueAsString(words)));
        requestMap.put("keywordsVector", mapper.writeValueAsString(words));
        requestMap.put("timestamp", EncryptUtils.encryptByPrivateKey(privateKey, System.currentTimeMillis() + ""));

        System.out.println(System.currentTimeMillis() + "/send request:" + requestKey + ";" + requestIndex);

        //放进去一个null值
        resultMap.put("CAR_RESPONSE" + requestIndex, null);

        //要压缩
        vehicleSendThread.send(ExpressUtils.deflate(mapper.writeValueAsBytes(requestMap)));

    }

    public void sendIdentityRequestToRSU() throws Exception {
        String requestKey = "CAR_IDENTITY_REQUEST";
        String requestIndex = name + id + System.currentTimeMillis();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("requestKey", requestKey);
        requestMap.put("requestIndex", requestIndex);
        requestMap.put("cardBlockHash", myBlockHash);
        requestMap.put("carTransactionIndex", myTransactionIndex);
        requestMap.put("timestamp", EncryptUtils.encryptByPublicKey(publicKey, System.currentTimeMillis() + ""));
//
        //要压缩
        byte[] result = vehicleSendThread.send(ExpressUtils.deflate(mapper.writeValueAsBytes(requestMap)), 1);
        try {
            result = ExpressUtils.inflate(result);
        } catch (Exception e) {
            System.out.println("解压失败");
            e.printStackTrace();
            return;
        }
        Map<String, Object> map = mapper.readValue(result, Map.class);

        IdentityResponseForVehicle response = mapper.readValue(mapper.writeValueAsString(map.get("data")), IdentityResponseForVehicle.class);
        checkRSUIdentity(response.getPublicKey(), response.getIdentityBlockHash(), response.getIdentityBlockMerkleRoot(), response.getIdentityBlockMerklePath(), response.getIdentityVerifyBlockHash(), response.getIdentityVerifyBlockMerklePath());
        rsuPublicKeyMap.put(Long.parseLong(response.getRsuId()), response.getPublicKey());
    }

    /**
     * 用requestKey来判断是哪种请求
     * 用requestIndex来判断是哪次请求
     *
     * @param data
     * @throws Exception
     */
    public void receiveFromOthers(byte[] data) throws Exception {
        try {
            data = ExpressUtils.inflate(data);
        } catch (Exception e) {
            System.out.println("解压失败");
            e.printStackTrace();
            return;
        }


        Map<String, Object> map = mapper.readValue(data, Map.class);
        String requestKey = (String) map.get("requestKey");
        String requestIndex = (String) map.get("requestIndex");

        switch (requestKey) {
            case "CAR_RESPONSE"://路侧节点回复
//                long current = System.currentTimeMillis();
                if (!resultMap.containsKey("CAR_RESPONSE" + requestIndex) || resultMap.get("CAR_RESPONSE" + requestIndex) != null) {
                    //不是给自己的 或者 已经收到了其他回复 那就忽略
                    return;
                }
                //不做复杂检查了错误就跳过
                if (!map.containsKey("data") || map.get("data") == null || StringUtils.isBlank((String) map.get("data"))) {
                    return;
                }
                pool.execute(new VehicleBusinessThread("vehicle business thread " + (threadIndex++), this, map));
//                long requestTime = Long.parseLong(requestIndex.substring((name + id).length()));
//                //TODO:车辆解析请求之后再写
//                //记录间隔时间
//                long interval = current - requestTime;
//                resultMap.put("CAR_RESPONSE" + requestIndex, interval);
//                receiveFromRSU(mapper.readValue((String) map.get("data"), ResponseForVehicle.class), (String) map.get("timestamp"));
//                //打印间隔时间
//                System.out.println(interval + "/" + current + "/get response2:" + requestKey + ";" + requestIndex);
//                if (resultMap.size() % 100 == 0) {//每100个就记录一下
//                    ExperimentRecordUtils.printToExcel1(new ArrayList<>(resultMap.values()), "VehicleDelayed_" + current);
//                }
//                if (minDisList.size() % 100 == 0) {//每100个就记录一下
//                    ExperimentRecordUtils.printToExcel(minDisList, "VehicleMinDis_" + current);
//                }
                break;
            case "VEHICLE_BLOCK_HEAD"://云服务器分配头
                System.out.println(System.currentTimeMillis() + "/receive cs block start");

                dataVerifyHeadList = ((ArrayList<Map>) map.get("data")).get(0);
                identityVerifyHeadList = ((ArrayList<Map>) map.get("data")).get(1);

                System.out.println(System.currentTimeMillis() + "/receive cs block end");

                break;
        }
    }

    public void receiveFromRSU(ResponseForVehicle responseForVehicle, String timestamp) throws Exception {
        String rsuPublicKey = responseForVehicle.getPublicKey();

        //时间验证
        long time = Long.parseLong(EncryptUtils.decryptByPublicKey(rsuPublicKey, timestamp));
        if (System.currentTimeMillis() - time > 20000) {//TODO:这个时间
            throw new Exception("回复超时");
        }

        //身份验证
        checkRSUIdentity(rsuPublicKey, responseForVehicle.getIdentityBlockHash(), responseForVehicle.getIdentityBlockMerkleRoot(), responseForVehicle.getIdentityBlockMerklePath(), responseForVehicle.getIdentityVerifyBlockHash(), responseForVehicle.getIdentityVerifyBlockMerklePath());
        //TODO:处理数据先不写了，反正也不重要
        //打印最小距离
        System.out.println(responseForVehicle.getMinDis());
        minDisList.add(responseForVehicle.getMinDis());
    }

    public void receiveFromRSU(SimpleResponseForVehicle responseForVehicle, String timestamp) throws Exception {

        if (!rsuPublicKeyMap.containsKey(responseForVehicle.getRsuId())) {
            //二次请求
            sendIdentityRequestToRSU();
        }
        String rsuPublicKey = rsuPublicKeyMap.get(responseForVehicle.getRsuId());

        //时间验证
        long time = Long.parseLong(EncryptUtils.decryptByPublicKey(rsuPublicKey, timestamp));
        if (System.currentTimeMillis() - time > 20000) {//TODO:这个时间
            throw new Exception("回复超时");
        }

        //TODO:处理数据先不写了，反正也不重要
        //打印最小距离
        System.out.println(responseForVehicle.getMinDis());
        minDisList.add(responseForVehicle.getMinDis());
    }


    private void checkRSUIdentity(String rsuPublicKey, String rsuBlockHash, String rsuBlockMerkleRoot, List<String> rsuBlockMerklePath, String rsuVerifyBlockHash, List<String> rsuVerifyBlockMerklePath) throws Exception {
        //1.验证区块确实存在
        if (!identityVerifyHeadList.containsKey(rsuVerifyBlockHash)) {
            log.log(Level.WARNING, "RSU身份验证错误1");
            throw new Exception("RSU身份验证错误1");
        }
        String rsuVerifyMerkelRoot = identityVerifyHeadList.get(rsuVerifyBlockHash);

        //2.身份区块的头+merkle根确实在验证区块上
        boolean result = checkMerklePath(rsuVerifyMerkelRoot, rsuVerifyBlockMerklePath);
        if (!result) {
            log.log(Level.WARNING, "RSU身份验证错误2");
            throw new Exception("RSU身份验证错误2");
        }

        //3.身份区块在验证区块上的merkle路径的第0个是SHA256Twice(身份区块Hash值+身份区块merkle根)
        if (!rsuVerifyBlockMerklePath.get(0).equals(HashUtils.applySha256ByApacheTwice(rsuBlockHash + rsuBlockMerkleRoot))) {
            log.log(Level.WARNING, "RSU身份验证错误3");
            throw new Exception("RSU身份验证错误3");
        }
        //公钥确实在路径上
        if (!rsuBlockMerklePath.get(0).equals(HashUtils.applySha256ByApacheTwice(rsuPublicKey)) && !rsuBlockMerklePath.get(1).equals(HashUtils.applySha256ByApacheTwice(rsuPublicKey))) {
            log.log(Level.WARNING, "RSU身份验证错误3");
            throw new Exception("RSU身份验证错误3");
        }
        //4.路径正确
        result = checkMerklePath(rsuBlockMerkleRoot, rsuBlockMerklePath);
        if (!result) {
            log.log(Level.WARNING, "RSU身份验证错误5");
            throw new Exception("RSU身份验证错误5");
        }
    }

    private boolean checkMerklePath(String merkleRoot, List<String> merklePath) {
        String prev = HashUtils.applySha256ByApacheTwice(merklePath.get(0) + merklePath.get(1));
        for (int i = 2; i < merklePath.size(); i += 2) {
            if (prev.equals(merklePath.get(i))) {
                prev = HashUtils.applySha256ByApacheTwice(prev + merklePath.get(i + 1));
                continue;
            }
            if (prev.equals(merklePath.get(i + 1))) {
                prev = HashUtils.applySha256ByApacheTwice(merklePath.get(i) + prev);
                continue;
            }
            return false;
        }
        return merkleRoot.equals(prev);
    }

    /**
     * 得到某笔交易的merkle路径
     *
     * @param merkleTree 所有交易的hash
     * @param index      该交易的索引
     * @return
     */
    private List<String> getMerklePath(List<String> merkleTree, int index) {
        List<String> path = new LinkedList<>();
        path.add(merkleTree.get(index));
        int tmp = index;
        while (tmp > 0) {
            if (tmp % 2 == 0) {
                path.add(merkleTree.get(index - 1));
                tmp = (tmp - 2) / 2;
            }
            path.add(merkleTree.get(index + 1));
            tmp = (tmp - 1) / 2;
        }
        return path;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, Long> getResultMap() {
        return resultMap;
    }

    public void setVehicleSendThread(VehicleTCPThread vehicleSendThread) {
        this.vehicleSendThread = vehicleSendThread;
    }

    public void setVehicleReceiveThread(VehicleTCPThread vehicleReceiveThread) {
        this.vehicleReceiveThread = vehicleReceiveThread;
    }

    public static List<Double> getMinDisList() {
        return minDisList;
    }
}
