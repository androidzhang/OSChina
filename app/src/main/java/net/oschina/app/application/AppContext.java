package net.oschina.app.application;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import net.oschina.app.AppConfig;
import net.oschina.app.AppException;
import net.oschina.app.common.StringUtils;

import java.util.Properties;

/**
 * Created by zlx on 2017/7/13.
 * 全局应用程序类：用于保存和调用全局应用配置及访问网络数据
 */

public class AppContext extends Application {


    private String saveImagePath;

    @Override
    public void onCreate() {
        super.onCreate();
        //注册App异常崩溃处理器
        Thread.setDefaultUncaughtExceptionHandler(AppException.getAppExceptionHandler());

        init();


    }

    /**
     * 初始化
     */
    private void init() {
        //设置保存图片的路径
        saveImagePath = getProperty(AppConfig.SAVE_IMAGE_PATH);
        if (StringUtils.isEmpty(saveImagePath)) {

            setProperty(AppConfig.SAVE_IMAGE_PATH, AppConfig.DEFAULT_SAVE_IMAGE_PATH);
            saveImagePath = AppConfig.DEFAULT_SAVE_IMAGE_PATH;
        }


    }


    public boolean containsProperty(String key) {
        Properties props = getProperties();
        return props.containsKey(key);
    }

    public void setProperties(Properties ps) {
        AppConfig.getAppConfig(this).set(ps);
    }

    public Properties getProperties() {
        return AppConfig.getAppConfig(this).get();
    }

    public void setProperty(String key, String value) {
        AppConfig.getAppConfig(this).set(key, value);
    }

    public String getProperty(String key) {
        return AppConfig.getAppConfig(this).get(key);
    }

    public void removeProperty(String... key) {
        AppConfig.getAppConfig(this).remove(key);
    }

    /**
     * 获取App安装包信息
     * @return
     */
    public PackageInfo getPackageInfo() {
        PackageInfo info = null;
        try {
            info = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace(System.err);
        }
        if(info == null) info = new PackageInfo();
        return info;
    }

}
