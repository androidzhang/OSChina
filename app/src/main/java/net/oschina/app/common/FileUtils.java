package net.oschina.app.common;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件操作工具包
 * Created by zlx on 2017/7/13.
 */

public class FileUtils {
    /**
     * 写文本文件 在Android系统中，文件保存在 /data/data/PACKAGE_NAME/files 目录下
     */


    //获取应用程序缓存文件夹下的指定目录
    public static String getAppCache(Context context, String dir) {
        String savePath = context.getCacheDir().getAbsolutePath() + "/" + dir + "/";


        File savedir = new File(savePath);


        if (!savedir.exists()) {
            savedir.mkdirs();
        }

        savedir = null;


        return savePath;

    }

    //    获取一个文件夹下的所有文件
    public static List<File> listPathFiles(String root) {

        List<File> allDir = new ArrayList<>();
        SecurityManager securityManager = new SecurityManager();
        File path = new File(root);
        securityManager.checkRead(root);
        File[] files = path.listFiles();
        for (File f : files) {
            if (f.isFile()) {
                allDir.add(f);
            } else {
                listPath(f.getAbsolutePath());
            }
        }


        return allDir;
    }

    //列出root目录下所有子目录
    private static List<String> listPath(String root) {

        List<String> allDir = new ArrayList<String>();
        SecurityManager checker = new SecurityManager();
        File path = new File(root);
        checker.checkRead(root);
        // 过滤掉以.开始的文件夹
        if (path.isDirectory()) {
            for (File f : path.listFiles()) {
                if (f.isDirectory() && !f.getName().startsWith(".")) {
                    allDir.add(f.getAbsolutePath());
                }
            }
        }
        return allDir;


    }
}
