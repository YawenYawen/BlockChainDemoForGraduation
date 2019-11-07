package testVersion;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * 记录实验
 */
public class ExperimentRecordUtils {
    private final Logger log = Logger.getLogger("ExperimentRecordUtil");

    /**
     * 打印优秀个体的变化曲线图
     */
    public static void printToExcel1(List<Long> longList, String filename) throws Exception {
        File file = new File(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd_HH:mm:ss")) + "_" + filename + ".xls");
        Workbook wb = new HSSFWorkbook();
        wb.createSheet();
        wb.setSheetName(0, filename);

        Sheet s = wb.getSheetAt(0);
        s.autoSizeColumn(1, true);

        Row r = null;
        Cell c = null;

        for (int i = 0; i < longList.size(); i++) {
            r = s.createRow(i);

            //序号/代数
            c = r.createCell(0);
            c.setCellValue(i);

            //适应度值
            c = r.createCell(1);
            c.setCellValue(longList.get(i));

        }

        FileOutputStream outputStream = new FileOutputStream(file);
        wb.write(outputStream);
        wb.close();
        outputStream.close();
    }

    public static void printToExcel(List<Double> bestFitnessValueList, String filename) throws Exception {
        File file = new File(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd_HH:mm:ss")) + "_" + filename + ".xls");
        Workbook wb = new HSSFWorkbook();
        wb.createSheet();
        wb.setSheetName(0, filename);

        Sheet s = wb.getSheetAt(0);
        s.autoSizeColumn(1, true);

        Row r = null;
        Cell c = null;

        for (int i = 0; i < bestFitnessValueList.size(); i++) {
            r = s.createRow(i);

            //序号/代数
            c = r.createCell(0);
            c.setCellValue(i);

            //适应度值
            c = r.createCell(1);
            c.setCellValue(bestFitnessValueList.get(i));

        }

        FileOutputStream outputStream = new FileOutputStream(file);
        wb.write(outputStream);
        wb.close();
        outputStream.close();
    }
}
