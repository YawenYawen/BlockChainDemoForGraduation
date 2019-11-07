package testVersion;

import java.util.*;
import java.util.stream.Collectors;

public class ProdPDCachingPolicyService {

    private static double MAX_PATH_VALUE = 100;

    public static void main(String[] args) throws Exception {
        ProdPDCachingPolicyService service = new ProdPDCachingPolicyService();

        List<Long> blockIdList = service.createBlocks();
        Map<Integer, Map<Long, Long>> heatTable = service.createHeatTable(blockIdList, 4);
        double[][] paths = service.createPaths(4);

        service.testCaching(4, 3, heatTable, paths, blockIdList, 1000L);
    }

    private List<Long> createBlocks() {
        List<Long> blockIdList = new ArrayList();

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
     * ProdPD缓存策略：利用一个可能性函数来判断节点是否缓存某个区块
     *
     * @param nodeNum
     * @param nodeCapacity
     * @param heatTable
     * @param paths
     * @param blockIdList
     * @param MAX_GENERATION
     * @throws Exception
     */
    public void testCaching(int nodeNum, int nodeCapacity, Map<Integer, Map<Long, Long>> heatTable, double[][] paths, List<Long> blockIdList, long MAX_GENERATION) throws Exception {
        long[][] nodeCaches = new long[nodeNum][nodeCapacity];
        //记录每一次迭代的延迟
        List<Double> latencySumList = new ArrayList();
        //由于区块不断被替换，所以需要保存一个下一次需要替换的位置；本数组内的数字范围是[0,nodeCapacity)
        int[] nextPos = new int[nodeNum];
        //记录最好的一次迭代
        int bestGeneration = 0;

        //热度比率:每个区块在每个节点的访问比率=该节点该区块访问次数/该节点总访问次数
        Map<Integer, Map<Long, Double>> heatRatioMap = heatTable.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> {
            double sum = (double) e.getValue().values().stream().mapToLong(l -> l).sum();
            Map<Long, Double> map = e.getValue().entrySet().stream().collect(Collectors.toMap(e1 -> e1.getKey(), e1 -> e1.getValue() / sum));
            return map;
        }));
        heatRatioMap.forEach((k, v) -> {
            double sum = v.values().stream().reduce(0d, (a, b) -> a + b);
            for (Long l : v.keySet()) {
                v.put(l, v.get(l) / sum);
            }
        });

        //可能性表：保存节点-区块-保存可能性
        //该map不断变化
        Map<Integer, Map<Long, Double>> possibilityMap = new HashMap<>();

        initNodeCaches(nodeCaches, blockIdList, latencySumList, heatTable, paths);

        Random random = new Random();
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
                        //只保存四个节点对同一个区块的保存概率
                        List<Double> possibilities = new ArrayList();
                        for (int i = 0; i < paths[nodeId].length; i++) {
                            //有区块并且比当前最小值小那么就更新
                            boolean hasBlock = containsBlock(nodeCaches[i], blockId);
                            if (hasBlock && paths[nodeId][i] < pathValue) {
                                pathValue = paths[nodeId][i];
                            }
                            //同时计算概率值，决定其他节点是否保留区块
                            double poss = calculatePossibility(paths[nodeId][i], heatRatioMap.get(nodeId).get(blockId));
                            System.out.println(poss);
                            possibilities.add(poss);
//                            boolean remainBlock = random.nextDouble() > poss;
//                            if (hasBlock && !remainBlock) {//不保留那就替换
//                                replaceBlock(nodeCaches[i], -1, blockIdList, nextPos, i, nodeCapacity);
//                            }
//                            if (!hasBlock && remainBlock) {//要增加这个区块进去
//                                replaceBlock(nodeCaches[i], blockId, blockIdList, nextPos, i, nodeCapacity);
//                            }
                        }
                        heatSum += pathValue;

                        //决定是否替换
                        double averagePoss = possibilities.stream().collect(Collectors.averagingDouble(a -> a)) / possibilities.size();
                        for (int n = 0; n < possibilities.size(); n++) {
                            boolean hasBlock = containsBlock(nodeCaches[n], blockId);
                            if (hasBlock && possibilities.get(n) < averagePoss) {//不保留那就替换
                                replaceBlock(nodeCaches[n], -1, blockIdList, nextPos, n, nodeCapacity);
                            }
                            if (!hasBlock && possibilities.get(n) >= averagePoss) {//要增加这个区块进去
                                replaceBlock(nodeCaches[n], blockId, blockIdList, nextPos, n, nodeCapacity);
                            }
                        }
                        possibilities.clear();
                    }
                }
            }
            if (heatSum < bestSum) {
                bestGeneration = gen;
                bestSum = heatSum;
            }

            latencySumList.add(heatSum);
        }

        ExperimentRecordUtils.printToExcel(latencySumList, "ProdPD");
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
     * 替换算法：1.有的替换成别的 2.没有的替换回来
     */
    private void replaceBlock(long[] nodeCache, long blockId, List<Long> blockIdList, int[] nextPos, int nodeId, int nodeCapacity) {
        Random random = new Random();
        if (blockId == -1) {//说明要换出去
            for (int i = 0; i < nodeCache.length; i++) {
                if (nodeCache[i] == blockId) {//找到这个位置然后替换
                    blockId = blockIdList.get(random.nextInt(blockIdList.size()));
                    nodeCache[nextPos[nodeId]] = blockId;
                }
            }
        } else {//换进来
            nodeCache[nextPos[nodeId]] = blockId;
        }
        nextPos[nodeId]++;
        if (nextPos[nodeId] >= nodeCapacity) {
            nextPos[nodeId] = 0;
        }
    }

    /**
     * 计算概率
     *
     * @param pathValue 路径代价
     * @param heatRatio 热度占比
     * @return
     */
    private double calculatePossibility(double pathValue, double heatRatio) {
        return (MAX_PATH_VALUE - pathValue) / MAX_PATH_VALUE * heatRatio;
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
