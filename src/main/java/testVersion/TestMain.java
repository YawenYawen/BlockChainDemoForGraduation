package testVersion;

import org.bouncycastle.util.encoders.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.zip.*;

public class TestMain {
    public static void main(String args[]) throws Exception {
        String request = "{\n" +
                "\t\"keywordsVector\": \"iev1OHfZfW/PCzi83IJafdIPjIrPa8AxFFil27+1329MD0sCGmNxYuY0oIKuUI4GRCnYjXzTDAPK0Zc4829DEobOyJgQ7Lzcsw6RorEN94JVmrBRfG9GaSOnBMOaR9iP8AtPBMvR72gOLVipZeCjg88aWHddZ3BoSrmFkU832E8=\",\n" +
                "\t\"requestKey\": \"CAR_REQUEST\",\n" +
                "\t\"requestIndex\": \"VEHICLE331571749463034\",\n" +
                "\t\"carTransactionIndex\": 3,\n" +
                "\t\"cardBlockHash\": \"004c0ec8c0751e6c541aedc97ee1863699fccc166a14882fe899adf3f6505cfa\",\n" +
                "\t\"timestamp\": \"dwnWB7hn+zptlqZ1vBwZLBdb+js6E3Y4ybK2gKmFaVn1rlPYxeyupvJPGnYVR3/0GSmA3by/LzGt0Y6KxlNQsCKyb6zkwF3E+qzKcf4FRvFo2nDOgZ1FmmJAiL5lMJt866RB9YKQGBKCNHbfGJTRY+tUIcYQlLGGAIgGcD53YDk=\"\n" +
                "}";

        request = "{\"data\":\"{\\\"minDis\\\":0.13122776993819785,\\\"dataBlockHash\\\":\\\"0576b59e2b2ce87663d2e9987db6c6213d52ba2dd3f7f64067feaa310aaa6b10\\\",\\\"dataBlockMerkleRoot\\\":\\\"cfdf6baffd3e2e30f4d94aa2b40c1f1a89fbc011b1445eae263a4773d1e7bb0c\\\",\\\"dataTranHash\\\":\\\"11c93f3c2c09ace181458126c4b4590aee67d5d66dad29841a9d16476a07bf07\\\",\\\"content\\\":\\\"Z28+jq2duTPCNC3MAF85O1EQ4pKrjL7LnhiesVk/fs5Kxw3+gw6K7jtpy5d5NiW61fdXQhYJy0YPmbuls5rH6w==\\\",\\\"tranMerklePath\\\":[\\\"11c93f3c2c09ace181458126c4b4590aee67d5d66dad29841a9d16476a07bf07\\\",\\\"3642e15ce333a59d292ef18e481351c359e36e6fdaadd2c681e89611e276a4d8\\\",\\\"99eb9072af39d416e64a78ceb9b81c28564a94a5b63bcb101cdec431e4621abe\\\",\\\"0379852f5dea718d66f51548083e76a9b964190e15153d68a5047f55ce9597fe\\\",\\\"cc62dbac5b61d03e40a16460ef412e4c97e6b7982212e5708adffb8e44072476\\\",\\\"0225d377152997a589b4e6041b1998fcfc88912bf9ecddc4fe161fb40a02e7c0\\\"],\\\"dataBlockMerklePath\\\":[\\\"725596ff3666ed8a856cc6e77b557c6419bb49399c7d44a453cf33ab63aa954a\\\",\\\"3efdd8abe62701d061e7c2aa1256b2692c5dc3c6b25997dfd11c30f50a4913e6\\\",\\\"a7c87477630f07cff45ff878f52e78fb2345248d4159e83a23b2fab7d3a28a57\\\",\\\"3a1367407d57e57790c1d2a75bb398eb8f47e706dc03a5505f86f4886aa0d507\\\",\\\"33e48c40c1b8a9c6385e51c29b3fc38019803aa8aa4100b9403ef5c8c4dd898b\\\",\\\"eabbab21be424a8bfdb0e9e5181951d7df609fff01aaa86d7a344970d06922c8\\\",\\\"680b79b786de665461c79d45cc0cfa47fa35b069cfca38b6e00f658f7f04af15\\\",\\\"590856c9a2f5bbf67c0dfb61227da0bee601a16c6a0f4c2d8f4a54c7fe2e1ac9\\\"],\\\"dataVerifyBlockIndex\\\":null,\\\"publicKey\\\":\\\"MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAIBsTne6ixuiwqlafnQw5u0cYtpK6uhJt7aAHycwvUKGywKsel/pWfgWZqcpV4HgluKx13LOV+Vr6ntxipF58a8CAwEAAQ==\\\",\\\"identityBlockHash\\\":\\\"0d420594f7044e4f9bcf0c4777058f9d7e9ba4e3f04970e39aeeb98e44c598a0\\\",\\\"identityBlockMerkleRoot\\\":\\\"f5b93a5429de537fc829d3c4638a502842f9ee6814ba7e8b803aa4060d99b20f\\\",\\\"identityBlockMerklePath\\\":[\\\"9211c62401c4a02b6fea13c760a877eb2e5d80124f7f5d5f63590745c54c2e50\\\",\\\"0e7c8a14e2de70af0ce85825328ad7df3cd03b4a06bd90a0439a1a656c9896aa\\\",\\\"c6a0bf39d34fe08fc320e6e56fae1d6513a2a2db723529edbf80d6921f77a860\\\",\\\"35e6e367f5eadab92fc86b0209bf8c553c6dc443a3864b7187641b53b84d5f40\\\",\\\"f3a345441415c0f40cc0cc254ab94f9ce4f7e91fa04e2a012257521d3a95a501\\\",\\\"2d3d04813ccecb70e1095a2fa5b0ce84f8b16bba4ebd45142e20f09a4e308973\\\",\\\"6f0d8118add5d8e035133f6414b7383f5899f59acbdc704e21dcbaf3d2e60e95\\\",\\\"08ad2521e2abb56ae8d18b0c21f864b576327bd61a92000adaa92623dc4d84c3\\\"],\\\"identityVerifyBlockHash\\\":\\\"099c4534b84caa49ead2104bc897737a8b207299ba1d2ddff96bb87573fb7eac\\\",\\\"identityVerifyBlockMerklePath\\\":[\\\"f8734f427f3a746c755587898e7ba7dd7c5add418b804f0f9a7a9da77017b39d\\\",\\\"1a572ee80c875b31687d4c29fe7e6706220776ec63af8aeef7db932ff07482dc\\\",\\\"fa696bb9a5f1efb912a830524140ecdc0b76e157051015ab9895a318c4025c5b\\\",\\\"b464b3cac2343d043036364139f4469f35e272b9111660934caf083288ef0795\\\",\\\"26dd4c070a30337fa7eb3301f73923b393e9c30b9dd93cd92092a039975945ed\\\",\\\"31bf77bb1c5a8453f22b3ab9f0efbe72299ee50e96726d2d266c1251feaf8436\\\",\\\"ce0705d1d41940bc7525a631f186324f2cf22349350fafefbda4bcbaeab92456\\\",\\\"a9b4d442a16de9bf17499468431d0844c563d73f53be75e10697cedfa0996b82\\\"]}\",\"requestKey\":\"CAR_RESPONSE\",\"requestIndex\":\"VEHICLE331571989504178\",\"timestamp\":\"FNHhgJbdL0vm3TOFWuLognZ1OhXj7MWnTQXsNtfP3t8acZ3bEqeATdmIjSerp8AV7wPonKlavV/HwPL+9DYKgA==\"}";
        System.out.println(request.getBytes().length);
//        byte[] requests = Base64.decode(request.getBytes());
//        System.out.println(requests.length);


//        byte[] com = zip(data.getBytes());
//        System.out.println(com.length);
//        byte[] result = unzip(com);
//        System.out.println(result.length);

        byte[] com = deflateCompress(request.getBytes());
        System.out.println(com.length);
        byte[] result = deflateUncompress(com);
        System.out.println(result.length);


        System.out.println(new String(result));

    }

    /**
     * @param inputByte 待解压缩的字节数组
     * @return 解压缩后的字节数组
     * @throws IOException
     */
    public static byte[] deflateUncompress(byte[] inputByte) throws Exception {
        Inflater infl = new Inflater();
        infl.setInput(inputByte);

        //输出解压后的内容
        byte[] result = new byte[4000];
        int resultLength = infl.inflate(result);
        infl.end();
        System.out.println("resultLength:::" + resultLength);

        return result;
    }

    /**
     * 压缩.
     *
     * @param inputByte 待压缩的字节数组
     * @return 压缩后的数据
     * @throws IOException
     */
    public static byte[] deflateCompress(byte[] inputByte) {
        Deflater defl = new Deflater(4);
        defl.setInput(inputByte);
        defl.finish();

        //输出压缩后的内容
        byte[] output = new byte[3000];
        int compressedDataLength = defl.deflate(output);
        System.out.println("compressedDataLength:::" + compressedDataLength);
        defl.end();
        return output;
    }

    public static byte[] gZip(byte[] data) {

        byte[] b = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(bos);
            gzip.write(data);
            gzip.finish();
            gzip.close();
            b = bos.toByteArray();
            bos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return b;
    }

    /***
     * 解压GZip
     *
     * @param data
     * @return
     */
    public static byte[] unGZip(byte[] data) {
        byte[] b = null;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            GZIPInputStream gzip = new GZIPInputStream(bis);
            byte[] buf = new byte[1024];
            int num = -1;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((num = gzip.read(buf, 0, buf.length)) != -1) {
                baos.write(buf, 0, num);
            }
            b = baos.toByteArray();
            baos.flush();
            baos.close();
            gzip.close();
            bis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return b;
    }


    /**
     * 使用zip进行压缩
     *
     * @param str 压缩前的文本
     * @return 返回压缩后的文本
     */
    public static final byte[] zip(byte[] str) {
        byte[] compressed = null;
        ByteArrayOutputStream out = null;
        ZipOutputStream zout = null;
        try {
            out = new ByteArrayOutputStream();
            zout = new ZipOutputStream(out);
            zout.putNextEntry(new ZipEntry("0"));
            zout.write(str);
            zout.closeEntry();
            compressed = out.toByteArray();
            return compressed;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zout != null) {
                try {
                    zout.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
        return compressed;
    }

    /**
     * 使用zip进行解压缩
     *
     * @param compressed 压缩后的文本
     * @return 解压后的字符串
     */
    public static final byte[] unzip(byte[] compressed) {
        ByteArrayOutputStream out = null;
        ByteArrayInputStream in = null;
        ZipInputStream zin = null;
        byte[] decompressed = null;
        try {
            out = new ByteArrayOutputStream();
            in = new ByteArrayInputStream(compressed);
            zin = new ZipInputStream(in);
            zin.getNextEntry();
            byte[] buffer = new byte[1024];
            int offset = -1;
            while ((offset = zin.read(buffer)) != -1) {
                out.write(buffer, 0, offset);
            }
            decompressed = out.toByteArray();
        } catch (IOException e) {
            decompressed = null;
        } finally {
            if (zin != null) {
                try {
                    zin.close();
                } catch (IOException e) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
        return decompressed;
    }


    public static void test1() throws Exception {
        double[][] paths = new double[2][2];
        double[][] paths1 = new double[2][2];
        Random random = new Random();
        for (int k = 0; k < 500; k++) {
            System.out.println(k);
            for (int i = 0; i < paths.length; i++) {
                for (int j = 0; j <= i; j++) {
                    if (i == j) {//自我访问有一点点的代价
                        paths[i][j] = random.nextDouble();
                    }
                    paths[i][j] = random.nextDouble() * 10 + 1;
                    paths[j][i] = paths[i][j];
                }
            }

            FileUtils.serialization(paths, "/Users/zhaoyawen/Documents/blockchaindemo/cs/print0.json");

            try {
                paths1 = (double[][]) FileUtils.deserialization("/Users/zhaoyawen/Documents/blockchaindemo/cs/print0.json");
            } catch (Exception e) {
                for (double[] doubles : paths) {
                    for (double d : doubles) {
                        System.out.print(d + " ");
                    }
                    System.out.println();
                }
            }
        }
    }
}
