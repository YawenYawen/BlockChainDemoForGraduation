package testVersion;

import java.util.List;

public class ResponseForVehicle {

    //数据信息
    private Double minDis;
    private String dataBlockHash;//数据区块的hash值
    private String dataBlockMerkleRoot;//数据区块的merkle根
    private String dataTranHash;//交易的hash值
    private String content;//交易的内容
    private List<String> tranMerklePath;//交易在数据链上的merkle路径
    private List<String> dataBlockMerklePath;//数据区块hash值在验证链上的merkle路径
    private Integer dataVerifyBlockIndex;//验证区块的索引值

    //路侧节点身份信息
    private String publicKey;
    private String identityBlockHash;//数据区块hash值
    private String identityBlockMerkleRoot;//数据区块merkle根
    private List<String> identityBlockMerklePath;//身份信息在数据区块上的merkle路径
    private String identityVerifyBlockHash;//验证区块hash值
    private List<String> identityVerifyBlockMerklePath;//数据区块hash值在验证区块上的merkle路径

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getIdentityBlockHash() {
        return identityBlockHash;
    }

    public void setIdentityBlockHash(String identityBlockHash) {
        this.identityBlockHash = identityBlockHash;
    }

    public List<String> getIdentityBlockMerklePath() {
        return identityBlockMerklePath;
    }

    public void setIdentityBlockMerklePath(List<String> identityBlockMerklePath) {
        this.identityBlockMerklePath = identityBlockMerklePath;
    }

    public String getIdentityVerifyBlockHash() {
        return identityVerifyBlockHash;
    }

    public void setIdentityVerifyBlockHash(String identityVerifyBlockHash) {
        this.identityVerifyBlockHash = identityVerifyBlockHash;
    }

    public List<String> getIdentityVerifyBlockMerklePath() {
        return identityVerifyBlockMerklePath;
    }

    public void setIdentityVerifyBlockMerklePath(List<String> identityVerifyBlockMerklePath) {
        this.identityVerifyBlockMerklePath = identityVerifyBlockMerklePath;
    }

    public Double getMinDis() {
        return minDis;
    }

    public void setMinDis(Double minDis) {
        this.minDis = minDis;
    }

    public String getDataBlockHash() {
        return dataBlockHash;
    }

    public void setDataBlockHash(String dataBlockHash) {
        this.dataBlockHash = dataBlockHash;
    }

    public String getDataTranHash() {
        return dataTranHash;
    }

    public void setDataTranHash(String dataTranHash) {
        this.dataTranHash = dataTranHash;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getTranMerklePath() {
        return tranMerklePath;
    }

    public void setTranMerklePath(List<String> tranMerklePath) {
        this.tranMerklePath = tranMerklePath;
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

    public String getIdentityBlockMerkleRoot() {
        return identityBlockMerkleRoot;
    }

    public void setIdentityBlockMerkleRoot(String identityBlockMerkleRoot) {
        this.identityBlockMerkleRoot = identityBlockMerkleRoot;
    }

    public void copyResponse(ResponseForIoV responseForIoV) {
        this.minDis = responseForIoV.getMinDis();
        this.dataBlockHash = responseForIoV.getDataBlockHash();
        this.dataBlockMerkleRoot = responseForIoV.getDataBlockMerkleRoot();
        this.dataTranHash = responseForIoV.getDataTranHash();
        this.content = responseForIoV.getContent();
        this.tranMerklePath = responseForIoV.getTranMerklePath();
        this.dataBlockMerklePath = responseForIoV.getDataBlockMerklePath();
    }
}
