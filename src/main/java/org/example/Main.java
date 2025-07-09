package org.example;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class Main {

    // 定义动态库接口
    public interface CalFeatureLib extends Library {
        // 加载动态库 - 使用资源中的库文件
        CalFeatureLib INSTANCE = loadInstance();

        static CalFeatureLib loadInstance() {
            try {
                // 从资源加载 DLL
                URL dllUrl = Main.class.getResource("/native/CalFeature.dll");
                if (dllUrl == null) {
                    throw new RuntimeException("DLL not found in resources");
                }

                // 创建临时文件
                Path tempPath = Files.createTempFile("calfeature", ".dll");
                tempPath.toFile().deleteOnExit();

                // 复制资源到临时文件
                try (InputStream in = dllUrl.openStream()) {
                    Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
                }

                // 加载 DLL
                return (CalFeatureLib) Native.load(tempPath.toString(), CalFeatureLib.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load DLL", e);
            }
        }

        // C 函数声明
        Pointer create_cal_feature();
        void destroy_cal_feature(Pointer obj);
        Pointer cal_feature_compute(Pointer obj, double[] data);
    }

    public static void main(String[] args) {
        // 创建 CalFeature 实例
        Pointer calFeatureObj = CalFeatureLib.INSTANCE.create_cal_feature();
        if (calFeatureObj == null) {
            System.err.println("错误: 无法创建 CalFeature 对象");
            return;
        }

        try {
            // 读取数据文件 - 使用项目相对路径
            String filePath = "D:\\Test\\QTestDll\\file\\LTG_WF0001_WT1_CH1_ST0_20000121161219.txt";
            File dataFile = new File(filePath);

            if (!dataFile.exists()) {
                System.err.println("错误: 数据文件不存在: " + dataFile.getAbsolutePath());
                return;
            }

            double[] data = readDataFromFile(dataFile);

            if (data == null || data.length == 0) {
                System.err.println("错误: 未读取到有效数据");
                return;
            }

            System.out.println("读取数据长度: " + data.length);

            // 调用特征计算函数
            Pointer resultPtr = CalFeatureLib.INSTANCE.cal_feature_compute(calFeatureObj, data);

            if (resultPtr == null) {
                System.err.println("警告: 计算结果为空");
            } else {
                // 假设结果包含4个double值
                double[] result = resultPtr.getDoubleArray(0, 4);
                System.out.println("计算结果:");
                for (int i = 0; i < result.length; i++) {
                    System.out.printf("特征 %d: %.6f\n", i + 1, result[i]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 确保销毁对象
            CalFeatureLib.INSTANCE.destroy_cal_feature(calFeatureObj);
            System.out.println("已清理资源");
        }
    }

    // 从文件读取数据
    private static double[] readDataFromFile(File file) {
        List<Double> dataList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    double value = Double.parseDouble(line.trim());
                    dataList.add(value);
                } catch (NumberFormatException e) {
                    System.err.println("警告: 跳过无效数据行: " + line);
                }
            }
        } catch (Exception e) {
            System.err.println("读取文件错误: " + e.getMessage());
            return null;
        }

        // 转换为数组
        double[] data = new double[dataList.size()];
        for (int i = 0; i < dataList.size(); i++) {
            data[i] = dataList.get(i);
        }
        return data;
    }
}