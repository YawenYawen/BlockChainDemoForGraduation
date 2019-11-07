package testVersion;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtils {

    /**
     * 写入文件，按照行写
     *
     * @param filepath
     * @param dataList
     * @throws Exception
     */
    public static void printToFileByline(String filepath, List<String> dataList) throws Exception {
        FileWriter pubfw = new FileWriter(filepath);
        BufferedWriter pubbw = new BufferedWriter(pubfw);
        for (String line : dataList) {
            pubbw.write(line);
            pubbw.newLine();
        }
        pubbw.flush();
        pubbw.close();
        pubfw.close();
    }

    /**
     * 写入文件，没有行
     *
     * @param filepath
     * @throws Exception
     */
    public static void printToFile(String filepath, String data) throws Exception {
        FileWriter pubfw = new FileWriter(filepath);
        BufferedWriter pubbw = new BufferedWriter(pubfw);
        pubbw.write(data);
        pubbw.flush();
        pubbw.close();
        pubfw.close();
    }

    /**
     * 写入文件，按照行写
     *
     * @param filepath
     * @throws Exception
     */
    public static List<String> readFileByline(String filepath) throws Exception {
        FileReader fileReader = new FileReader(filepath);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        List<String> result = bufferedReader.lines().collect(Collectors.toList());
        bufferedReader.close();
        fileReader.close();
        return result;
    }

    /**
     * 序列化
     */
    public static void serialization(Object object, String filename) throws Exception {
        FileOutputStream fileOutputStream = new FileOutputStream(new File(filename));
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(object);
        objectOutputStream.close();
        fileOutputStream.close();
    }

    /**
     * 反序列化
     */
    public static Object deserialization(String filename) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(new File(filename));
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        Object object = objectInputStream.readObject();
        objectInputStream.close();
        fileInputStream.close();
        return object;
    }
}
