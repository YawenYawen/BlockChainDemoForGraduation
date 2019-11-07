package testVersion;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.net.Socket;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class CloudServer {
    private final Logger log = Logger.getLogger("CloudServer");

    private final String ROOT_PATH = this.getClass().getResource("").getPath();
    private final String SUFFIX = ".json";

    //数据
    //密钥文件
    private final String PUBLIC_KEY_PATH = ROOT_PATH + "/pubkeys.json";
    private final String PRIVATE_KEY_PATH = ROOT_PATH + "/prikeys.json";
    //车联网数据文件
    private final String DATA_PATH = ROOT_PATH + "/data.json";
    //命令行命令文件
    private final String CMD_PATH = ROOT_PATH + "/cmd.json";

    //打印路径代价
    private final String PRINT_PATH_0 = ROOT_PATH + "/print0.json";
    //打印rsu
    private final String PRINT_PATH_1 = ROOT_PATH + "/print1.json";

    //打印区块(四个文件)
    private final String PRINT_PATH_2 = ROOT_PATH + "/print2.json";
    private final String PRINT_PATH_3 = ROOT_PATH + "/print3.json";
    private final String PRINT_PATH_4 = ROOT_PATH + "/print4.json";
    private final String PRINT_PATH_5 = ROOT_PATH + "/print5.json";


    //下面的属性都与线程相同
    private Integer id;
    private String name;
    private String privateKey;
    private String publicKey;

    //自己的身份信息记录在哪里
    private String myBlockHash;//身份信息区块链hash值
    private Integer myTransactionIndex;//身份区块交易索引

    //路侧节点
    private static final Integer RSU_NUM = 2;//路侧节点总数
    private static final Integer RSU_CAPICITY = 20;//路侧节点容量
    private static final Integer CAR_NUM = 2;//车总数
    private static Map<Integer, String> rsuMap = new HashMap<>();//路侧节点的id和name
    private static Map<Integer, String> rsuIpMap = new HashMap<>();//路侧节点的id和ip
    //路侧节点之间的通信代价:路侧节点id从1开始，为了兼容所以路径包含一个假的0
    private static double[][] paths = new double[RSU_NUM + 1][RSU_NUM + 1];
    //热度表:每个区块在每个节点被请求的次数统计，节点id-区块id-次数
    private static Map<Integer, Map<Long, Long>> heatTable = new HashMap<>();

    //每种区块的交易条数：必须是2的幂次
    private static final Integer IDEN_NUM = 16;//身份信息
    private static final Integer DATA_NUM = 8;//数据信息
    private static final Integer VERIFY_NUM = 16;//验证区块
    private static final Integer DIFFICULTY = 1;//难度

    //关键词
    private static final Integer WORD_SIZE = 10;//关键词个数
    private static List<String> wordList = new ArrayList<>(WORD_SIZE);//关键词

    //数据区块链及其区块头链
    private List<Block> dataList = new LinkedList<>();
    private Map<String, Block> dataMap = new HashMap<>();
    private List<Block> dataVerifyList = new LinkedList<>();

    //身份信息区块链及其区块头链
    private List<Block> identityList = new LinkedList<>();
    private List<Block> identityVerifyList = new LinkedList<>();

    //每次请求的key和结果
    private Map<String, Object[]> resultMap = new HashMap<>();

    private CloudServerTCPThread cloudServerSendThread;
    private CloudServerTCPThread cloudServerReceiveThread;

    private ObjectMapper mapper = new ObjectMapper();

    //最小距离阈值，超过这个值就说明距离太远不能采用
    //云服务器的阈值会偏大
    private static final double THRESHHOLD = 0.7;

    public CloudServer(Integer id, String name) {
        this.id = id;
        this.name = name;

        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
    }

    /**
     * 云服务器主动发送指令
     */
    public void sendConstructions(int ins) throws Exception {
        String requestKey = "";
        String requestIndex = "";
        //项目启动流程：
        //0 :启动云服务器，初始化密钥文件和数据文件 + 初始化区块链 + 生成命令行命令文件
        //启动路侧节点
        //启动车辆
        //1 :给所有RSU分配车联网区块, 给车辆分配区块头
        switch (ins) {
            case -1://简单初始化
                dataList = (List) FileUtils.deserialization(PRINT_PATH_2);
                dataMap = dataList.stream().collect(Collectors.toMap(Block::getHash, Function.identity()));
                dataVerifyList = (List) FileUtils.deserialization(PRINT_PATH_3);
                System.out.println("init data block finished");

                identityList = (List) FileUtils.deserialization(PRINT_PATH_4);
                identityVerifyList = (List) FileUtils.deserialization(PRINT_PATH_5);
                System.out.println("init identity block finished");

                intRSU();
                System.out.println("init rsu finished");
                //TODO:有的时候就报错,单独测试过TestMain一切正常说明是因为和本类叠加所致
//                paths = (double[][]) FileUtils.deserialization(PRINT_PATH_0);
                initRSUPath();
                System.out.println("init rsu path finished");

                List<String> priKeys = FileUtils.readFileByline(PRIVATE_KEY_PATH);
                List<String> pubKeys = FileUtils.readFileByline(PUBLIC_KEY_PATH);
                //第一个是云服务器自己；默认都在第一个区块里面
                privateKey = priKeys.get(0);
                publicKey = pubKeys.get(0);
                myBlockHash = identityList.get(0).getHash();
                myTransactionIndex = 0;

                break;
            case 0://初始化数据，包括密钥数据和车联网数据
                //随机生成密钥
                initKeys();
                System.out.println("creating key finished");

                //初始化关键词
                initWords();
                //随机生成车联网数据
                initDatas();
                System.out.println("creating data finished");

                //挖矿
                initDataBlocks();
                System.out.println("init data block finished");
                initIdentityBlocks();
                System.out.println("init identity block finished");

                //初始化路侧节点id和名字
                intRSU();
                System.out.println("init rsu finished");
                initRSUPath();
                System.out.println("init rsu path finished");

                //生成命令行文件:全部都是运行命令，因为编译在我自己电脑上做好了
                String currentPath = this.getClass().getResource("").getPath().replace("cs", "");//当前文件所在目录
                List<String> cmds = new ArrayList<>(RSU_NUM + CAR_NUM);
                priKeys = FileUtils.readFileByline(PRIVATE_KEY_PATH);
                pubKeys = FileUtils.readFileByline(PUBLIC_KEY_PATH);
                //第一个是云服务器自己；默认都在第一个区块里面
                privateKey = priKeys.get(0);
                publicKey = pubKeys.get(0);
                myBlockHash = identityList.get(0).getHash();
                myTransactionIndex = 0;
                for (int i = 1; i <= RSU_NUM + CAR_NUM; i++) {
                    String blockHash = identityList.get(i / IDEN_NUM).getHash();
                    int tranIndex = i % IDEN_NUM;
                    if (i > RSU_NUM) {
                        //生成车的
                        cmds.add("java -Djava.ext.dirs=" + currentPath + ":$JAVA_HOME/jre/lib/ext -Djava.net.preferIPv4Stack=true VehicleThread " + i + " VEHICLE" + i + " " + tranIndex + " " + blockHash + " " + priKeys.get(i) + " " + pubKeys.get(i));
                        continue;
                    }
                    //生成路侧节点的
                    //TODO:暂时都是一个组的
                    cmds.add("java -Djava.ext.dirs=" + currentPath + ":$JAVA_HOME/jre/lib/ext -Djava.net.preferIPv4Stack=true RoadSideUnitThread " + i + " RSU" + i + " " + 0 + " " + tranIndex + " " + blockHash + " " + priKeys.get(i) + " " + pubKeys.get(i));
                }
                FileUtils.printToFileByline(CMD_PATH, cmds);
                System.out.println("init cmd file finished");
                break;
            case 1://发布区块
                System.out.println(System.currentTimeMillis() + "/send block start");

                requestKey = "CLOUD_DATA_BLOCK_ALLOCATING_1";
                //初始化就平均分,每个路侧节点分到size个区块
                int size = dataList.size() / RSU_NUM;
                initSendBlocksSlowly(dataList, size, requestKey);
                System.out.println("1 finished");
                break;
            case 2:
                System.out.println(System.currentTimeMillis() + "/send block start");

                requestKey = "CLOUD_VERIFY_BLOCK_ALLOCATING_1";
                //所有路侧节点都要保存一份
                requestIndex = "ALL";
                initSendBlocksSlowly(dataVerifyList, requestKey, requestIndex);
                System.out.println("2 finished");
                break;
            case 3:
                System.out.println(System.currentTimeMillis() + "/send block start");

                requestKey = "CLOUD_DATA_BLOCK_ALLOCATING_2";
                //所有路侧节点都要保存一份
                requestIndex = "ALL";
                initSendBlocksSlowly(identityList, requestKey, requestIndex);
                System.out.println("3 finished");
                break;
            case 4:
                System.out.println(System.currentTimeMillis() + "/send block start");

                requestKey = "CLOUD_VERIFY_BLOCK_ALLOCATING_2";
                //所有路侧节点都要保存一份
                requestIndex = "ALL";
                initSendBlocksSlowly(identityVerifyList, requestKey, requestIndex);
                System.out.println("4 finished");
                break;
            case 5:
                System.out.println(System.currentTimeMillis() + "/send block start");

                requestKey = "VEHICLE_BLOCK_HEAD";
                requestIndex = "ALL";//所有车都要接受
                Map<String, String> data1 = dataVerifyList.stream().collect(Collectors.toMap(b -> b.getHash(), b -> b.getMerkleTree().get(0), (key1, key2) -> key1, LinkedHashMap::new));
                Map<String, String> data2 = identityVerifyList.stream().collect(Collectors.toMap(b -> b.getHash(), b -> b.getMerkleTree().get(0), (key1, key2) -> key1, LinkedHashMap::new));
                for (String ip : rsuIpMap.values()) {
                    sendRequestToOthers(ip, requestKey, requestIndex, true, new Map[]{data1, data2});
                }
                System.out.println("5 finished");
                break;//459b e7b5
            case 6:
                break;//开始挖矿
            case 7://打印区块、路侧节点、自己
                printBlocks();
                System.out.println("7-1 finished");
                printRSU();
                System.out.println("7-2 finished");
                printPaths();
                System.out.println("7-3 finished");

                System.out.println(id);
                System.out.println(name);
                System.out.println(myBlockHash);
                System.out.println(publicKey);
                System.out.println(privateKey);
                System.out.println(myTransactionIndex);
                break;
            case 8:
                //运行遗传算法
                GeneticAlgorithmService service = new GeneticAlgorithmService();
                List<Long> blockIdList = LongStream.range(0, dataList.size()).boxed().collect(Collectors.toList());
                //用于补全的
                Map<Long, Long> map = new LinkedHashMap<>();

                //路侧节点id从1开始，但是为了兼容遗传算法，所以添加一个假的0作为开头

                //必要数据
                for (int i = 0; i < dataList.size(); i++) {
                    map.put((long) i, 0L);
                }

                //补全热度表
                for (int i = 0; i <= RSU_NUM; i++) {
                    if (!heatTable.containsKey(i)) {
                        //热度表从0开始
                        heatTable.put(i, new LinkedHashMap<>(map));
                    }
                }
                System.out.println(heatTable);

                long[][] bestPopulation = service.startGeneticAlgorith(RSU_NUM + 1, RSU_CAPICITY, 10000, heatTable, null, paths, blockIdList, 1000L, 0.4, 0.5);

                //RSU的id-应该分配的区块
                Map<Integer, List<Block>> bestPopulationMap = new LinkedHashMap<>();
                for (int i = 1; i < bestPopulation.length; i++) {
                    bestPopulationMap.put(i, Arrays.stream(bestPopulation[i]).boxed().sorted().map(l -> dataList.get(Integer.parseInt(l + ""))).collect(Collectors.toList()));
                }

                //下面开始重新分配区块
                for (int i = 1; i <= RSU_NUM; i++) {
                    sendBlocksSlowly(rsuIpMap.get(i), bestPopulationMap.get(i), "CLOUD_DATA_BLOCK_ALLOCATING_1", i);
                }
                System.out.println("8 finished");
                break;
        }
    }


    /**
     * 按照遗传算法计算出来的发送数据区块
     * 分配区块要慢一些
     */
    private void sendBlocksSlowly(String ip, List<Block> blockList, String requestKey, int rsuIndex) throws Exception {
        String requestIndex = rsuMap.get(rsuIndex) + rsuIndex + "_" + System.currentTimeMillis();//name+id
        //慢点发，否则就遗失了
        for (int j = 0; j < blockList.size(); j += 2) {//一个块一个块发；如果两个两个发，那就要保证size是2的倍数，一次类推
            if (j == 0) {
                sendRequestToOthers(ip, requestKey, requestIndex, true, blockList.subList(j, j + 2).stream().map(Block::getHash).collect(Collectors.toSet()));
            } else {
                sendRequestToOthers(ip, requestKey, requestIndex, false, blockList.subList(j, j + 2).stream().map(Block::getHash).collect(Collectors.toSet()));
            }
            Thread.sleep(2000);
        }
    }

    /**
     * 平均发送数据区块
     * 分配区块要慢一些
     */
    private void initSendBlocksSlowly(List<Block> blockList, int size, String requestKey) throws Exception {
        for (int i = 1; i <= RSU_NUM; i++) {
            String requestIndex = rsuMap.get(i) + i + "_" + System.currentTimeMillis();//name+id
//                    sendRequestToOthers(requestKey, requestIndex, dataList.subList(i * size, (i + 1) * size).stream().map(Block::getHash).collect(Collectors.toSet()));
            //慢点发，否则就遗失了
            for (int j = (i - 1) * size; j < i * size; j += 2) {//一个块一个块发；如果两个两个发，那就要保证size是2的倍数，一次类推
                if (j == 0) {
                    sendRequestToOthers(rsuIpMap.get(i), requestKey, requestIndex, true, blockList.subList(j, j + 2).stream().map(Block::getHash).collect(Collectors.toSet()));
                } else {
                    sendRequestToOthers(rsuIpMap.get(i), requestKey, requestIndex, false, blockList.subList(j, j + 2).stream().map(Block::getHash).collect(Collectors.toSet()));
                }
                Thread.sleep(2000);
            }
        }
    }

    /**
     * 发送全部区块
     * 分配区块要慢一些
     */
    private void initSendBlocksSlowly(List<Block> blockList, String requestKey, String requestIndex) throws Exception {
        //慢点发，否则就遗失了
        for (int i = 1; i <= RSU_NUM; i++) {

            for (int j = 0; j < blockList.size(); j += 2) {//一个块一个块发
                if (j == 0) {
                    sendRequestToOthers(rsuIpMap.get(i), requestKey, requestIndex, true, blockList.subList(j, j + 2).stream().map(Block::getHash).collect(Collectors.toSet()));
                } else {
                    sendRequestToOthers(rsuIpMap.get(i), requestKey, requestIndex, false, blockList.subList(j, j + 2).stream().map(Block::getHash).collect(Collectors.toSet()));
                }
                Thread.sleep(2000);
            }
        }
    }

    private void initKeys() throws Exception {
        //先生成路侧节点的key
        int total = (RSU_NUM + CAR_NUM) * IDEN_NUM * VERIFY_NUM;
        List<String> pubKeys = new ArrayList<>(total);
        List<String> priKeys = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            String key[] = EncryptUtils.genKeyPair(null);
            //一行公钥一行私钥
            priKeys.add(key[0]);//私钥
            pubKeys.add(key[1]);//公钥
        }

        FileUtils.printToFileByline(PUBLIC_KEY_PATH, pubKeys);
        FileUtils.printToFileByline(PRIVATE_KEY_PATH, priKeys);
    }

    /**
     * 初始化关键词：a,b,c,d,...
     */
    private void initWords() {
        for (int i = 0; i < WORD_SIZE; i++) {
            wordList.add(String.valueOf((char) ('a' + i)));
        }
    }

    private void initDatas() throws Exception {
        int total = (RSU_NUM + CAR_NUM) * DATA_NUM * VERIFY_NUM;
        //车联网数据
        List<String> datas = new ArrayList<>(total);
        //每一条车联网数据对应的向量
        List<String> vectors = getRandomVectors(total);
        for (int i = 0; i < total; i++) {
            datas.add("Internet of Vehicle Test Data" + i + ";" + vectors.get(i));
        }

        FileUtils.printToFileByline(DATA_PATH, datas);
    }

    /**
     * a 0.1;b 0.2 这样形式的初始化
     *
     * @return
     */
    private List<String> getRandomVectors(int total) {
        List<String> vectors = new ArrayList<>(total);
        Random random = new Random();
        for (int i = 0; i < total; i++) {
            vectors.add(wordList.stream().map(word -> word + " " + random.nextDouble()).collect(Collectors.joining(";")));
        }
        return vectors;
    }


    private void printRSU() throws Exception {
        FileUtils.printToFile(PRINT_PATH_1, mapper.writeValueAsString(rsuMap));
    }

    private void printPaths() throws Exception {
        FileUtils.printToFile(PRINT_PATH_0, mapper.writeValueAsString(paths));
    }

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
     * 用requestKey来判断是哪种请求
     * 用requestIndex来判断是哪次请求
     *
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

        switch (requestKey) {
            case "UNIT_BLOCK_REQUEST_TO_CS"://区块请求
                System.out.println(System.currentTimeMillis() + "/cs receive rsu block request");
                ResponseForIoV response = findBestBlock1(((ArrayList<String>) map.get("data")).toArray(new String[]{}));
                //没找到就不返回
                if (response.getDataBlockHash() != null) {
                    setData(response);
                    System.out.println(System.currentTimeMillis() + "/cs return rsu block request");
                    sendRequestToOthers(socket, "UNIT_BLOCK_RESPONSE_FROM_CS", requestIndex, false, response);
                }
                break;
            case "MINE_START_1"://车联网信息挖矿
                break;
            case "MINE_START_2"://身份信息挖矿
                break;
            case "CLOUD_DATA_BLOCK_ALLOCATING_REQUEST_1"://分配车联网数据区块请求
                Set<String> newHashes = new HashSet<>((List) map.get("data"));//路侧节点请求的区块
                List<Block> resultList = dataList.stream().filter(b -> newHashes.contains(b.getHash())).collect(Collectors.toList());
                sendRequestToOthers(socket, "CLOUD_DATA_BLOCK_ALLOCATING_RESPONSE_1", requestIndex, false, resultList);
                break;
            case "CLOUD_VERIFY_BLOCK_ALLOCATING_REQUEST_1"://分配车联网验证数据区块请求
                newHashes = new HashSet<>((List) map.get("data"));//路侧节点请求的区块
                resultList = dataVerifyList.stream().filter(b -> newHashes.contains(b.getHash())).collect(Collectors.toList());
                sendRequestToOthers(socket, "CLOUD_VERIFY_BLOCK_ALLOCATING_RESPONSE_1", requestIndex, false, resultList);
                break;
            case "CLOUD_DATA_BLOCK_ALLOCATING_REQUEST_2"://分配身份信息数据区块请求
                newHashes = new HashSet<>((List) map.get("data"));//路侧节点请求的区块
                resultList = identityList.stream().filter(b -> newHashes.contains(b.getHash())).collect(Collectors.toList());
                sendRequestToOthers(socket, "CLOUD_DATA_BLOCK_ALLOCATING_RESPONSE_2", requestIndex, false, resultList);
                break;
            case "CLOUD_VERIFY_BLOCK_ALLOCATING_REQUEST_2"://分配身份信息验证数据区块请求
                newHashes = new HashSet<>((List) map.get("data"));//路侧节点请求的区块
                resultList = identityVerifyList.stream().filter(b -> newHashes.contains(b.getHash())).collect(Collectors.toList());
                sendRequestToOthers(socket, "CLOUD_VERIFY_BLOCK_ALLOCATING_RESPONSE_2", requestIndex, false, resultList);
                break;
            case "SUBMIT_HEAT_TABLE":
                Map<String, Integer> rsuHeatMap = (Map) map.get("data");
                Map<Long, Long> heatMap = new HashMap<>();
                for (Map.Entry<String, Integer> entry : rsuHeatMap.entrySet()) {
                    heatMap.put((long) (dataList.indexOf(dataMap.get(entry.getKey()))), (long) entry.getValue());
                }
                heatTable.put(Integer.parseInt(requestIndex), heatMap);
                break;
        }
    }

    /**
     * 已知关键词找区块
     *
     * @param words
     * @return
     */
    private ResponseForIoV findBestBlock1(String[] words) {
        ResponseForIoV response = new ResponseForIoV();
        response.setMinDis(Double.MAX_VALUE);

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
     * 计算向量距离，没有对应关键词的而不计入计算
     *
     * @param v1 两个向量大小相同
     * @param v2
     */
    private double calculateVectorDistance(Object[][] v1, Object[][] v2) {
        double result = 0;
        for (int i = 0; i < v1.length; i++) {
            result += Math.pow((Integer) v1[i][1] - (Integer) v2[i][i], 2);
        }
        return Math.pow(result, 0.5);//开根号
    }


    /**
     * 向路侧节点发送数据
     *
     * @param requestKey   请求类型
     * @param requestIndex 请求序号
     * @param flag         在分配区块的时候标识是否需要把之前的区块清空,如果是其他的命令就忽略
     */
    private void sendRequestToOthers(String ip, String requestKey, String requestIndex, boolean flag, Object data) throws Exception {
        Map<String, Object> requestDataMap = new HashMap<>();
        requestDataMap.put("requestKey", requestKey);
        requestDataMap.put("requestIndex", requestIndex);
        requestDataMap.put("flag", flag);
        requestDataMap.put("data", data);
        cloudServerSendThread.send(ip, ExpressUtils.deflate(mapper.writeValueAsBytes(requestDataMap)));
    }

    private void sendRequestToOthers(Socket socket, String requestKey, String requestIndex, boolean flag, Object data) throws Exception {
        Map<String, Object> requestDataMap = new HashMap<>();
        requestDataMap.put("requestKey", requestKey);
        requestDataMap.put("requestIndex", requestIndex);
        requestDataMap.put("flag", flag);
        requestDataMap.put("data", data);
        cloudServerSendThread.send(socket, ExpressUtils.deflate(mapper.writeValueAsBytes(requestDataMap)));
    }

    /**
     * 初始化区块：身份信息
     */
    private void initIdentityBlocks() throws Exception {
        //从文件中读出来所有的公钥
        List<String> publicKeys = EncryptUtils.loadDatasFromFile(PUBLIC_KEY_PATH);
        //跨行读，因为只把公钥写入区块
        mine(publicKeys, identityList, identityVerifyList, IDEN_NUM, VERIFY_NUM, 2);

        //写入文件
        FileUtils.serialization(identityList, PRINT_PATH_4);
        FileUtils.serialization(identityVerifyList, PRINT_PATH_5);
    }

    /**
     * 初始化区块：车联网数据信息
     */
    private void initDataBlocks() throws Exception {
        //从文件中读出来所有的数据
        List<String> IoVDatas = EncryptUtils.loadDatasFromFile(DATA_PATH);
        mine(IoVDatas, dataList, dataVerifyList, DATA_NUM, VERIFY_NUM, 1);
        dataMap = dataList.stream().collect(Collectors.toMap(Block::getHash, Function.identity()));

        //写入文件
        FileUtils.serialization(dataList, PRINT_PATH_2);
        FileUtils.serialization(dataVerifyList, PRINT_PATH_3);
    }

    //初始化所有路侧节点的id和名字
    private void intRSU() {
        for (int i = 1; i <= RSU_NUM; i++) {
            rsuMap.put(i, "RSU" + i);
            rsuIpMap.put(i, "192.168.1.10" + i);
        }
    }

    private void initRSUPath() throws Exception {
        Random random = new Random();
        //从1开始
        for (int i = 1; i < paths.length; i++) {
            for (int j = 1; j <= i; j++) {
                if (i == j) {//自我访问有一点点的代价
                    paths[i][j] = random.nextDouble();
                }
                paths[i][j] = random.nextDouble() * 10 + 1;//11以内的
                paths[j][i] = paths[i][j];
            }
        }
        //第0个是假的，所以初始化为距离很大
        for (int i = 0; i < paths.length; i++) {
            paths[0][i] = 100;
            paths[i][0] = 100;
        }
        FileUtils.serialization(paths, PRINT_PATH_0);
    }

    /**
     * 挖矿，即创建区块
     *
     * @param mineDatas 待挖矿数据
     * @param list1     数据列表
     * @param list2     验证列表
     * @param num1      数据列表区块容量
     * @param num2      验证列表区块容量
     */
    private void mine(List<String> mineDatas, List<Block> list1, List<Block> list2, int num1, int num2, int type) {
        //所有的数据区块SHA256(hash+merkle根)，用于创建验证区块
        List<String> verifyBlockTransactionList = new ArrayList<>(num2);

        //数据区块前一个区块hash值
        String prevHash = "0000000000000000000000000000000000000000000000000000000000000000";
        //验证区块前一个区块hash值
        String prevVerifyHash = "0000000000000000000000000000000000000000000000000000000000000000";

        //将车联网数据和其对应的向量分开
        List<String> realDatas = null;
        List<Map<String, Double>> vectors = null;
        if (type == 1) {
            realDatas = mineDatas.stream().map(data -> data.substring(0, data.indexOf(";"))).collect(Collectors.toList());
            vectors = mineDatas.stream().map(data -> {
                String[] lines = data.substring(data.indexOf(";") + 1).split(";");
                return Arrays.asList(lines).stream().collect(Collectors.toMap(line -> line.split(" ")[0], line -> Double.valueOf(line.split(" ")[1])));
            }).collect(Collectors.toList());
        } else if (type == 2) {
            //身份信息没有向量
            realDatas = mineDatas;
            vectors = null;
        }


        //每条身份信息都是一条交易+向量
        int index = 0;//数据区块索引
        List<IoVTransaction> dataTransactions = new ArrayList<>(num1);
        for (int i = 0; i < realDatas.size(); i++) {
            IoVTransaction dataTransaction = null;
            if (type == 1) {
                dataTransaction = new IoVTransaction(type, realDatas.get(i), null, vectors.get(i));
            } else if (type == 2) {
                dataTransaction = new IoVTransaction(type, null, realDatas.get(i), null);//身份信息没有向量
            }
            dataTransactions.add(dataTransaction);

            if (dataTransactions.size() == num1) {
                //交易到数量了就创建区块
                Block block = new Block(prevHash, "1.0", dataTransactions, DIFFICULTY);
                //每一条验证区块的交易内容是数据区块的hash+其merkle根
                verifyBlockTransactionList.add(block.getHash() + block.getMerkleTree().get(0));
                prevHash = block.getHash();
                list1.add(block);
                dataTransactions = new ArrayList<>(num1);
                block.setVerifyBlockIndex(index / num2);
                block.setVerifyTranIndex(index % num2);
                index++;
                setMerkelPathAndBlockHash(block);
            }

            if (verifyBlockTransactionList.size() == num2) {
                //区块到数量了就创建验证区块
                //！！！验证区块的每一条交易数据是数据区块hash值+数据区块merkel根
                Block block = new Block(prevVerifyHash, "2.0", verifyBlockTransactionList.stream().map(b -> new IoVTransaction(type + 2, b, null, null)).collect(Collectors.toList()), DIFFICULTY);
                prevVerifyHash = block.getHash();
                list2.add(block);
                verifyBlockTransactionList = new ArrayList<>(num2);
                setMerkelPathAndBlockHash(block);
            }
        }
    }

    //验证区块设置merkle树和父区块hash值
    private void setMerkelPathAndBlockHash(Block block) {
        for (int j = 0; j < block.getData().size(); j++) {
            block.getData().get(j).setBlockHash(block.getHash());
            block.getData().get(j).setMerklePath(getMerklePath(block.getMerkleTree(), j + block.getData().size() - 1));
        }
    }


    /**
     * 得到某笔交易的merkle路径
     * 路径详见ppt 共2*logn个节点
     *
     * @param merkleTree 所有交易的hash
     * @param index      该交易的索引
     * @return
     */
    private List<String> getMerklePath(List<String> merkleTree, int index) {
        List<String> path = new LinkedList<>();
        int tmp = index;
        while (tmp > 0) {
            if (tmp % 2 == 0) {
                path.add(merkleTree.get(tmp - 1));
                path.add(merkleTree.get(tmp));
                tmp = (tmp - 2) / 2;
            } else {
                path.add(merkleTree.get(tmp));
                path.add(merkleTree.get(tmp + 1));
                tmp = (tmp - 1) / 2;
            }
        }
        return path;
    }

    public void setCloudServerSendThread(CloudServerTCPThread cloudServerSendThread) {
        this.cloudServerSendThread = cloudServerSendThread;
    }

    public void setCloudServerReceiveThread(CloudServerTCPThread cloudServerReceiveThread) {
        this.cloudServerReceiveThread = cloudServerReceiveThread;
    }

    public Integer getId() {
        return id;
    }
}
