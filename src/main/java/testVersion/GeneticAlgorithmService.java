package testVersion;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 考虑链路代价的所有节点的遗传算法
 */
public class GeneticAlgorithmService {

    private final Logger log = Logger.getLogger("GeneticAlgorithmService");

    //最大链路代价
    private static double MAX_PATH_VALUE = 100;


    /**
     * 执行遗传算法，并更新本地数据库
     *
     * @param nodeNum        个体数 / 路测节点数
     * @param nodeCapacity   个体容量 / 每个节点的数据库中可以存存储多少个区块
     * @param individualNum  种群大小 / 初始化多少个个体
     * @param heatTable      热度表 / 每个区块在每个节点被请求的次数统计，节点id-区块id-次数
     * @param heatSum        热度统计表 / 每个节点被请求的次数总计，节点id-总次数
     * @param paths          两两节点之间的链路代价
     * @param MAX_GENERATION 最高代数 / 最多迭代多少次
     * @param crossRate      交叉概率：把某个区块随机移动/复制到相邻节点上；节点随机替换某个区块
     * @param mutaRate       变异概率：随机两个个体替换部分片段，注意是对应的片段才能互换
     */
    public long[][] startGeneticAlgorith(int nodeNum, int nodeCapacity, int individualNum, Map<Integer, Map<Long, Long>> heatTable, Map<Integer, Long> heatSum, double[][] paths, List<Long> blockIdList, long MAX_GENERATION, double crossRate, double mutaRate) throws Exception {
        //所有的区块id
        int blockNum = blockIdList.size();//应满足blockNum>>nodeCapacity

        //初始种群，父代种群，行数表示种群规模，一行代表一个个体，即染色体，列表示染色体基因片段
        //个体id-节点id-区块id序列
        long[][][] oldPopulation = new long[individualNum][nodeNum][nodeCapacity];
        //新的种群，子代种群
        //个体id-节点id-区块id序列
        long[][][] newPopulation = new long[individualNum][nodeNum][nodeCapacity];
        //最好的一代
        long[][] bestPopulation = new long[nodeNum][nodeCapacity];

        //种群适应度，表示种群中各个个体的适应度
        //每一个数字代表每个节点在相对应的热度表下的对每一个区块的适应度,
        //即节点id-区块id-适应度，即延迟
        //list即代表不同个体
        List<Map<Integer, Map<Long, Double>>> oldFitness = new ArrayList(individualNum);//Maps.newHashMapWithExpectedSize(nodeNum);
        List<Map<Integer, Map<Long, Double>>> newFitness = new ArrayList(individualNum);
        //最好的适应度
        Map<Integer, Map<Long, Double>> bestFitness = new HashMap<>();

        //曾经出现过的最好的适应度记录
        List<Map<Integer, Map<Long, Double>>> bestFitnessRecordList = new ArrayList<>();
        List<long[][]> bestIndividualRecordList = new ArrayList<>();

        return evolution1(oldPopulation, newPopulation, bestPopulation, bestIndividualRecordList, blockIdList, oldFitness, newFitness, bestFitness, bestFitnessRecordList, paths, heatTable, MAX_GENERATION, crossRate, mutaRate);
    }

    /**
     * 迭代算法1
     */
    private long[][] evolution1(long[][][] oldPopulation,
                                long[][][] newPopulation,
                                long[][] bestPopulation,
                                List<long[][]> bestIndividualRecordList,
                                List<Long> blockIdList,
                                List<Map<Integer, Map<Long, Double>>> oldFitness,
                                List<Map<Integer, Map<Long, Double>>> newFitness,
                                Map<Integer, Map<Long, Double>> bestFitness,
                                List<Map<Integer, Map<Long, Double>>> bestFitnessRecordList,
                                double[][] paths,
                                Map<Integer, Map<Long, Long>> heatTable,
                                long MAX_GENERATION,
                                double crossRate,
                                double mutaRate) throws Exception {

        //迭代算法1使用延迟加和的方式计算适应度，这个列表是保存历代最好适应度值
        List<Double> bestFitnessValueList = new ArrayList<>(Integer.valueOf(MAX_GENERATION + ""));

        //最好适应度出现的代数
        long bestGeneration = 0;

        //轮盘
        double[] fitnessWheel = null;

        //初始化种群:注意这里的路侧节点从1开始，即0号没有用
        initFitness1(heatTable, oldFitness, oldPopulation.length);
        initFitness1(heatTable, newFitness, newPopulation.length);
        initPopulation1(oldPopulation, blockIdList, paths, heatTable, oldFitness);


        for (int gen = 0; gen < MAX_GENERATION; gen++) {
            //找到本代具有适应度最高的个体，直接进入下一代第一个位置,防止优良基因的丢失
            int best = findBestFitness1(oldFitness);
            individualArrayCopy(oldPopulation[best], newPopulation[0]);

            //本代适应度最低的个体被替换成适应度最高的个体
            int worst = findWorstFitness1(oldFitness);
            individualArrayCopy(oldPopulation[best], oldPopulation[worst]);
            fitnessCopy(oldFitness.get(best), oldFitness.get(worst));

            //记录每一代最好的适应度
            recordBestFitness1(oldPopulation[best], bestPopulation, oldFitness.get(best), bestFitness, bestFitnessRecordList, bestIndividualRecordList, bestFitnessValueList);
            //如果是至今为止最好的，那么记录一下
            if (gen == 0 || compareFitness1(bestFitness, oldFitness.get(best))) {
                fitnessCopy(oldFitness.get(best), bestFitness);
                //拷贝个体
                individualArrayCopy(oldPopulation[best], bestPopulation);
                bestGeneration = gen;
            }

            //轮盘选择下一代
            fitnessWheel = new double[oldPopulation.length];
            calWheel(fitnessWheel, oldFitness);
            //创建新种群
            for (int i = 1; i < oldPopulation.length; i++) {//不包含第一个因为第一个是最优秀个体
                individualArrayCopy(oldPopulation[getWheelIndex(fitnessWheel)], newPopulation[i]);
            }

            //重组+变异
            Random random = new Random();
            for (int i = 0; i < oldPopulation.length; i += 2) {
                //拷贝到新种群中
                if (random.nextDouble() < crossRate) {
                    crossOver1(newPopulation[i], newPopulation[i + 1]);
                } else {
                    if (random.nextDouble() < mutaRate) {
                        muta1(newPopulation[i], blockIdList);
                    }
                    if (random.nextDouble() < mutaRate) {
                        muta1(newPopulation[i + 1], blockIdList);
                    }
                }

                //重新计算新种群适应度
                calculateFitnessMap1(newPopulation[i], paths, heatTable, newFitness.get(i));
                calculateFitnessMap1(newPopulation[i + 1], paths, heatTable, newFitness.get(i + 1));
            }

            //新种群成为旧种群
            long[][][] tmp1 = oldPopulation;
            oldPopulation = newPopulation;
            newPopulation = tmp1;

            List<Map<Integer, Map<Long, Double>>> tmp2 = oldFitness;
            oldFitness = newFitness;
            newFitness = tmp2;
        }

        ExperimentRecordUtils.printToExcel(bestFitnessValueList, "遗传算法");
        printHeatTable(heatTable);
        System.out.println("-------------------");
        printPath(paths);
        System.out.println("-------------------");
        printIndividual(bestPopulation);
        System.out.println("-------------------");
        System.out.println(bestGeneration);

        return bestPopulation;
    }

    /**
     * 初始化适应度：适应度是根据热度表计算得出，所以适应度也跟随热度表初始化，
     */
    private void initFitness1(Map<Integer, Map<Long, Long>> heatTable, List<Map<Integer, Map<Long, Double>>> fitness, int individualNum) throws Exception {
        for (int i = 0; i < individualNum; i++) {
            Map<Integer, Map<Long, Double>> m = new HashMap<>();
            fitness.add(m);

            for (Map.Entry<Integer, Map<Long, Long>> entry : heatTable.entrySet()) {
                Integer k1 = entry.getKey();
                Map<Long, Long> v1 = entry.getValue();
                if (m.containsKey(k1)) {//初始化期间是不可能的
                    throw new Exception("初始化错误:" + k1);
                }
                Map<Long, Double> map = new HashMap<>(v1.size());

                for (Map.Entry<Long, Long> en : v1.entrySet()) {
                    if (m.containsKey(en.getKey())) {//初始化期间是不可能的
                        throw new Exception("初始化错误:" + en.getKey());
                    }
                    map.put(en.getKey(), 0d);//都初始化为0
                }
                m.put(k1, map);
            }
        }
    }


    /**
     * 初始化种群
     * TODO：初始化算法优化
     *
     * @param population
     * @param blockIdList
     */
    private void initPopulation1(long[][][] population, List<Long> blockIdList, double[][] paths, Map<Integer, Map<Long, Long>> heatTable, List<Map<Integer, Map<Long, Double>>> fitness) {
        int length = blockIdList.size();
        Random r = new Random();
        for (int k = 0; k < population.length; k++) {
            long[][] individual = population[k];
            //individual即一个个体
            for (int i = 0; i < individual.length; i++) {
                for (int j = 0; j < individual[i].length; j++) {
                    //随机选出一些区块放入每个个体
                    individual[i][j] = blockIdList.get(r.nextInt(length));
                }
                //计算适应度
            }
            calculateFitnessMap1(individual, paths, heatTable, fitness.get(k));
        }
    }

    /**
     * 轮盘赌：筛选下一次迭代的种群，适应度高的个体被筛选出来的概率大
     * 创建轮盘
     */
    private void calWheel(double[] fitnessWheel, List<Map<Integer, Map<Long, Double>>> fitness) {
        //转换成适应度值
        List<Double> fitnessList = fitness.stream().map(this::calculateFitness1).collect(Collectors.toList());
        for (int i = 0; i < fitnessWheel.length; i++) {
            fitnessWheel[i] = 1 / fitnessList.get(i);//这样小的延迟就变成大的适应度
        }
        for (int i = 1; i < fitnessWheel.length; i++) {
            fitnessWheel[i] += fitnessWheel[i - 1];//转换成比率，注意这里的适应度是延迟，所以延迟越小占比越大
        }
        double sum = fitnessWheel[fitnessWheel.length - 1];
        fitnessWheel[0] = fitnessWheel[0] / sum;
        for (int i = 0; i < fitnessWheel.length; i++) {
            fitnessWheel[i] = fitnessWheel[i] / sum;
        }
    }

    /**
     * 轮盘赌：筛选下一次迭代的种群，适应度高的个体被筛选出来的概率大
     * 扔飞镖选中
     *
     * @return
     */
    private int getWheelIndex(double[] fitnessWheel) {
        Random random = new Random();
        double index = random.nextDouble();
        for (int i = 0; i < fitnessWheel.length; i++) {
            if (index <= fitnessWheel[i]) {
                return i;
            }
        }
        return 0;//这个情况应该不可能
    }

    /**
     * 找到适应度最高的个体
     * 返回个体的索引
     */
    private int findBestFitness1(List<Map<Integer, Map<Long, Double>>> fitness) {
        int best = IntStream.range(0, fitness.size()).reduce((a, b) -> compareFitness1(fitness.get(a), fitness.get(b)) ? b : a).getAsInt();
        return best;
    }

    /**
     * 找到适应度最低的个体
     * 返回个体的索引
     */
    private int findWorstFitness1(List<Map<Integer, Map<Long, Double>>> fitness) {
        int worst = IntStream.range(0, fitness.size()).reduce((a, b) -> compareFitness1(fitness.get(a), fitness.get(b)) ? a : b).getAsInt();
        return worst;
    }


    /**
     * 记录历代最好的适应度
     * 返回true说明最优适应度被更新
     */
    private void recordBestFitness1(long[][] oldIndividual, long[][] bestIndividual, Map<Integer, Map<Long, Double>> fitness, Map<Integer, Map<Long, Double>> bestFitness, List<Map<Integer, Map<Long, Double>>> bestFitnessRecordList, List<long[][]> bestIndividualRecordList, List<Double> bestFitnessValueList) {
        //无论如何都记录本代最好适应度
        //记录本代最好个体
        long[][] individualRecord = new long[oldIndividual.length][oldIndividual[0].length];
        individualArrayCopy(oldIndividual, individualRecord);
        bestIndividualRecordList.add(individualRecord);
        //记录本代最好适应度
        Map<Integer, Map<Long, Double>> fitnessRecord = new HashMap<>();
        fitnessCopy(fitness, fitnessRecord);
        bestFitnessRecordList.add(fitnessRecord);
        //记录适应度的值
        bestFitnessValueList.add(calculateFitness1(fitnessRecord));
    }

    /**
     * 比较适应度的算法——加和
     * 返回true说明fitnessB更好
     */
    private boolean compareFitness1(Map<Integer, Map<Long, Double>> fitnessA, Map<Integer, Map<Long, Double>> fitnessB) {
        double d1 = calculateFitness1(fitnessA);
        double d2 = calculateFitness1(fitnessB);
        return d1 > d2;
    }

    private double calculateFitness1(Map<Integer, Map<Long, Double>> fitness) {
        double result = fitness.values().stream().flatMap(m -> m.values().stream()).mapToDouble(d -> d).sum();
        return result;
    }

    /**
     * 对一个个体计算适应度即延迟
     *
     * @param population
     * @param paths      两两节点之间的延迟代价
     * @param heatTable  节点id-区块id-热度、访问次数
     * @param fitness    节点id-区块id-延迟、适应度
     */
    private void calculateFitnessMap1(long[][] population, double[][] paths, Map<Integer, Map<Long, Long>> heatTable, Map<Integer, Map<Long, Double>> fitness) {
        for (int nodeId : heatTable.keySet()) {
            //nodeId是节点的id，heatRecord是节点的热度表
            Map<Long, Long> heatRecord = heatTable.get(nodeId);

            //以节点nodeId为起点计算每条链路寻找当前区块的代价
            for (long blockId : heatRecord.keySet()) {
                //自己就有，延迟就是0
                if (containsBlock(population[nodeId], blockId)) {
                    fitness.get(nodeId).put(blockId, (double) 0);
                    continue;
                }
                //查找最近的有区块的节点
                double pathValue = MAX_PATH_VALUE;
                for (int i = 0; i < paths[nodeId].length; i++) {
                    //有区块并且比当前最小值小那么就更新
                    if (containsBlock(population[i], blockId) && paths[nodeId][i] < pathValue) {
                        pathValue = paths[nodeId][i];
                    }
                }
                //最终的适应度就是访问次数*延迟，所以延迟的差距会被放大
                fitness.get(nodeId).put(blockId, pathValue * heatRecord.get(blockId));
            }
        }
    }

    /**
     * 判断一个节点是否含有区块
     *
     * @return
     */
    private boolean containsBlock(long[] population, long blockId) {
        for (long l : population) {
            if (l == blockId) {
                return true;
            }
        }
        return false;
    }

    /**
     * 基因重组：两个个体之间进行基因重组
     * 两个个体交换对应某两个节点的区块
     */
    private void crossOver1(long[][] father, long[][] mother) {
        Random random = new Random();
        int nodeNum = father.length;
        int fromIndex = random.nextInt(nodeNum);//起始
        int toIndex = random.nextInt(nodeNum);//终止
        if (toIndex < fromIndex) {
            int tmp = toIndex;
            toIndex = fromIndex;
            fromIndex = tmp;
        }
        individualArrayExchange1(father, mother, fromIndex, toIndex);
    }

    /**
     * 基因突变：单个个体基因突变
     * 每个节点的某个区块突变
     */
    private void muta1(long[][] father, List<Long> blockIdList) {
        Random random = new Random();
        //哪个节点突变
        int node = random.nextInt(father.length);
        //哪个位置突变
        int pos = random.nextInt(father[node].length);
        //突变后区块是谁
        long block = blockIdList.get(random.nextInt(blockIdList.size()));
        father[node][pos] = block;
    }


    /**
     * 个体拷贝
     *
     * @param from
     * @param to
     */
    private void individualArrayCopy(long[][] from, long[][] to) {
        for (int i = 0; i < from.length; i++) {
            for (int j = 0; j < from[i].length; j++) {
                to[i][j] = from[i][j];
            }
        }
    }

    /**
     * 个体拷贝
     *
     * @param from
     * @param to
     */
    private void individualArrayExchange2(long[][] from, long[][] to, int fromIndex, int toIndex) {
        for (int i = fromIndex; i <= toIndex; i++) {
            for (int j = 0; j < from[i].length; j++) {
                long tmp = to[i][j];
                to[i][j] = from[i][j];
                from[i][j] = tmp;
            }
        }
    }


    /**
     * 个体拷贝
     *
     * @param from
     * @param to
     */
    private void individualArrayExchange1(long[][] from, long[][] to, int fromIndex, int toIndex) {
        for (int j = 0; j < from[fromIndex].length; j++) {
            long tmp = to[fromIndex][j];
            to[fromIndex][j] = from[fromIndex][j];
            from[fromIndex][j] = tmp;
        }

        for (int j = 0; j < from[toIndex].length; j++) {
            long tmp = to[toIndex][j];
            to[toIndex][j] = from[toIndex][j];
            from[toIndex][j] = tmp;
        }
    }

    /**
     * 拷贝适应度
     *
     * @param from
     * @param to
     */
    private void fitnessCopy(Map<Integer, Map<Long, Double>> from, Map<Integer, Map<Long, Double>> to) {
        for (Integer i : from.keySet()) {
            to.put(i, new HashMap<>());
            for (Long l : from.get(i).keySet()) {
                to.get(i).put(l, from.get(i).get(l));
            }
        }
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
