package testVersion;

import java.util.List;

public class IdentityResponseForVehicle {

    //路侧节点身份信息
    private String rsuId;
    private String publicKey;
    private String identityBlockHash;//数据区块hash值
    private String identityBlockMerkleRoot;//数据区块merkle根
    private List<String> identityBlockMerklePath;//身份信息在数据区块上的merkle路径
    private String identityVerifyBlockHash;//验证区块hash值
    private List<String> identityVerifyBlockMerklePath;//数据区块hash值在验证区块上的merkle路径

    //加密后的时间戳
    private String timestamp1;//车辆公钥加密
    private String timestamp2;//RSU私钥加密

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

    public String getIdentityBlockMerkleRoot() {
        return identityBlockMerkleRoot;
    }

    public void setIdentityBlockMerkleRoot(String identityBlockMerkleRoot) {
        this.identityBlockMerkleRoot = identityBlockMerkleRoot;
    }

    public String getRsuId() {
        return rsuId;
    }

    public void setRsuId(String rsuId) {
        this.rsuId = rsuId;
    }

    public String getTimestamp1() {
        return timestamp1;
    }

    public void setTimestamp1(String timestamp1) {
        this.timestamp1 = timestamp1;
    }

    public String getTimestamp2() {
        return timestamp2;
    }

    public void setTimestamp2(String timestamp2) {
        this.timestamp2 = timestamp2;
    }


}
