package testVersion;

import java.util.List;

public class ResponseForIoV {

    private Double minDis;
    private String dataBlockHash;//数据区块的hash值
    private String dataBlockMerkleRoot;//数据区块的merkle根
    private String dataTranHash;//交易的hash值
    private String content;//交易的内容
    private List<String> tranMerklePath;//交易在数据链上的merkle路径
    private List<String> dataBlockMerklePath;//数据区块hash值在验证链上的merkle路径
    private Integer dataVerifyBlockIndex;//验证区块的索引值

    public String getDataTranHash() {
        return dataTranHash;
    }

    public void setDataTranHash(String dataTranHash) {
        this.dataTranHash = dataTranHash;
    }

    public List<String> getTranMerklePath() {
        return tranMerklePath;
    }

    public void setTranMerklePath(List<String> tranMerklePath) {
        this.tranMerklePath = tranMerklePath;
    }

    public Double getMinDis() {
        return minDis;
    }

    public void setMinDis(Double minDis) {
        this.minDis = minDis;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDataBlockHash() {
        return dataBlockHash;
    }

    public void setDataBlockHash(String dataBlockHash) {
        this.dataBlockHash = dataBlockHash;
    }

    public List<String> getDataBlockMerklePath() {
        return dataBlockMerklePath;
    }

    public void setDataBlockMerklePath(List<String> dataBlockMerklePath) {
        this.dataBlockMerklePath = dataBlockMerklePath;
    }

    public Integer getDataVerifyBlockIndex() {
        return dataVerifyBlockIndex;
    }

    public void setDataVerifyBlockIndex(Integer dataVerifyBlockIndex) {
        this.dataVerifyBlockIndex = dataVerifyBlockIndex;
    }

    public String getDataBlockMerkleRoot() {
        return dataBlockMerkleRoot;
    }


    public void setDataBlockMerkleRoot(String dataBlockMerkleRoot) {
        this.dataBlockMerkleRoot = dataBlockMerkleRoot;
    }
}
