package testVersion;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 两种链：身份信息链和数据链，且都有各自的区块头链
 */
public class RoadSideUnit {

    private final Logger log = Logger.getLogger("RoadSideUnit");

    //打印路径
    private final String ROOT_PATH = this.getClass().getResource("").getPath();
    //文件格式
    private final String SUFFIX = ".json";

    //测试专用:用于统计
    private int COUNT = 0;

    //下面的属性都与线程相同
    private Integer id;
    private String name;
    private Integer groupId;//组别，分区域
    private String privateKey;
    private String publicKey;

    //自己的身份信息记录在哪里
    private String myBlockHash;//身份信息区块链hash值
    private Integer myTransactionIndex;//身份区块交易索引

    //只记录一个临近路侧节点的ip
    private static final String NEIGHBOR_IP = "192.168.1.100";
    //云服务器ip
    private static final String CS_IP = "192.168.1.100";

    //数据区块链及其区块头链
    private List<Block> dataList = new LinkedList<>();
    private Map<String, Block> dataMap = new HashMap<>();
    private Map<String, Double> dataPointMap = new HashMap<>();//上一次区块的得分
    private Map<String, Long> dataAccessTimeMap = new HashMap<>();//上一次区块被访问时间
    private List<Block> dataVerifyList = new LinkedList<>();
    private Map<String, Block> dataVerifyMap = new HashMap<>();

    //身份信息区块链及其区块头链
    private List<Block> identityList = new LinkedList<>();
    private Map<String, Block> identityMap = new HashMap<>();
    private List<Block> identityVerifyList = new LinkedList<>();
    private Map<String, Block> identityVerifyMap = new HashMap<>();

    //每种区块的交易条数
    private static final Integer IDEN_NUM = 20;//身份信息
    private static final Integer DATA_NUM = 10;//数据信息
    private static final Integer VERIFY_NUM = 10;//验证区块
    private static final Integer DIFFICULTY = 5;//难度

    //每次请求的key和结果
    private Map<String, ResponseForIoV> resultMap = new HashMap<>();

    //车辆提交的车联网数据，将来用于挖矿
    private List<String> mineDataList = new LinkedList<>();
    private String prevHash = "";//数据区块前一个区块hash值
    private Block currentBlock = null;//当前自己已挖出来的区块，用于比较时间戳

    //同命令行交互的线程
    private RoadSideUnitTCPThread roadSideUnitCMDThread;

    //任务列表，方便线程取用
    //车辆请求任务队列
    private ConcurrentLinkedQueue<byte[]> carTaskQueue = new ConcurrentLinkedQueue<>();
    //用于处理车辆请求的线程池
    private ExecutorService pool = new ThreadPoolExecutor(5, 8, 1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());
    private static int threadIndex = 0;//处理车辆请求的线程的编号

    //关键词请求表 记录每次车辆请求的关键词:key是关键词，value是被访问次数
    private static Map<String, Integer> wordMap = new HashMap<>();

    //热度表 记录每次车辆请求的区块:key是区块hash值，value是被访问次数
    private static Map<String, Integer> heatMap = new HashMap<>();

    //总请求数
    private static long all = 0;
    //未命中数
    private static long miss = 0;
    //命中率:每100次记录一下
    private static List<Double> hitList = new LinkedList<>();

    //最小距离阈值，超过这个值就说明距离太远不能采用
    private static final double THRESHHOLD = 0.3;

    //公式的参数:相加为1
    private static double PARAM_A = 0.3;
    private static double PARAM_B = 0.3;
    private static double PARAM_C = 0.4;

    private ObjectMapper mapper = new ObjectMapper();

    public RoadSideUnit(Integer id, String name, Integer groupId, String privateKey, String publicKey, String myBlockHash, Integer myTransactionIndex) {
        this.id = id;
        this.name = name;
        this.groupId = groupId;
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
            case -1://将区块从文件中读出老
                deserializeBlocks();
                System.out.println("-1 finished");
                break;
            case 0://打印区块，同时序列化区块到文件中便于将来读出
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
            case 2://提交热度表
                System.out.println(heatMap);
                System.out.println(wordMap);
                String requestKey = "SUBMIT_HEAT_TABLE";
                String requestIndex = id + "";
                sendRequestToOthers(CS_IP, requestKey, requestIndex, heatMap);
                System.out.println("2 finished");
                break;
            case 3://筛选区块
                filterBlocks();
                System.out.println("3 finished");
                break;
            case 4://清空记录
                heatMap = new HashMap<>();
                wordMap = new HashMap<>();
                all = 0;
                miss = 0;
                hitList = new LinkedList<>();
                resultMap = new HashMap<>();

                System.out.println("4 finished");
                break;
        }
    }

    /**
     * 打印区块为可读形式到文件中
     *
     * @throws Exception
     */
    private void printBlocks() throws Exception {
        System.out.println("Print Data Blocks---------");
        List<String> list = new ArrayList<>(dataList.size());
        for (Block block : dataList) {
            list.add(mapper.writeValueAsString(block));
        }
        FileUtils.printToFileByline(ROOT_PATH + "/dataBlocks" + SUFFIX, list);

        System.out.println("Print Data Verify Blocks---------");
        list = new ArrayList<>(dataVerifyList.size());
        for (Block block : dataVerifyList) {
            list.add(mapper.writeValueAsString(block));
        }
        FileUtils.printToFileByline(ROOT_PATH + "/dataVerifyBlocks" + SUFFIX, list);

        System.out.println("Print Identity Blocks---------");
        list = new ArrayList<>(identityList.size());
        for (Block block : identityList) {
            list.add(mapper.writeValueAsString(block));
        }
        FileUtils.printToFileByline(ROOT_PATH + "/identityBlocks" + SUFFIX, list);

        System.out.println("Print Identity Verify Blocks---------");
        list = new ArrayList<>(identityVerifyList.size());
        for (Block block : identityVerifyList) {
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
        FileUtils.serialization(dataList, ROOT_PATH + "/serialDataBlocks");
        FileUtils.serialization(dataVerifyList, ROOT_PATH + "/serialDataVerifyList");
        FileUtils.serialization(identityList, ROOT_PATH + "/serialIdentityList");
        FileUtils.serialization(identityVerifyList, ROOT_PATH + "/serialIdentityVerifyList");
        System.out.println("serialize blocks finished");
    }

    /**
     * 序列化区块读出来
     *
     * @throws Exception
     */
    private void deserializeBlocks() throws Exception {
        dataList = (List) FileUtils.deserialization(ROOT_PATH + "/serialDataBlocks");
        dataVerifyList = (List) FileUtils.deserialization(ROOT_PATH + "/serialDataVerifyList");
        identityList = (List) FileUtils.deserialization(ROOT_PATH + "/serialIdentityList");
        identityVerifyList = (List) FileUtils.deserialization(ROOT_PATH + "/serialIdentityVerifyList");
        System.out.println("deserialize blocks finished");
    }

    /**
     * 用requestKey来判断是哪种请求
     * 用requestIndex来判断是哪次请求
     *
     * @param socket 车辆请求需要回复 服务器请求需要回复
     * @param data
     * @throws Exception
     */
    public void receiveFromOthers(Socket socket, byte[] data) throws Exception {
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

//        //分组下发命令:以000开头的为广播
//        if (!requestKey.startsWith(groupId + "_") && !requestKey.startsWith("000_")) {
//            return;
//        }
        //去掉组别
//        requestKey = requestIndex.substring(requestKey.indexOf("_") + 1);
        switch (requestKey) {
            case "CAR_IDENTITY_REQUEST"://车辆请求路侧节点身份信息
                String carPublicKey = checkVehicleIdentity((String) map.get("cardBlockHash"), null, (Integer) map.get("carTransactionIndex"), null);
                sendRequestToVehicle(socket, "CAR_IDENTITY_RESPONSE", requestIndex, setIdentityDataForVehicle(carPublicKey));
                break;
            case "CAR_REQUEST"://车辆请求数据
                //用线程池处理
                carTaskQueue.add(data);
                pool.execute(new RoadSideUnitBusinessThread("rsu business thread " + (threadIndex++), this, socket));
                break;
            case "CAR_SUBMIT"://车辆提交数据,无返回
                saveIoVData((String) map.get("cardBlockHash"), (Integer) map.get("carTransactionIndex"), (String) map.get("encryptData"));
                break;
            case "UNIT_BLOCK_RESPONSE_FROM_CS":
                if (!requestIndex.contains(name + id + "_")) {
                    break;
                }
                System.out.println(System.currentTimeMillis() + "/" + Thread.currentThread().getName() + "/receive cs response:" + requestKey + ";" + requestIndex);

                String key = "UNIT_BLOCK_REQUEST_TO_CS" + requestIndex;
                if (resultMap.containsKey(key) && resultMap.get(key) == null) {
                    resultMap.put(key, mapper.readValue(mapper.writeValueAsString(map.get("data")), ResponseForIoV.class));
                }
                break;
            case "UNIT_BLOCK_RESPONSE"://区块请求回复
                if (!requestIndex.contains(name + id + "_")) {
                    break;
                }
                System.out.println(System.currentTimeMillis() + "/" + Thread.currentThread().getName() + "/receive rsu response:" + requestKey + ";" + requestIndex);

                key = "UNIT_BLOCK_REQUEST" + requestIndex;
                if (resultMap.containsKey(key) && resultMap.get(key) == null) {
                    resultMap.put(key, mapper.readValue(mapper.writeValueAsString(map.get("data")), ResponseForIoV.class));
                }
                break;
            case "UNIT_BLOCK_REQUEST"://区块请求
                //不收自己的数据
                if (requestIndex.contains(name + id + "_")) {
                    break;
                }
                System.out.println(System.currentTimeMillis() + "/" + Thread.currentThread().getName() + "/receive rsu request:" + requestKey + ";" + requestIndex);

                ResponseForIoV response = findBestBlock2(((ArrayList<String>) map.get("data")).toArray(new String[]{}));
                //没找到就不返回
                if (response.getDataBlockHash() != null) {
                    setData(response);
                    sendRequestToOthers(socket, "UNIT_BLOCK_RESPONSE", (String) map.get("requestIndex"), response);
                    System.out.println(System.currentTimeMillis() + "/" + Thread.currentThread().getName() + "/send rsu response:" + requestKey + ";" + requestIndex);
                } else {
                    System.out.println(System.currentTimeMillis() + "/" + Thread.currentThread().getName() + "/send rsu response fail:" + requestKey + ";" + requestIndex);
                }
                break;
            case "MINE_START_1"://挖矿，由服务器发布挖矿开始命令
                //TODO:暂时不做挖矿了
                prevHash = (String) map.get("prevHash");//云服务器告知最后一个区块的头
                mine(dataList, DATA_NUM);
                break;
            case "MINE_SUBMIT"://挖矿，其他节点提交新区块
                Block block = (Block) map.get("data");
                Set<String> newBlockDataSet = block.getData().stream().map(IoVTransaction::getContent).collect(Collectors.toSet());

                if (!block.validate()) {
                    break;
                }
                if (block.getPrevHash().equals(prevHash)) {//说明冲突了
                    if (currentBlock == null || block.getCreateTimeStamp() < currentBlock.getCreateTimeStamp()) {
                        prevHash = block.getHash();//设定父区块是新的区块
                        mineDataList.removeIf(d -> newBlockDataSet.contains(d));//移除已被写入的数据
                        currentBlock = null;
                        break;
                    }
                    //自己的区块更早那么就舍弃收到的区块
                    break;
                }

                break;
            case "CLOUD_DATA_BLOCK_ALLOCATING_1"://分配车联网数据区块
                if (requestIndex.contains(name + id + "_")) {//是不是给自己的
                    System.out.println(name + "/" + System.currentTimeMillis() + "/receive cs block start1");
                    Set<String> newHashes = new HashSet<>((List) map.get("data"));//服务器发过来分配区块的hash
                    Set<String> myHashes = dataList.stream().map(Block::getHash).collect(Collectors.toSet());//本地区块的hash
                    myHashes.retainAll(newHashes);//同本地对比得到交集
                    if ((Boolean) map.get("flag")) {
                        dataList.removeIf(b -> !myHashes.contains(b.getHash()));//把不在交集中的区块移除
                        dataMap = dataList.stream().collect(Collectors.toMap(Block::getHash, Function.identity()));
                    }
                    newHashes.removeAll(myHashes);//得到需要的新区块
                    if (newHashes.isEmpty()) {//新的列表是一个子集或者相同，那么就不变了
                        return;
                    }
                    //向云服务器请求没缓存过的区块
                    sendRequestToOthers(socket, "CLOUD_DATA_BLOCK_ALLOCATING_REQUEST_1", name + id + "_" + System.currentTimeMillis(), newHashes);
                    System.out.println(name + "/" + System.currentTimeMillis() + " / receive cs block end1");
                }
                break;
            case "CLOUD_DATA_BLOCK_ALLOCATING_RESPONSE_1"://分配车联网数据区块二次请求
                if (requestIndex.contains(name + id + "_")) {//是不是给自己的
                    System.out.println(name + "/" + System.currentTimeMillis() + "/receive cs block start2");
                    List<Block> newBlockList = mapper.readValue(mapper.writeValueAsString(map.get("data")), mapper.getTypeFactory().constructCollectionType(LinkedList.class, Block.class));
                    dataList.addAll(newBlockList);//把新缓存的加入就可以啦
                    newBlockList.forEach(b -> dataMap.put(b.getHash(), b));
                    System.out.println(name + "/" + System.currentTimeMillis() + "/receive cs block end2");
                }
                break;
            case "CLOUD_DATA_BLOCK_ALLOCATING_2"://分配身份信息数据区块
                System.out.println(name + "/" + System.currentTimeMillis() + "/receive cs block start3");
                Set<String> newHashes = new HashSet<>((List) map.get("data"));//服务器发过来分配区块的hash
                Set<String> myHashes = identityList.stream().map(Block::getHash).collect(Collectors.toSet());//本地区块的hash
                myHashes.retainAll(newHashes);//同本地对比得到交集
                if ((Boolean) map.get("flag")) {
                    identityList.removeIf(b -> !myHashes.contains(b.getHash()));//把不在交集中的区块移除
                    identityMap = identityList.stream().collect(Collectors.toMap(Block::getHash, Function.identity()));
                }
                newHashes.removeAll(myHashes);//得到需要的新区块
                if (newHashes.isEmpty()) {//新的列表是一个子集或者相同，那么就不变了
                    return;
                }
                //向云服务器请求没缓存过的区块
                sendRequestToOthers(socket, "CLOUD_DATA_BLOCK_ALLOCATING_REQUEST_2", name + id + "_" + System.currentTimeMillis(), newHashes);
                System.out.println(name + "/" + System.currentTimeMillis() + "/receive cs block end3");
                break;
            case "CLOUD_DATA_BLOCK_ALLOCATING_RESPONSE_2"://分配身份信息区块二次请求
                if (requestIndex.contains(name + id + "_")) {//是不是给自己的
                    System.out.println(name + "/" + System.currentTimeMillis() + "/receive cs block start4");
                    List<Block> newBlockList = mapper.readValue(mapper.writeValueAsString(map.get("data")), mapper.getTypeFactory().constructCollectionType(LinkedList.class, Block.class));
                    identityList.addAll(newBlockList);//把新缓存的加入就可以啦
                    newBlockList.forEach(b -> identityMap.put(b.getHash(), b));
                    System.out.println(name + "/" + System.currentTimeMillis() + "/receive cs block end4");
                }
                break;
            case "CLOUD_VERIFY_BLOCK_ALLOCATING_1"://分配车联网数据验证区块
                System.out.println(name + "/" + System.currentTimeMillis() + "/receive cs block start5");
                newHashes = new HashSet<>((List) map.get("data"));//服务器发过来分配区块的hash
                myHashes = dataVerifyList.stream().map(Block::getHash).collect(Collectors.toSet());//本地区块的hash
                myHashes.retainAll(newHashes);//同本地对比得到交集
                if ((Boolean) map.get("flag")) {
                    dataVerifyList.removeIf(b -> !myHashes.contains(b.getHash()));//把不在交集中的区块移除
                    dataVerifyMap = dataVerifyList.stream().collect(Collectors.toMap(Block::getHash, Function.identity()));
                }
                newHashes.removeAll(myHashes);//得到需要的新区块
                if (newHashes.isEmpty()) {//新的列表是一个子集或者相同，那么就不变了
                    return;
                }
                //向云服务器请求没缓存过的区块
                sendRequestToOthers(socket, "CLOUD_VERIFY_BLOCK_ALLOCATING_REQUEST_1", name + id + "_" + System.currentTimeMillis(), newHashes);
                System.out.println(name + "/" + System.currentTimeMillis() + "/receive cs block end5");
                break;
            case "CLOUD_VERIFY_BLOCK_ALLOCATING_RESPONSE_1"://分配车联网数据验证区块二次请求
                if (requestIndex.contains(name + id + "_")) {//是不是给自己的
                    System.out.println(name + "/" + System.currentTimeMillis() + "/receive cs block start6");
                    List<Block> newBlockList = mapper.readValue(mapper.writeValueAsString(map.get("data")), mapper.getTypeFactory().constructCollectionType(LinkedList.class, Block.class));
                    dataVerifyList.addAll(newBlockList);//把新缓存的加入就可以啦
                    newBlockList.forEach(b -> dataVerifyMap.put(b.getHash(), b));
                    System.out.println(name + "/" + System.currentTimeMillis() + "/receive cs block end6");
                }
                break;
            case "CLOUD_VERIFY_BLOCK_ALLOCATING_2"://分配身份信息数据验证区块
                System.out.println(name + "/" + System.currentTimeMillis() + "/receive cs block start7");
                newHashes = new HashSet<>((List) map.get("data"));//服务器发过来分配区块的hash
                myHashes = identityVerifyList.stream().map(Block::getHash).collect(Collectors.toSet());//本地区块的hash
                myHashes.retainAll(newHashes);//同本地对比得到交集
                if ((Boolean) map.get("flag")) {
                    identityVerifyList.removeIf(b -> !myHashes.contains(b.getHash()));//把不在交集中的区块移除
                    identityVerifyMap = identityVerifyList.stream().collect(Collectors.toMap(Block::getHash, Function.identity()));
                }
                newHashes.removeAll(myHashes);//得到需要的新区块
                if (newHashes.isEmpty()) {//新的列表是一个子集或者相同，那么就不变了
                    return;
                }
                //向云服务器请求没缓存过的区块
                sendRequestToOthers(socket, "CLOUD_VERIFY_BLOCK_ALLOCATING_REQUEST_2", name + id + "_" + System.currentTimeMillis(), newHashes);
                System.out.println(name + "/" + System.currentTimeMillis() + "/receive cs block end7");
                break;
            case "CLOUD_VERIFY_BLOCK_ALLOCATING_RESPONSE_2"://分配身份信息数据验证区块二次请求
                if (requestIndex.contains(name + id + "_")) {//是不是给自己的
                    System.out.println(name + "/" + System.currentTimeMillis() + "/receive cs block start8");
                    List<Block> newBlockList = mapper.readValue(mapper.writeValueAsString(map.get("data")), mapper.getTypeFactory().constructCollectionType(LinkedList.class, Block.class));
                    identityVerifyList.addAll(newBlockList);//把新缓存的加入就可以啦
                    newBlockList.forEach(b -> identityVerifyMap.put(b.getHash(), b));
                    System.out.println(name + "/" + System.currentTimeMillis() + "/receive cs block end8");
                }
                break;
        }
    }

    /**
     * 一个必须按顺序访问的函数，用于挖矿冲突
     *
     * @param newBlock
     */
    private synchronized void processCollision(Block newBlock) {

    }

    //TODO:按照云服务器的代码改一下
    private void mine(List<Block> list1, int num1) throws Exception {
        List<IoVTransaction> dataTransactions = new ArrayList<>(num1);
        for (String mineData : mineDataList) {
            IoVTransaction dataTransaction = new IoVTransaction(1, null, mineData, null);
            dataTransactions.add(dataTransaction);
        }

        for (int i = 0; i < dataTransactions.size(); i++) {
            int start = i;
            int end = i * DATA_NUM;
            Block block = new Block(prevHash, "1.0", dataTransactions.subList(start, end), DIFFICULTY);
            block.mineBlock();
            sendRequestToOthers(CS_IP, "MINE_SUBMIT", "ALL", block);
        }


        //每个数据区块都要设置自己的验证区块在哪里；每笔交易都要设置自己的merkle路径
        for (int i = 0; i < list1.size(); i++) {
            Block block = list1.get(i);
            for (int j = 0; j < block.getData().size(); j++) {
                block.getData().get(j).setMerklePath(getMerklePath(block.getMerkleTree(), j));
            }
        }

        mineDataList = new LinkedList<>();
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


    /**
     * 路侧节点接收车辆请求
     *
     * @param cardBlockHash       车辆身份所在数据区块
     * @param carTransactionIndex 车辆身份所在数据交易索引
     * @return 返回请求内容和路侧节点身份信息
     */
    public String receiveVehicleRequest(String cardBlockHash, String cardBlockHash1, Integer carTransactionIndex, String carTransactionIndex1, String keywordsVectorEncryptData, String timestampEncryptData) throws Exception {
        if (dataList == null || dataList.isEmpty()) {//尚未初始化
            return null;
        }
        all++;
        if (all % 100 == 0) {//每100次请求记录一下命中率
            hitList.add(1 - ((0.0 + miss) / all));
            ExperimentRecordUtils.printToExcel(hitList, "RSU_" + System.currentTimeMillis());
        }
        try {
            String carPublicKey = checkVehicleIdentity(cardBlockHash, cardBlockHash1, carTransactionIndex, carTransactionIndex1);

            //TODO:检查时间戳
            long timestamp = Long.parseLong(EncryptUtils.decryptByPublicKey(carPublicKey, timestampEncryptData));
            //下面根据关键词匹配区块，只返回一个区块
            //关键词匹配率达到80%以上才算是合格的区块
//            String[] keywordsVector = mapper.readValue(EncryptUtils.decryptByPublicKey(carPublicKey, keywordsVectorEncryptData), String[].class);
            String[] keywordsVector = mapper.readValue(keywordsVectorEncryptData, String[].class);

            //测试版本最简化
//            ResponseForVehicle response = findBestBlock1(keywordsVector);
            SimpleResponseForVehicle response = findBestBlock3(keywordsVector);

            //先询问临近路侧节点,最后询问云服务器
            if (StringUtils.isBlank(response.getDataBlockHash())) {
                //记录未命中数
                miss++;
                String requestKey = "UNIT_BLOCK_REQUEST";
                String requestIndex = name + id + "_" + System.currentTimeMillis();
                //自己发送的自己监听
                String key = requestKey + requestIndex;
                resultMap.put(key, null);
                Socket socket = sendRequestToOthers(NEIGHBOR_IP, requestKey, requestIndex, keywordsVector);
                roadSideUnitCMDThread.receive(socket);
                if (resultMap.get(key) != null) {
                    ResponseForIoV result = resultMap.get(key);
                    response.copyResponse(result);
                } else {
                    //TODO:设置一个阈值，用于表明没找到
                    requestKey = "UNIT_BLOCK_REQUEST_TO_CS";
                    sendRequestToOthers(CS_IP, requestKey, requestIndex, keywordsVector);
                    key = requestKey + requestIndex;
                    resultMap.put(key, null);
                    for (int i = 0; i < 5; i++) {//等50ms
                        System.out.println(key + "-" + i);
                        if (resultMap.get(key) != null) {
                            break;
                        }
                        Thread.sleep(10);
                    }
                    //默认云服务器一定找到
                    ResponseForIoV result = resultMap.get(key);
                    response.copyResponse(result);
                }
//                resultMap.remove(key);//暂时不移除
            }

            //记录最近被访问时间
            dataAccessTimeMap.put(response.getDataBlockHash(), System.currentTimeMillis());

            //组装回复参数
            if (heatMap.containsKey(response.getDataBlockHash())) {
                heatMap.put(response.getDataBlockHash(), heatMap.get(response.getDataBlockHash()) + 1);
            } else {
                heatMap.put(response.getDataBlockHash(), 1);
            }
            for (String word : keywordsVector) {
                if (wordMap.containsKey(word)) {
                    wordMap.put(word, wordMap.get(word) + 1);
                    continue;
                }
                wordMap.put(word, 1);
            }
            return setDataForVehicle(carPublicKey, response);
        } catch (Exception e) {
            log.log(Level.WARNING, "车辆请求出错");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 校验车辆身份
     *
     * @param cardBlockHash        车辆身份所在区块hash
     * @param cardBlockHash1       cardBlockHash被车私钥加密后的数据
     * @param carTransactionIndex  车辆身份所在交易索引
     * @param carTransactionIndex1 carTransactionIndex被车私钥加密后的数据
     * @return 返回车辆公钥
     * @throws Exception
     */
    private String checkVehicleIdentity(String cardBlockHash, String cardBlockHash1, Integer carTransactionIndex, String carTransactionIndex1) throws Exception {
        Block carIdentityBlock = identityMap.get(cardBlockHash);
        if (carIdentityBlock == null) {
            log.log(Level.WARNING, "车辆身份所在数据区块索引不存在");
            throw new Exception("车辆身份所在数据区块索引不存在");
        }
        IoVTransaction carIdentityTran = carIdentityBlock.getData().get(carTransactionIndex);
        if (carIdentityTran == null) {
            log.log(Level.WARNING, "车辆身份所在数据交易索引不存在");
            throw new Exception("车辆身份所在数据交易索引不存在");
        }

        //请求数据包括:id车辆唯一标识，keywords关键词列表，carIdentityBlock的id和carIdentityTran的id，车辆的公钥
//        String cardBlockHashPlainText = EncryptUtils.decryptByPublicKey(carIdentityTran.getRSAPublicKey(), cardBlockHash1);
//        if (!cardBlockHashPlainText.equals(cardBlockHash)) {
//            log.log(Level.WARNING, "车辆身份数据1错误");
//            throw new Exception("车辆身份数据1错误");
//        }
//        String carTransactionIndexText = EncryptUtils.decryptByPublicKey(carIdentityTran.getRSAPublicKey(), carTransactionIndex1);
//        if (!carTransactionIndexText.equals(carTransactionIndex + "")) {
//            log.log(Level.WARNING, "车辆身份数据2错误");
//            throw new Exception("车辆身份数据2错误");
//        }
        return carIdentityTran.getRSAPublicKey();
    }

    /**
     * 已知关键词找区块
     *
     * @param words
     * @return
     */
    private ResponseForVehicle findBestBlock1(String[] words) {
        ResponseForVehicle response = new ResponseForVehicle();
        response.setMinDis(THRESHHOLD);

//        dataList.forEach(block -> {
//            block.getData().forEach(tran -> {
//                double tmp = calculateVectorSize(words, tran.getKeywordsVector());
//                if (tmp < response.getMinDis()) {
//                    response.setMinDis(tmp);
//                    response.setDataBlockHash(block.getHash());
//                    response.setDataTranHash(tran.getHash());
//                }
//            });
//        });

        //想要跳出循环
        dataList.stream().anyMatch(block -> {
            block.getData().stream().anyMatch(tran -> {
                double tmp = calculateVectorSize(words, tran.getKeywordsVector());
                if (tmp < THRESHHOLD) {
                    response.setMinDis(tmp);
                    response.setDataBlockHash(block.getHash());
                    response.setDataTranHash(tran.getHash());
                    return true;
                }
                return false;
            });

            return !StringUtils.isBlank(response.getDataBlockHash());
        });

        if (response.getMinDis() > THRESHHOLD) {
            response.setMinDis(null);
            response.setDataBlockHash(null);
            response.setDataTranHash(null);
        }
        return response;
    }

    /**
     * 已知关键词找区块
     *
     * @param words
     * @return
     */
    private SimpleResponseForVehicle findBestBlock3(String[] words) {
        SimpleResponseForVehicle response = new SimpleResponseForVehicle();
        response.setMinDis(THRESHHOLD);

//        dataList.forEach(block -> {
//            block.getData().forEach(tran -> {
//                double tmp = calculateVectorSize(words, tran.getKeywordsVector());
//                if (tmp < response.getMinDis()) {
//                    response.setMinDis(tmp);
//                    response.setDataBlockHash(block.getHash());
//                    response.setDataTranHash(tran.getHash());
//                }
//            });
//        });

        //想要跳出循环
        dataList.stream().anyMatch(block -> {
            block.getData().stream().anyMatch(tran -> {
                double tmp = calculateVectorSize(words, tran.getKeywordsVector());
                if (tmp < THRESHHOLD) {
                    response.setMinDis(tmp);
                    response.setDataBlockHash(block.getHash());
                    response.setDataTranHash(tran.getHash());
                    return true;
                }
                return false;
            });

            return !StringUtils.isBlank(response.getDataBlockHash());
        });

        if (response.getMinDis() > THRESHHOLD) {
            response.setMinDis(null);
            response.setDataBlockHash(null);
            response.setDataTranHash(null);
        }
        return response;
    }

    /**
     * 在车辆需要的关键词下分值最高（长度最长）的交易所在的区块
     *
     * @param words
     */
    private double calculateVectorSize(String[] words, Map<String, Double> keywordsVector) {
        double sum = 0;
        for (String word : words) {
            sum += Math.pow(keywordsVector.get(word), 2);
        }
        return Math.pow(sum, 0.5);//开根号
    }

    /**
     * 计算向量间距离
     *
     * @param v1
     * @param v2
     * @return
     */
    private double calculateVectorDistance(Map<String, Double> v1, Map<String, Double> v2) {
        //只计算重叠的部分
        v1.keySet().retainAll(v2.keySet());
        double sum = v1.entrySet().stream().mapToDouble(entry -> Math.pow(entry.getValue() - v2.get(entry.getKey()), 2)).sum();
        return Math.pow(sum, 0.5);
    }

    /**
     * 已知向量找区块
     *
     * @param words
     * @return
     */
    private ResponseForIoV findBestBlock2(String... words) {
        ResponseForIoV response = new ResponseForIoV();
        response.setMinDis(Double.MAX_VALUE);
        //遍历
        dataList.forEach(block -> {
            block.getData().forEach(tran -> {
                double tmp = calculateVectorSize(words, tran.getKeywordsVector());
                if (tmp < response.getMinDis()) {
                    response.setMinDis(tmp);
                    response.setDataBlockHash(block.getHash());
                    response.setDataTranHash(tran.getHash());
                }
            });
        });

        if (response.getMinDis() > THRESHHOLD) {
            response.setMinDis(null);
            response.setDataBlockHash(null);
            response.setDataTranHash(null);
        }

        return response;
    }


    /**
     * 车辆请求信息设置
     *
     * @param carPublicKey
     * @param response
     * @return
     * @throws Exception
     */
    private String setDataForVehicle(String carPublicKey, ResponseForVehicle response) throws Exception {
        if (StringUtils.isBlank(response.getContent())) {
            setData(response);
            //content不为空说明是向其他路侧节点请求的，那么说明其他字段已经被设置了，况且本路侧节点也没有这个区块
        }
        //车公钥 加密数据
        //由于加密长度限制，所以只加密重要数据
        response.setContent(EncryptUtils.encryptByPublicKey(carPublicKey, response.getContent()));

        //特别的，车辆需要验证路侧节点身份，所以还需要加上路侧节点身份信息
        Block identityBlock = identityMap.get(myBlockHash);
        Block identityVerifyBlock = identityVerifyList.get(identityBlock.getVerifyBlockIndex());
        response.setPublicKey(publicKey);
        response.setIdentityBlockHash(identityBlock.getHash());
        response.setIdentityBlockMerkleRoot(identityBlock.getMerkleTree().get(0));
        response.setIdentityBlockMerklePath(identityBlock.getData().get(myTransactionIndex).getMerklePath());
        response.setIdentityVerifyBlockHash(identityVerifyBlock.getHash());
        response.setIdentityVerifyBlockMerklePath(identityVerifyBlock.getData().get(identityBlock.getVerifyTranIndex()).getMerklePath());

        return mapper.writeValueAsString(response);
    }

    /**
     * 车辆请求信息设置
     *
     * @param carPublicKey
     * @param response
     * @return
     * @throws Exception
     */
    private String setDataForVehicle(String carPublicKey, SimpleResponseForVehicle response) throws Exception {
        if (StringUtils.isBlank(response.getContent())) {
            setData(response);
            //content不为空说明是向其他路侧节点请求的，那么说明其他字段已经被设置了，况且本路侧节点也没有这个区块
        }
        //车公钥 加密数据
        //由于加密长度限制，所以只加密重要数据
        response.setContent(EncryptUtils.encryptByPublicKey(carPublicKey, response.getContent()));

        response.setRsuId(id);
        return mapper.writeValueAsString(response);
    }

    /**
     * 车辆请求信息设置
     *
     * @param carPublicKey
     * @return
     * @throws Exception
     */
    private String setIdentityDataForVehicle(String carPublicKey) throws Exception {
        IdentityResponseForVehicle response = new IdentityResponseForVehicle();
        //特别的，车辆需要验证路侧节点身份，所以还需要加上路侧节点身份信息
        Block identityBlock = identityMap.get(myBlockHash);
        Block identityVerifyBlock = identityVerifyList.get(identityBlock.getVerifyBlockIndex());

        response.setPublicKey(publicKey);
        response.setIdentityBlockHash(identityBlock.getHash());
        response.setIdentityBlockMerkleRoot(identityBlock.getMerkleTree().get(0));
        response.setIdentityBlockMerklePath(identityBlock.getData().get(myTransactionIndex).getMerklePath());
        response.setIdentityVerifyBlockHash(identityVerifyBlock.getHash());
        response.setIdentityVerifyBlockMerklePath(identityVerifyBlock.getData().get(identityBlock.getVerifyTranIndex()).getMerklePath());

        response.setRsuId(EncryptUtils.encryptByPublicKey(carPublicKey, id + ""));

        response.setTimestamp1(EncryptUtils.encryptByPublicKey(carPublicKey, System.currentTimeMillis() + ""));
        response.setTimestamp2(EncryptUtils.encryptByPrivateKey(privateKey, System.currentTimeMillis() + ""));
        return mapper.writeValueAsString(response);
    }

    /**
     * 通用交易及其验证信息设置
     *
     * @param response
     * @throws Exception
     */
    private void setData(ResponseForIoV response) {
        Block dataBlock = dataMap.get(response.getDataBlockHash());
        Block dataVerifyBlock = dataVerifyList.get(dataBlock.getVerifyBlockIndex());
        IoVTransaction transaction = dataBlock.getData().stream().filter(tran -> tran.getHash().equals(response.getDataTranHash())).findFirst().get();
        response.setDataTranHash(transaction.getHash());
        response.setDataBlockMerkleRoot(dataBlock.getMerkleTree().get(0));
        response.setContent(transaction.getContent());
        response.setTranMerklePath(transaction.getMerklePath());
        response.setDataBlockMerklePath(dataVerifyBlock.getData().get(dataBlock.getVerifyTranIndex()).getMerklePath());
        response.setDataVerifyBlockIndex(dataBlock.getVerifyBlockIndex());
    }

    /**
     * 通用交易及其验证信息设置
     *
     * @param response
     * @throws Exception
     */
    private void setData(ResponseForVehicle response) {
        Block dataBlock = dataMap.get(response.getDataBlockHash());
        Block dataVerifyBlock = dataVerifyList.get(dataBlock.getVerifyBlockIndex());
        IoVTransaction transaction = dataBlock.getData().stream().filter(tran -> tran.getHash().equals(response.getDataTranHash())).findFirst().get();
        response.setDataTranHash(transaction.getHash());
        response.setDataBlockMerkleRoot(dataBlock.getMerkleTree().get(0));
        response.setContent(transaction.getContent());
        response.setTranMerklePath(transaction.getMerklePath());
        response.setDataBlockMerklePath(dataVerifyBlock.getData().get(dataBlock.getVerifyTranIndex()).getMerklePath());
        response.setDataVerifyBlockIndex(dataBlock.getVerifyBlockIndex());
    }

    /**
     * 通用交易及其验证信息设置
     *
     * @param response
     * @throws Exception
     */
    private void setData(SimpleResponseForVehicle response) {
        Block dataBlock = dataMap.get(response.getDataBlockHash());
        Block dataVerifyBlock = dataVerifyList.get(dataBlock.getVerifyBlockIndex());
        IoVTransaction transaction = dataBlock.getData().stream().filter(tran -> tran.getHash().equals(response.getDataTranHash())).findFirst().get();
        response.setDataTranHash(transaction.getHash());
        response.setDataBlockMerkleRoot(dataBlock.getMerkleTree().get(0));
        response.setContent(transaction.getContent());
        response.setTranMerklePath(transaction.getMerklePath());
        response.setDataBlockMerklePath(dataVerifyBlock.getData().get(dataBlock.getVerifyTranIndex()).getMerklePath());
        response.setDataVerifyBlockIndex(dataBlock.getVerifyBlockIndex());
    }


    /**
     * 向临近路侧节点或者云服务器发送数据
     *
     * @param requestKey   请求类型
     * @param requestIndex 请求序号
     */
    private Socket sendRequestToOthers(String ip, String requestKey, String requestIndex, Object data) throws Exception {
        Map<String, Object> requestDataMap = getGeneralRequestMap(requestKey, requestIndex, data);
        return roadSideUnitCMDThread.send(ip, ExpressUtils.deflate(mapper.writeValueAsBytes(requestDataMap)));
    }

    /**
     * 此函数用于路侧节点向云服务器的二次请求
     *
     * @param socket
     * @param requestKey
     * @param requestIndex
     * @param data
     * @throws Exception
     */
    private void sendRequestToOthers(Socket socket, String requestKey, String requestIndex, Object data) throws Exception {
        Map<String, Object> requestDataMap = getGeneralRequestMap(requestKey, requestIndex, data);
        roadSideUnitCMDThread.send(socket, ExpressUtils.deflate(mapper.writeValueAsBytes(requestDataMap)));

        //接受云服务器返回的消息
        roadSideUnitCMDThread.receive(socket);
    }

    public void sendRequestToVehicle(Socket socket, String requestKey, String requestIndex, Object data) throws Exception {
        Map<String, Object> requestDataMap = getGeneralRequestMap(requestKey, requestIndex, data);
        //私钥加密时间戳技能防止重放攻击，又能做数字签名
        requestDataMap.put("timestamp", EncryptUtils.encryptByPrivateKey(privateKey, System.currentTimeMillis() + ""));

        //记得压缩
        roadSideUnitCMDThread.send(socket, ExpressUtils.deflate(mapper.writeValueAsBytes(requestDataMap)));
    }


    /**
     * 保存车辆提交的车联网数据，将来用于挖矿
     *
     * @param cardBlockHash
     * @param carTransactionIndex
     * @param encryptData
     */
    private void saveIoVData(String cardBlockHash, Integer carTransactionIndex, String encryptData) throws Exception {
//        Map<String, Object> map = checkVehicleIdentity(cardBlockHash, carTransactionIndex, encryptData);
//        mineDataList.add((String) map.get("data"));
    }

    private Map<String, Object> getGeneralRequestMap(String requestKey, String requestIndex, Object data) {
        Map<String, Object> requestDataMap = new HashMap<>();
        requestDataMap.put("requestKey", requestKey);
        requestDataMap.put("requestIndex", requestIndex);
        requestDataMap.put("data", data);

        return requestDataMap;
    }

    /**
     * 将自身的区块筛选
     * 利用打分函数#getBlockScore
     */
    private void filterBlocks() {
        if (wordMap.isEmpty()) {
            return;
        }
        if (dataList.isEmpty()) {
            return;
        }
        long current = System.currentTimeMillis();

        double sum1 = wordMap.values().stream().mapToInt(i -> i).sum();//关键词被访问总量
        //路侧节点的向量
        Map<String, Double> wordFrequencyMap = wordMap.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue() / sum1));

        double sum2 = heatMap.values().stream().mapToInt(i -> i).sum();//总访问次数

        //key是区块hash，value是区块的得分
        Map<String, Double> blockPointMap = dataList.stream().collect(Collectors.toMap(Block::getHash, b -> {
            long l = dataAccessTimeMap.containsKey(b.getHash()) ? current - dataAccessTimeMap.get(b.getHash()) : 1000000L;//没被访问就是一个很大的时间差值-1000000L毫秒
            double prev = dataPointMap.containsKey(b.getHash()) ? dataPointMap.get(b.getHash()) : 0.3;//上次没有得分那就取平均分-经测试后是0.3左右
            double dis = calculateVectorDistance(getBlockVector(b.getData()), wordFrequencyMap);
            double freq = heatMap.containsKey(b.getHash()) ? heatMap.get(b.getHash()) / sum2 : 0;//没有被访问过就是0
            return getBlockScore(prev, l, dis, freq);
        }));

        //转成map之后排序,这个地方似乎新建一个list但是为了防止错乱还是新建一个
        List<Map.Entry<String, Double>> tmp = new ArrayList<>(blockPointMap.entrySet());
        //倒序
        tmp.sort((a, b) -> a.getValue() - b.getValue() > 0 ? -1 : 1);
        //移除多余的区块：TODO：先设定为5个
        Set<String> removeSet = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            String hash = tmp.remove(0).getKey();
            dataMap.remove(hash);
            removeSet.add(hash);
        }
        dataList.removeIf(block -> removeSet.contains(block.getHash()));
    }

    /**
     * 利用公式计算得分
     *
     * @param prevScore      前一次得分
     * @param millionseconds 最近被访问时间与当前时间的差值
     * @param distance       向量距离
     * @param frequency      被访问频率=访问次数/总访问次数
     * @return
     */
    private double getBlockScore(double prevScore, long millionseconds, double distance, double frequency) {
        return prevScore * Math.pow(millionseconds, PARAM_A) * Math.pow(distance, -PARAM_B) * Math.pow(frequency, PARAM_C);
    }

    /**
     * 根据区块内所有交易的向量计算出区块的向量
     * 即向量合成
     *
     * @param list
     * @return
     */
    private Map<String, Double> getBlockVector(List<IoVTransaction> list) {
        return list.stream().map(IoVTransaction::getKeywordsVector).flatMap(map -> map.entrySet().stream()).collect(Collectors.groupingBy(entry -> entry.getKey(), Collectors.summingDouble(entry -> entry.getValue())));
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setRoadSideUnitCMDThread(RoadSideUnitTCPThread roadSideUnitCMDThread) {
        this.roadSideUnitCMDThread = roadSideUnitCMDThread;
    }

    public Map<String, ResponseForIoV> getResultMap() {
        return resultMap;
    }

    public ConcurrentLinkedQueue<byte[]> getCarTaskQueue() {
        return carTaskQueue;
    }
}
