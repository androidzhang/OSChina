package net.oschina.app.application;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;

import net.oschina.app.AppConfig;
import net.oschina.app.AppException;
import net.oschina.app.R;
import net.oschina.app.api.ApiClient;
import net.oschina.app.bean.ActiveList;
import net.oschina.app.bean.Barcode;
import net.oschina.app.bean.BlogList;
import net.oschina.app.bean.MessageList;
import net.oschina.app.bean.NewsList;
import net.oschina.app.bean.Notice;
import net.oschina.app.bean.Post;
import net.oschina.app.bean.PostList;
import net.oschina.app.bean.Result;
import net.oschina.app.bean.SearchList;
import net.oschina.app.bean.SoftwareCatalogList;
import net.oschina.app.bean.SoftwareList;
import net.oschina.app.bean.Tweet;
import net.oschina.app.bean.TweetList;
import net.oschina.app.bean.User;
import net.oschina.app.common.CyptoUtils;
import net.oschina.app.common.FileUtils;
import net.oschina.app.common.MethodsCompat;
import net.oschina.app.common.StringUtils;
import net.oschina.app.common.UIHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Properties;
import java.util.UUID;

import static android.nfc.tech.MifareUltralight.PAGE_SIZE;

/**
 * Created by zlx on 2017/7/13.
 * 全局应用程序类：用于保存和调用全局应用配置及访问网络数据
 */

public class AppContext extends Application {


    public static final int PAGE_SIZE = 20;//默认分页大小
    private static final int CACHE_TIME = 60*60000;//缓存失效时间
    private String saveImagePath;
    private boolean login = false;//用户是否登录
    private int loginUid = 0;    //登录用户的id
    private Handler unLoginHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);


            if (msg.what == 1) {

                UIHelper.ToastMessage(AppContext.this, getString(R.string.msg_login_error));
                UIHelper.showLoginDialog(AppContext.this);
            }


        }
    };

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
     *
     * @return
     */
    public PackageInfo getPackageInfo() {
        PackageInfo info = null;
        try {
            info = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace(System.err);
        }
        if (info == null) info = new PackageInfo();
        return info;
    }

    /**
     * 用户是否登录
     *
     * @return
     */
    public boolean isLogin() {
        return login;
    }

    /**
     * 动弹列表
     *
     * @param catalog   -1 热门，0 最新，大于0 某用户的动弹(uid)
     * @param pageIndex
     * @return
     * @throws AppException
     */
    public TweetList getTweetList(int catalog, int pageIndex, boolean isRefresh) throws AppException {
        TweetList list = null;
        String key = "tweetlist_" + catalog + "_" + pageIndex + "_" + PAGE_SIZE;
        if (isNetworkConnected() && (!isReadDataCache(key) || isRefresh)) {
            try {
                list = ApiClient.getTweetList(this, catalog, pageIndex, PAGE_SIZE);
                if (list != null && pageIndex == 0) {
                    Notice notice = list.getNotice();
                    list.setNotice(null);
                    list.setCacheKey(key);
                    saveObject(list, key);
                    list.setNotice(notice);
                }
            } catch (AppException e) {
                list = (TweetList) readObject(key);
                if (list == null)
                    throw e;
            }
        } else {
            list = (TweetList) readObject(key);
            if (list == null)
                list = new TweetList();
        }
        return list;
    }

    /**
     * 保存对象
     *
     * @param ser
     * @param file
     * @throws 。IOException
     */
    public boolean saveObject(Serializable ser, String file) {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = openFileOutput(file, MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(ser);
            oos.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                oos.close();
            } catch (Exception e) {
            }
            try {
                fos.close();
            } catch (Exception e) {
            }
        }
    }


    /**
     * 判断缓存数据是否可读
     *
     * @param cachefile
     * @return
     */
    private boolean isReadDataCache(String cachefile) {
        return readObject(cachefile) != null;
    }

    /**
     * 读取对象
     *
     * @param file
     * @return
     * @throws
     */
    public Serializable readObject(String file) {
        if (!isExistDataCache(file))
            return null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = openFileInput(file);
            ois = new ObjectInputStream(fis);
            return (Serializable) ois.readObject();
        } catch (FileNotFoundException e) {
        } catch (Exception e) {
            e.printStackTrace();
            //反序列化失败 - 删除缓存文件
            if (e instanceof InvalidClassException) {
                File data = getFileStreamPath(file);
                data.delete();
            }
        } finally {
            try {
                ois.close();
            } catch (Exception e) {
            }
            try {
                fis.close();
            } catch (Exception e) {
            }
        }
        return null;
    }

    /**
     * 判断缓存是否存在
     *
     * @param cachefile
     * @return
     */
    private boolean isExistDataCache(String cachefile) {
        boolean exist = false;
        File data = getFileStreamPath(cachefile);
        if (data.exists())
            exist = true;
        return exist;
    }

    /**
     * 检测网络是否可用
     *
     * @return
     */
    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting();
    }

    /**
     * 用户注销
     */
    public void Logout() {
        ApiClient.cleanCookie();
        this.cleanCookie();
        this.login = false;
        this.loginUid = 0;
    }

    /**
     * 清除保存的缓存
     */
    public void cleanCookie() {
        removeProperty(AppConfig.CONF_COOKIE);
    }


    /**
     * 未登录或修改密码后的处理
     */
    public Handler getUnLoginHandler() {
        return this.unLoginHandler;
    }

    /**
     * 获取App唯一标识
     *
     * @return
     */
    public String getAppId() {
        String uniqueID = getProperty(AppConfig.CONF_APP_UNIQUEID);
        if (StringUtils.isEmpty(uniqueID)) {
            uniqueID = UUID.randomUUID().toString();
            setProperty(AppConfig.CONF_APP_UNIQUEID, uniqueID);
        }
        return uniqueID;
    }

    /**
     * 获取内存中保存图片的路径
     *
     * @return
     */
    public String getSaveImagePath() {
        return saveImagePath;
    }

    /**
     * 设置内存中保存图片的路径
     *
     * @return
     */
    public void setSaveImagePath(String saveImagePath) {
        this.saveImagePath = saveImagePath;
    }

    /**
     * 是否Https登录
     *
     * @return
     */
    public boolean isHttpsLogin() {
        String perf_httpslogin = getProperty(AppConfig.CONF_HTTPS_LOGIN);
        //默认是http
        if (StringUtils.isEmpty(perf_httpslogin))
            return false;
        else
            return StringUtils.toBool(perf_httpslogin);
    }

    /**
     * 设置是是否Https登录
     *
     * @param b
     */
    public void setConfigHttpsLogin(boolean b) {
        setProperty(AppConfig.CONF_HTTPS_LOGIN, String.valueOf(b));
    }

    /**
     * 是否加载显示文章图片
     *
     * @return
     */
    public boolean isLoadImage() {
        String perf_loadimage = getProperty(AppConfig.CONF_LOAD_IMAGE);
        //默认是加载的
        if (StringUtils.isEmpty(perf_loadimage))
            return true;
        else
            return StringUtils.toBool(perf_loadimage);
    }

    /**
     * 设置是否加载文章图片
     *
     * @param b
     */
    public void setConfigLoadimage(boolean b) {
        setProperty(AppConfig.CONF_LOAD_IMAGE, String.valueOf(b));
    }

    /**
     * 是否左右滑动
     *
     * @return
     */
    public boolean isScroll() {
        String perf_scroll = getProperty(AppConfig.CONF_SCROLL);
        //默认是关闭左右滑动
        if (StringUtils.isEmpty(perf_scroll))
            return false;
        else
            return StringUtils.toBool(perf_scroll);
    }

    /**
     * 设置是否左右滑动
     *
     * @param b
     */
    public void setConfigScroll(boolean b) {
        setProperty(AppConfig.CONF_SCROLL, String.valueOf(b));
    }

    /**
     * 是否发出提示音
     *
     * @return
     */
    public boolean isVoice() {
        String perf_voice = getProperty(AppConfig.CONF_VOICE);
        //默认是开启提示声音
        if (StringUtils.isEmpty(perf_voice))
            return true;
        else
            return StringUtils.toBool(perf_voice);
    }

    /**
     * 设置是否发出提示音
     *
     * @param b
     */
    public void setConfigVoice(boolean b) {
        setProperty(AppConfig.CONF_VOICE, String.valueOf(b));
    }

    /**
     * 是否启动检查更新
     *
     * @return
     */
    public boolean isCheckUp() {
        String perf_checkup = getProperty(AppConfig.CONF_CHECKUP);
        //默认是开启
        if (StringUtils.isEmpty(perf_checkup))
            return true;
        else
            return StringUtils.toBool(perf_checkup);
    }

    /**
     * 设置启动检查更新
     *
     * @param b
     */
    public void setConfigCheckUp(boolean b) {
        setProperty(AppConfig.CONF_CHECKUP, String.valueOf(b));
    }


    /**
     * 判断当前版本是否兼容目标版本的方法
     *
     * @param VersionCode
     * @return
     */
    public static boolean isMethodsCompat(int VersionCode) {
        int currentVersion = android.os.Build.VERSION.SDK_INT;
        return currentVersion >= VersionCode;
    }


    /**
     * 清除app缓存
     */
    public void clearAppCache() {
        //清除webview缓存
//        File file = CacheManager.getCacheFileBaseDir();
//        if (file != null && file.exists() && file.isDirectory()) {
//            for (File item : file.listFiles()) {
//                item.delete();
//            }
//            file.delete();
//        }
        deleteDatabase("webview.db");
        deleteDatabase("webview.db-shm");
        deleteDatabase("webview.db-wal");
        deleteDatabase("webviewCache.db");
        deleteDatabase("webviewCache.db-shm");
        deleteDatabase("webviewCache.db-wal");
        //清除数据缓存
        clearCacheFolder(getFilesDir(), System.currentTimeMillis());
        clearCacheFolder(getCacheDir(), System.currentTimeMillis());
        //2.2版本才有将应用缓存转移到sd卡的功能
        if (isMethodsCompat(android.os.Build.VERSION_CODES.FROYO)) {
            clearCacheFolder(MethodsCompat.getExternalCacheDir(this), System.currentTimeMillis());
        }
        //清除编辑器保存的临时内容
        Properties props = getProperties();
        for (Object key : props.keySet()) {
            String _key = key.toString();
            if (_key.startsWith("temp"))
                removeProperty(_key);
        }
    }

    /**
     * 清除缓存目录
     *
     * @param dir      目录
     * @param 。numDays 当前系统时间
     * @return
     */
    private int clearCacheFolder(File dir, long curTime) {
        int deletedFiles = 0;
        if (dir != null && dir.isDirectory()) {
            try {
                for (File child : dir.listFiles()) {
                    if (child.isDirectory()) {
                        deletedFiles += clearCacheFolder(child, curTime);
                    }
                    if (child.lastModified() < curTime) {
                        if (child.delete()) {
                            deletedFiles++;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return deletedFiles;
    }

    /**
     * 用户登录验证
     *
     * @param account
     * @param pwd
     * @return
     * @throws AppException
     */
    public User loginVerify(String account, String pwd) throws AppException {
        return ApiClient.login(this, account, pwd);
    }


    /**
     * 保存登录信息
     *
     * @param 。username
     * @param 。pwd
     */
    public void saveLoginInfo(final User user) {
        this.loginUid = user.getUid();
        this.login = true;
        setProperties(new Properties() {{
            setProperty("user.uid", String.valueOf(user.getUid()));
            setProperty("user.name", user.getName());
            setProperty("user.face", FileUtils.getFileName(user.getFace()));//用户头像-文件名
            setProperty("user.account", user.getAccount());
            setProperty("user.pwd", CyptoUtils.encode("oschinaApp", user.getPwd()));
            setProperty("user.location", user.getLocation());
            setProperty("user.followers", String.valueOf(user.getFollowers()));
            setProperty("user.fans", String.valueOf(user.getFans()));
            setProperty("user.score", String.valueOf(user.getScore()));
            setProperty("user.isRememberMe", String.valueOf(user.isRememberMe()));//是否记住我的信息
        }});
    }

    /**
     * 清除登录信息
     */
    public void cleanLoginInfo() {
        this.loginUid = 0;
        this.login = false;
        removeProperty("user.uid", "user.name", "user.face", "user.account", "user.pwd",
                "user.location", "user.followers", "user.fans", "user.score", "user.isRememberMe");
    }

    /**
     * 动态列表
     *
     * @param catalog   1最新动态 2@我 3评论 4我自己
     * @param 。id
     * @param pageIndex
     * @return
     * @throws AppException
     */
    public ActiveList getActiveList(int catalog, int pageIndex, boolean isRefresh) throws AppException {
        ActiveList list = null;
        String key = "activelist_" + loginUid + "_" + catalog + "_" + pageIndex + "_" + PAGE_SIZE;
        if (isNetworkConnected() && (!isReadDataCache(key) || isRefresh)) {
            try {
                list = ApiClient.getActiveList(this, loginUid, catalog, pageIndex, PAGE_SIZE);
                if (list != null && pageIndex == 0) {
                    Notice notice = list.getNotice();
                    list.setNotice(null);
                    list.setCacheKey(key);
                    saveObject(list, key);
                    list.setNotice(notice);
                }
            } catch (AppException e) {
                list = (ActiveList) readObject(key);
                if (list == null)
                    throw e;
            }
        } else {
            list = (ActiveList) readObject(key);
            if (list == null)
                list = new ActiveList();
        }
        return list;
    }

    public void initLoginInfo() {
        User loginUser = getLoginInfo();
        if (loginUser != null && loginUser.getUid() > 0 && loginUser.isRememberMe()) {
            this.loginUid = loginUser.getUid();
            this.login = true;
        } else {
            this.Logout();
        }
    }

    //获取登录信息
    public  User getLoginInfo() {
        User lu = new User();
        lu.setUid(StringUtils.toInt(getProperty("user.uid"), 0));
        lu.setName(getProperty("user.name"));
        lu.setFace(getProperty("user.face"));
        lu.setAccount(getProperty("user.account"));
        lu.setPwd(CyptoUtils.decode("oschinaApp", getProperty("user.pwd")));
        lu.setLocation(getProperty("user.location"));
        lu.setFollowers(StringUtils.toInt(getProperty("user.followers"), 0));
        lu.setFans(StringUtils.toInt(getProperty("user.fans"), 0));
        lu.setScore(StringUtils.toInt(getProperty("user.score"), 0));
        lu.setRememberMe(StringUtils.toBool(getProperty("user.isRememberMe")));
        return lu;
    }

    /**
     * 获取登录用户id
     *
     * @return
     */
    public int getLoginUid() {
        return this.loginUid;
    }

    /**
     * 新闻列表
     *
     * @param catalog
     * @param pageIndex
     * @param /pageSize
     * @return
     * @throws /ApiException
     */
    public NewsList getNewsList(int catalog, int pageIndex, boolean isRefresh) throws AppException {
        NewsList list = null;
        String key = "newslist_" + catalog + "_" + pageIndex + "_" + PAGE_SIZE;
        if (isNetworkConnected() && (!isReadDataCache(key) || isRefresh)) {
            try {
                list = ApiClient.getNewsList(this, catalog, pageIndex, PAGE_SIZE);
                if (list != null && pageIndex == 0) {
                    Notice notice = list.getNotice();
                    list.setNotice(null);
                    list.setCacheKey(key);
                    saveObject(list, key);
                    list.setNotice(notice);
                }
            } catch (AppException e) {
                list = (NewsList) readObject(key);
                if (list == null)
                    throw e;
            }
        } else {
            list = (NewsList) readObject(key);
            if (list == null)
                list = new NewsList();
        }
        return list;
    }

    /**
     * 清空通知消息
     *
     * @param uid
     * @param type 1:@我的信息 2:未读消息 3:评论个数 4:新粉丝个数
     * @return
     * @throws AppException
     */
    public Result noticeClear(int uid, int type) throws AppException {
        return ApiClient.noticeClear(this, uid, type);
    }

    /**
     * 应用程序是否发出提示音
     *
     * @return
     */
    public boolean isAppSound() {
        return isAudioNormal() && isVoice();
    }

    /**
     * 检测当前系统声音是否为正常模式
     *
     * @return
     */
    public boolean isAudioNormal() {
        AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        return mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
    }


    /**
     * 帖子列表
     *
     * @param catalog
     * @param pageIndex
     * @return
     * @throws 、ApiException
     */
    public PostList getPostList(int catalog, int pageIndex, boolean isRefresh) throws AppException {
        PostList list = null;
        String key = "postlist_" + catalog + "_" + pageIndex + "_" + PAGE_SIZE;
        if (isNetworkConnected() && (!isReadDataCache(key) || isRefresh)) {
            try {
                list = ApiClient.getPostList(this, catalog, pageIndex, PAGE_SIZE);
                if (list != null && pageIndex == 0) {
                    Notice notice = list.getNotice();
                    list.setNotice(null);
                    list.setCacheKey(key);
                    saveObject(list, key);
                    list.setNotice(notice);
                }
            } catch (AppException e) {
                list = (PostList) readObject(key);
                if (list == null)
                    throw e;
            }
        } else {
            list = (PostList) readObject(key);
            if (list == null)
                list = new PostList();
        }
        return list;
    }

    /**
     * 博客列表
     *
     * @param type      推荐：recommend 最新：latest
     * @param pageIndex
     * @return
     * @throws AppException
     */
    public BlogList getBlogList(String type, int pageIndex, boolean isRefresh) throws AppException {
        BlogList list = null;
        String key = "bloglist_" + type + "_" + pageIndex + "_" + PAGE_SIZE;
        if (isNetworkConnected() && (!isReadDataCache(key) || isRefresh)) {
            try {
                list = ApiClient.getBlogList(this, type, pageIndex, PAGE_SIZE);
                if (list != null && pageIndex == 0) {
                    Notice notice = list.getNotice();
                    list.setNotice(null);
                    list.setCacheKey(key);
                    saveObject(list, key);
                    list.setNotice(notice);
                }
            } catch (AppException e) {
                list = (BlogList) readObject(key);
                if (list == null)
                    throw e;
            }
        } else {
            list = (BlogList) readObject(key);
            if (list == null)
                list = new BlogList();
        }
        return list;
    }

    /**
     * 留言列表
     *
     * @param pageIndex
     * @return
     * @throws AppException
     */
    public MessageList getMessageList(int pageIndex, boolean isRefresh) throws AppException {
        MessageList list = null;
        String key = "messagelist_" + loginUid + "_" + pageIndex + "_" + PAGE_SIZE;
        if (isNetworkConnected() && (!isReadDataCache(key) || isRefresh)) {
            try {
                list = ApiClient.getMessageList(this, loginUid, pageIndex, PAGE_SIZE);
                if (list != null && pageIndex == 0) {
                    Notice notice = list.getNotice();
                    list.setNotice(null);
                    list.setCacheKey(key);
                    saveObject(list, key);
                    list.setNotice(notice);
                }
            } catch (AppException e) {
                list = (MessageList) readObject(key);
                if (list == null)
                    throw e;
            }
        } else {
            list = (MessageList) readObject(key);
            if (list == null)
                list = new MessageList();
        }
        return list;
    }

    /**
     * 获取用户通知信息
     *
     * @param uid
     * @return
     * @throws AppException
     */
    public Notice getUserNotice(int uid) throws AppException {
        return ApiClient.getUserNotice(this, uid);
    }

    /**
     * 获取搜索列表
     * @param catalog 全部:all 新闻:news  问答:post 软件:software 博客:blog 代码:code
     * @param content 搜索的内容
     * @param pageIndex
     * @param pageSize
     * @return
     * @throws AppException
     */
    public SearchList getSearchList(String catalog, String content, int pageIndex, int pageSize) throws AppException {
        return ApiClient.getSearchList(this, catalog, content, pageIndex, pageSize);
    }

    /**
     * 发帖子
     * @param post （uid、title、catalog、content、isNoticeMe）
     * @return
     * @throws AppException
     */
    public Result pubPost(Post post) throws AppException {
        return ApiClient.pubPost(this, post);
    }


    /**
     * 发动弹
     * @param 、Tweet-uid & msg & image
     * @return
     * @throws AppException
     */
    public Result pubTweet(Tweet tweet) throws AppException {
        return ApiClient.pubTweet(this, tweet);
    }

    /**
     * 软件分类列表
     * @param tag 第一级:0  第二级:tag
     * @return
     * @throws AppException
     */
    public SoftwareCatalogList getSoftwareCatalogList(int tag) throws AppException {
        SoftwareCatalogList list = null;
        String key = "softwarecataloglist_"+tag;
        if(isNetworkConnected() && isCacheDataFailure(key)) {
            try{
                list = ApiClient.getSoftwareCatalogList(this, tag);
                if(list != null){
                    Notice notice = list.getNotice();
                    list.setNotice(null);
                    list.setCacheKey(key);
                    saveObject(list, key);
                    list.setNotice(notice);
                }
            }catch(AppException e){
                list = (SoftwareCatalogList)readObject(key);
                if(list == null)
                    throw e;
            }
        } else {
            list = (SoftwareCatalogList)readObject(key);
            if(list == null)
                list = new SoftwareCatalogList();
        }
        return list;
    }

    /**
     * 判断缓存是否失效
     * @param cachefile
     * @return
     */
    public boolean isCacheDataFailure(String cachefile)
    {
        boolean failure = false;
        File data = getFileStreamPath(cachefile);
        if(data.exists() && (System.currentTimeMillis() - data.lastModified()) > CACHE_TIME)
            failure = true;
        else if(!data.exists())
            failure = true;
        return failure;
    }


    /**
     * 软件列表
     * @param searchTag 软件分类  推荐:recommend 最新:time 热门:view 国产:list_cn
     * @param pageIndex
     * @return
     * @throws AppException
     */
    public SoftwareList getSoftwareList(String searchTag, int pageIndex, boolean isRefresh) throws AppException {
        SoftwareList list = null;
        String key = "softwarelist_"+searchTag+"_"+pageIndex+"_"+PAGE_SIZE;
        if(isNetworkConnected() && (!isReadDataCache(key) || isRefresh)) {
            try{
                list = ApiClient.getSoftwareList(this, searchTag, pageIndex, PAGE_SIZE);
                if(list != null && pageIndex == 0){
                    Notice notice = list.getNotice();
                    list.setNotice(null);
                    list.setCacheKey(key);
                    saveObject(list, key);
                    list.setNotice(notice);
                }
            }catch(AppException e){
                list = (SoftwareList)readObject(key);
                if(list == null)
                    throw e;
            }
        } else {
            list = (SoftwareList)readObject(key);
            if(list == null)
                list = new SoftwareList();
        }
        return list;
    }
    /**
     * 软件分类的软件列表
     * @param searchTag 从softwarecatalog_list获取的tag
     * @param pageIndex
     * @return
     * @throws AppException
     */
    public SoftwareList getSoftwareTagList(int searchTag, int pageIndex, boolean isRefresh) throws AppException {
        SoftwareList list = null;
        String key = "softwaretaglist_"+searchTag+"_"+pageIndex+"_"+PAGE_SIZE;
        if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
            try{
                list = ApiClient.getSoftwareTagList(this, searchTag, pageIndex, PAGE_SIZE);
                if(list != null && pageIndex == 0){
                    Notice notice = list.getNotice();
                    list.setNotice(null);
                    list.setCacheKey(key);
                    saveObject(list, key);
                    list.setNotice(notice);
                }
            }catch(AppException e){
                list = (SoftwareList)readObject(key);
                if(list == null)
                    throw e;
            }
        } else {
            list = (SoftwareList)readObject(key);
            if(list == null)
                list = new SoftwareList();
        }
        return list;
    }

    /**
     * 扫描二维码签到
     * @param barcode
     * @return
     * @throws AppException
     */
    public String signIn(Barcode barcode) throws AppException{
        return ApiClient.signIn(this, barcode);
    }
}