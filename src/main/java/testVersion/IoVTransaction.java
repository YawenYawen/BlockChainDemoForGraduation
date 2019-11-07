package testVersion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IoVTransaction implements Serializable {

    private String blockHash;//父区块的hash值
    private String hash;//自己的hash值

    private Integer type;

    /**
     * content和RSAPublicKey二选一
     */
    //content既有可能是车联网数据，也可能是数据区块的hash值
    private String content;

    //身份区块才有的身份信息
    private String RSAPublicKey;

    //数据区块才有的关键词和关键词向量，注意这两个不计入hash值
    private Map<String, Double> keywordsVector;

    //路径包括自己;共2*logn个
    private List<String> merklePath;

    public IoVTransaction(Integer type, String content, String RSAPublicKey, Map<String, Double> keywordsVector) {
        this.content = content;
        this.RSAPublicKey = RSAPublicKey;
        this.type = type;
        this.keywordsVector = keywordsVector;

        switch (type) {
            case 1://车联网数据1
                hash = HashUtils.applySha256ByApacheTwice(content);
                //TODO:做分词然后设置关键词
                break;
            case 2://身份信息2
                hash = HashUtils.applySha256ByApacheTwice(RSAPublicKey);
                break;
            case 3://验证车联网数据3
                hash = HashUtils.applySha256ByApacheTwice(content);
                break;
            case 4://验证身份信息4
                hash = HashUtils.applySha256ByApacheTwice(content);
                break;
        }
    }

    public void setMerklePath(List<String> merklePath) {
        this.merklePath = merklePath;
    }

    public List<String> getMerklePath() {
        return merklePath;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getHash() {
        return hash;
    }

    public String getRSAPublicKey() {
        return RSAPublicKey;
    }

    public void setRSAPublicKey(String RSAPublicKey) {
        this.RSAPublicKey = RSAPublicKey;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Double> getKeywordsVector() {
        return keywordsVector;
    }

    public void setKeywordsVector(Map<String, Double> keywordsVector) {
        this.keywordsVector = keywordsVector;
    }

    public IoVTransaction() {
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }
}
