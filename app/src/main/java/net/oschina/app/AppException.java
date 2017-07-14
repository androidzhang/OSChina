package net.oschina.app;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Environment;
import android.widget.Toast;

import net.oschina.app.application.AppContext;

import org.apache.commons.httpclient.HttpException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * desc:应用程序异常类，用于捕获程序异常和提示错误信息
 * Created by zlx on 2017/7/13.
 */
public class AppException extends Exception implements Thread.UncaughtExceptionHandler {
    //是否保存错误日志
    private final boolean debug = false;
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    private byte type;
    private int code;


    //定义异常类型
    public final static byte TYPE_NETWORK = 0x01;
    public final static byte TYPE_SOCKET = 0x02;
    public final static byte TYPE_HTTP_CODE = 0x03;
    public final static byte TYPE_HTTP_ERROR = 0x04;
    public final static byte TYPE_XML = 0x05;
    public final static byte TYPE_IO = 0x06;
    public final static byte TYPE_RUN = 0x07;
    public final static byte TYPE_JSON = 0x08;


    public void setType(byte type) {
        this.type = type;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public byte getType() {
        return type;
    }

    public int getCode() {
        return code;
    }

    public AppException() {
        this.mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public AppException(byte type, int code, Exception excp) {
        super(excp);
        this.type = type;
        this.code = code;
        if (debug) {
            this.saveErrorLog(excp);
        }
    }

    //提示友好的错误信息
    public void makeToast(Context context) {
        switch (this.getType()) {
            case TYPE_HTTP_CODE:
                String err = context.getString(R.string.http_status_code_error, this.getCode());
                Toast.makeText(context, err, Toast.LENGTH_SHORT).show();
                break;
            case TYPE_HTTP_ERROR:
                Toast.makeText(context, R.string.http_exception_error, Toast.LENGTH_SHORT).show();
                break;
            case TYPE_SOCKET:
                Toast.makeText(context, R.string.socket_exception_error, Toast.LENGTH_SHORT).show();
                break;
            case TYPE_NETWORK:
                Toast.makeText(context, R.string.network_not_connected, Toast.LENGTH_SHORT).show();
                break;
            case TYPE_XML:
                Toast.makeText(context, R.string.xml_parser_failed, Toast.LENGTH_SHORT).show();
                break;
            case TYPE_JSON:
                Toast.makeText(context, R.string.xml_parser_failed, Toast.LENGTH_SHORT).show();
                break;
            case TYPE_IO:
                Toast.makeText(context, R.string.io_exception_error, Toast.LENGTH_SHORT).show();
                break;
            case TYPE_RUN:
                Toast.makeText(context, R.string.app_run_code_error, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    //保存异常日志
    private void saveErrorLog(Exception excp) {
        String errorlog = "errorlog.txt";
        String savePath = "";
        String logFilePath = "";
        FileWriter fw = null;
        PrintWriter pw = null;
        //判断是否挂载了SD卡
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            savePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            File file = new File(savePath);
            if (!file.exists()) {
                file.mkdirs();
            }
            logFilePath = savePath + errorlog;
        }
        //没有挂载SD卡，无法写文件

        if (logFilePath == "") {
            return;
        }
        File logFile = new File(logFilePath);
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
                fw = new FileWriter(logFile, true);
                pw = new PrintWriter(fw);
                pw.println("--------------------" + (new Date().toLocaleString()) + "---------------------");
                excp.printStackTrace(pw);
                pw.close();
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fw != null) {
                    try {
                        fw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (pw != null) {
                    pw.close();
                }
            }
        }
    }

    public static AppException http(int code) {
        return new AppException(TYPE_HTTP_CODE, code, null);
    }

    public static AppException http(Exception e) {
        return new AppException(TYPE_HTTP_ERROR, 0, e);
    }

    public static AppException socket(Exception e) {
        return new AppException(TYPE_SOCKET, 0, e);
    }

    public static AppException io(Exception e) {
        if (e instanceof UnknownHostException || e instanceof ConnectException) {
            return new AppException(TYPE_NETWORK, 0, e);
        } else if (e instanceof IOException) {
            return new AppException(TYPE_IO, 0, e);
        }
        return run(e);
    }

    public static AppException xml(Exception e) {
        return new AppException(TYPE_XML, 0, e);
    }

    public static AppException json(Exception e) {
        return new AppException(TYPE_JSON, 0, e);
    }

    public static AppException network(Exception e) {
        if (e instanceof UnknownHostException || e instanceof ConnectException) {
            return new AppException(TYPE_NETWORK, 0, e);
        } else if (e instanceof HttpException) {
            return http(e);
        } else if (e instanceof SocketException) {
            return socket(e);
        }
        return http(e);
    }

    public static AppException run(Exception e) {
        return new AppException(TYPE_RUN, 0, e);
    }


    //获取APP异常崩溃处理对象

    public static AppException getAppExceptionHandler() {
        return new AppException();
    }


    @Override
    public void uncaughtException(Thread t, Throwable e) {


        if (!handleException(e) && mDefaultHandler != null) {


            mDefaultHandler.uncaughtException(t, e);


        }
    }


    /**
     * 自定义异常处理:收集错误信息&发送错误报告
     *
     * @param
     * @return true:处理了该异常信息;否则返回false
     */
    private boolean handleException(Throwable e) {


        if (e == null) {

            return false;
        }


        Context context = AppManager.getAppManager().currentActivity();


        if (context == null) {


            return false;
        }


        getCrashReport(context, e);


        return true;
    }

    /**
     * 获取APP崩溃异常报告
     *
     * @param context
     * @param ex
     */
    private String getCrashReport(Context context, Throwable ex) {

        PackageInfo pinfo = ((AppContext) context.getApplicationContext()).getPackageInfo();
        StringBuffer exceptionStr = new StringBuffer();
        exceptionStr.append("Version: " + pinfo.versionName + "(" + pinfo.versionCode + ")\n");
        exceptionStr.append("Android: " + android.os.Build.VERSION.RELEASE + "(" + android.os.Build.MODEL + ")\n");
        exceptionStr.append("Exception: " + ex.getMessage() + "\n");
        StackTraceElement[] elements = ex.getStackTrace();
        for (int i = 0; i < elements.length; i++) {
            exceptionStr.append(elements[i].toString() + "\n");
        }
        return exceptionStr.toString();
    }
}
