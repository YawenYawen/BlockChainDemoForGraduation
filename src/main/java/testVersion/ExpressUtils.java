package testVersion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.*;

public class ExpressUtils {

    /**
     * ZIP解压缩
     * @param input 待解压缩
     * @return 解压缩后的
     */
    public static byte[] inflate(byte[] input) throws Exception {
        Inflater infl = new Inflater();
        infl.setInput(input);

        //输出解压后的内容
        byte[] result = new byte[10000000];
        int resultLength = infl.inflate(result);
        infl.end();
//        System.out.println("resultLength:::" + resultLength);

        return Arrays.copyOf(result, resultLength);
    }

    /**
     * ZIP压缩
     *
     * @param input 待压缩
     * @return 压缩后的数据
     */
    public static byte[] deflate(byte[] input) {
        Deflater defl = new Deflater(4);
        defl.setInput(input);
        defl.finish();

        //输出压缩后的内容
        byte[] output = new byte[10000000];
        int compressedDataLength = defl.deflate(output);
//        System.out.println("compressedDataLength:::" + compressedDataLength);
        defl.end();
        return Arrays.copyOf(output, compressedDataLength);
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

}
