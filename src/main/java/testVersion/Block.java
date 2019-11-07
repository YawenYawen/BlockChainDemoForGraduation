package testVersion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Block implements Serializable {

    private Integer verifyBlockIndex;//验证区块索引
    private Integer verifyTranIndex;//验证区块索引

    private String hash;

    private String prevHash;

    private String version;

    /**
     * 车联网交易list
     * json格式
     */
    private List<IoVTransaction> data;

    /**
     * the time when block is created as number of milliseconds since 1/1/1970.
     */
    private long createTimeStamp;

    /**
     * 用于工作量证明算法的计数器
     */
    private int nonce = 0;

    /**
     * 难度目标
     */
    private int difficulty;

    //0是根
    private List<String> merkleTree;

    public Block(String prevHash, String version, List<IoVTransaction> data, int difficulty) {
        this.prevHash = prevHash;
        this.version = version;
        this.data = data;
        this.createTimeStamp = System.currentTimeMillis();
        this.difficulty = difficulty;

        //创建merkle树
        List<String> tranHash = data.stream().map(IoVTransaction::getHash).collect(Collectors.toList());
        createMerkelTree(tranHash);

        //区块的参数都有了，就在构造函数里面挖矿
        mineBlock();
    }

    public Block() {
    }

    /**
     * 设置hash和nonce和createTimeStamp两个属性
     */
    public void mineBlock() {
        //看前面的0
        String target = "";
        for (int i = 0; i < difficulty; i++) {
            target += "0";
        }
        do {
            createTimeStamp = System.currentTimeMillis();
            nonce++;
            hash = calculateHash();
        } while (!hash.substring(0, difficulty).equals(target));
    }

    /**
     * 计算hash值
     *
     * @return
     */
    private String calculateHash() {
        return HashUtils.applySha256ByApacheTwice(prevHash + version + createTimeStamp + data + nonce + difficulty + merkleTree.get(0));
    }

    private void createMerkelTree(List<String> tranHash) {
        int length = 0;
        int tmp = tranHash.size();
        while (tmp > 0) {
            length += tmp;
            tmp /= 2;
        }
        String[] tree = new String[length];
        //底层是交易hash
        for (int i = 0; i < tranHash.size(); i++) {
            tree[i + tranHash.size() - 1] = tranHash.get(i);
        }
        setNodeHash(0, tree);
        merkleTree = Arrays.asList(tree);
    }

    private void setNodeHash(int headIndex, String[] tree) {
        if (headIndex >= tree.length / 2) {
            return;
        }
        int left = headIndex * 2 + 1;
        int right = headIndex * 2 + 2;
        setNodeHash(left, tree);
        setNodeHash(right, tree);
        tree[headIndex] = HashUtils.applySha256ByApacheTwice(tree[left] + tree[right]);
    }

    /**
     * 自身hash验证
     *
     * @return
     */
    public boolean validate() {
        String _hash = calculateHash();
        if (!StringUtils.equals(_hash, hash)) {
            return false;
        }
        String target = new String(new char[difficulty]).replace('\0', '0'); //Create a string with difficulty * "0"
        return hash.substring(0, difficulty).equals(target);
    }

    public String getPrevHash() {
        return prevHash;
    }

    public void setPrevHash(String prevHash) {
        this.prevHash = prevHash;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<IoVTransaction> getData() {
        return data;
    }

    public void setData(List<IoVTransaction> data) {
        this.data = data;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public String getHash() {
        return hash;
    }

    public long getCreateTimeStamp() {
        return createTimeStamp;
    }

    public int getNonce() {
        return nonce;
    }

    public List<String> getMerkleTree() {
        return merkleTree;
    }

    public void setMerkleTree(List<String> merkleTree) {
        this.merkleTree = merkleTree;
    }

    public Integer getVerifyBlockIndex() {
        return verifyBlockIndex;
    }

    public void setVerifyBlockIndex(Integer verifyBlockIndex) {
        this.verifyBlockIndex = verifyBlockIndex;
    }

    public Integer getVerifyTranIndex() {
        return verifyTranIndex;
    }

    public void setVerifyTranIndex(Integer verifyTranIndex) {
        this.verifyTranIndex = verifyTranIndex;
    }

    public void printBlock() {
        System.out.println("Block hash: " + hash);
        System.out.println("Prev hash: " + prevHash);
        System.out.println("Version: " + version);
        System.out.println("Data: " + data);
        System.out.println("difficulty: " + difficulty);
        System.out.println("nonce: " + nonce);
        System.out.println("Create TimeStamp: " + createTimeStamp);
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setCreateTimeStamp(long createTimeStamp) {
        this.createTimeStamp = createTimeStamp;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Block) {
            return hash == ((Block) obj).getHash();
        }
        return false;
    }

}
