package testVersion;

import java.util.*;

public class LCDCachingPolicyService {

    private static double MAX_PATH_VALUE = 100;

    public static void main(String[] args) throws Exception {
        LCDCachingPolicyService service = new LCDCachingPolicyService();

        List<Long> blockIdList = service.createBlocks();
        Map<Integer, Map<Long, Long>> heatTable = service.createHeatTable(blockIdList, 4);
        double[][] paths = service.createPaths(4);

        service.testCaching(4, 3, heatTable, paths, blockIdList, 1000L);
    }

    private List<Long> createBlocks() {
        List<Long> blockIdList = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
//            EthereumBlock block = new EthereumBlock();
//            block.setId((long) (i));
//            block.setHash("区块" + i);
//            ethereumBlockRepository.save(block);
            blockIdList.add((long) i);
        }

        return blockIdList;

    }

    private Map<Integer, Map<Long, Long>> createHeatTable(List<Long> blockIdList, int nodeNum) {
//        List<Long> blockIdList = ethereumBlockRepository.findAllIds();

        Map<Integer, Map<Long, Long>> heatTable = new HashMap<>();

        Map<Long, Long> map = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            map.put((long) i, (long) Math.pow(2, i % 5));
        }


        heatTable.put(0, map);

        for (int i = 1; i < nodeNum; i++) {
            map = new HashMap<>();
            heatTable.put(i, map);
        }

        return heatTable;
    }

    private double[][] createPaths(int nodeNum) {
        Random random = new Random();
        double[][] paths = new double[nodeNum][nodeNum];
        for (int i = 0; i < nodeNum; i++) {
            for (int j = 0; j <= i; j++) {
                if (i == j) {//自我访问有一点点的代价
                    paths[i][j] = random.nextDouble();
                }
                paths[i][j] = random.nextDouble() * 10 + 1;
                paths[j][i] = paths[i][j];
            }
        }

        return paths;
    }

    /**
     * LCD缓存策略：请求消息命中时，命中节点会向前传递缓存，进而消息会逐渐向源靠拢
     * 此处设定为没找到缓存会向中央服务器请求，统一延时MAX_PATH_VALUE
     */
    public void testCaching(int nodeNum, int nodeCapacity, Map<Integer, Map<Long, Long>> heatTable, double[][] paths, List<Long> blockIdList, long MAX_GENERATION) throws Exception {
        long[][] nodeCaches = new long[nodeNum][nodeCapacity];
        //记录每一次迭代的延迟
        List<Double> latencySumList = new ArrayList<>();
        //由于区块不断被替换，所以需要保存一个下一次需要替换的位置；本数组内的数字范围是[0,nodeCapacity)
        int[] nextPos = new int[nodeNum];
        //记录最好的一次迭代
        int bestGeneration = 0;

        initNodeCaches(nodeCaches, blockIdList, latencySumList, heatTable, paths);

        //记录上一次最好的，用于比较
        double bestSum = latencySumList.get(0);
        for (int gen = 0; gen < MAX_GENERATION; gen++) {
            double heatSum = 0;
            for (Map.Entry<Integer, Map<Long, Long>> entry : heatTable.entrySet()) {
                int nodeId = entry.getKey();
                Map<Long, Long> heatMap = entry.getValue();

                for (Map.Entry<Long, Long> entry1 : heatMap.entrySet()) {
                    long blockId = entry1.getKey();
                    long heat = entry1.getValue();
                    for (int h = 0; h < heat; h++) {//访问heat次
                        if (containsBlock(nodeCaches[nodeId], blockId)) {
                            continue;//自己有区块延迟就是0
                        }
                        double pathValue = MAX_PATH_VALUE;
                        for (int i = 0; i < paths[nodeId].length; i++) {
                            //有区块并且比当前最小值小那么就更新
                            if (containsBlock(nodeCaches[i], blockId) && paths[nodeId][i] < pathValue) {
                                pathValue = paths[nodeId][i];
                            }
                        }
                        //替换
                        nodeCaches[nodeId][nextPos[nodeId]] = blockId;
                        nextPos[nodeId]++;
                        if (nextPos[nodeId] >= nodeCapacity) {
                            nextPos[nodeId] = 0;//到头了就从0开始
                        }
                        heatSum += pathValue;
                    }

                }
            }
            if (heatSum < bestSum) {
                bestGeneration = gen;
                bestSum = heatSum;
            }

            latencySumList.add(heatSum);
        }

        ExperimentRecordUtils.printToExcel(latencySumList, "LCD");
        printHeatTable(heatTable);
        System.out.println("-------------------");
        printPath(paths);
        System.out.println("-------------------");
        printIndividual(nodeCaches);
        System.out.println("-------------------");
        System.out.println(bestSum);
        System.out.println("-------------------");
        System.out.println(bestGeneration);

    }

    /**
     * 初始化节点缓存
     */
    private void initNodeCaches(long[][] nodeCaches, List<Long> blockIdList, List<Double> latencySumList, Map<Integer, Map<Long, Long>> heatTable, double[][] paths) {
        Random random = new Random();
        int size = blockIdList.size();
        for (long[] longs : nodeCaches) {
            for (int i = 0; i < longs.length; i++) {
                longs[i] = blockIdList.get(random.nextInt(size));
            }
        }
        double heatSum = calculateLatency(nodeCaches, heatTable, paths);
        latencySumList.add(heatSum);
    }

    /**
     * 计算总延迟
     *
     * @return
     */
    private double calculateLatency(long[][] nodeCaches, Map<Integer, Map<Long, Long>> heatTable, double[][] paths) {
        double heatSum = 0;
        for (Map.Entry<Integer, Map<Long, Long>> entry : heatTable.entrySet()) {
            int nodeId = entry.getKey();
            Map<Long, Long> heatMap = entry.getValue();

            for (Map.Entry<Long, Long> entry1 : heatMap.entrySet()) {
                long blockId = entry1.getKey();
                long heat = entry1.getValue();
                if (containsBlock(nodeCaches[nodeId], blockId)) {
                    continue;//自己有区块延迟就是0
                }
                double pathValue = MAX_PATH_VALUE;
                for (int i = 0; i < paths[nodeId].length; i++) {
                    //有区块并且比当前最小值小那么就更新
                    if (containsBlock(nodeCaches[i], blockId) && paths[nodeId][i] < pathValue) {
                        pathValue = paths[nodeId][i];
                    }
                }
                heatSum += (pathValue * heat);
            }
        }

        return heatSum;
    }

    /**
     * 判断一个节点是否含有区块
     *
     * @return
     */
    private boolean containsBlock(long[] node, long blockId) {
        for (long l : node) {
            if (l == blockId) {
                return true;
            }
        }
        return false;
    }


    /**
     * 打印个体
     */
    private void printIndividual(long[][] individual) {
        for (long[] longs : individual) {
            for (long o : longs) {
                System.out.print(o + " ");
            }
            System.out.println();
        }
    }

    /**
     * 打印路径
     */
    private void printPath(double[][] individual) {
        for (double[] doubles : individual) {
            for (double o : doubles) {
                System.out.print(o + " ");
            }
            System.out.println();
        }
    }

    /**
     * 打印热度表
     */
    private void printHeatTable(Map<Integer, Map<Long, Long>> heatTable) {
        heatTable.forEach((k, v) -> {
            System.out.println(k + ":");
            v.forEach((k1, v1) -> {
                System.out.println(k1 + "-" + v1);
            });
            System.out.println("***");
        });
    }
}
