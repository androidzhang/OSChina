package net.oschina.app.api;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import net.oschina.app.AppException;
import net.oschina.app.application.AppContext;
import net.oschina.app.bean.ActiveList;
import net.oschina.app.bean.Barcode;
import net.oschina.app.bean.BlogList;
import net.oschina.app.bean.MessageList;
import net.oschina.app.bean.News;
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
import net.oschina.app.bean.URLs;
import net.oschina.app.bean.Update;
import net.oschina.app.bean.User;
import net.oschina.app.bean.WellcomeImage;
import net.oschina.app.common.FileUtils;
import net.oschina.app.common.ImageUtils;
import net.oschina.app.common.StringUtils;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API客户端接口：用于访问网络数据
 * Created by zlx on 2017/7/14.
 */

public class ApiClient {
    private final static int TIMEOUT_CONNECTION = 20000;
    private final static int TIMEOUT_SOCKET = 20000;
    private final static int RETRY_TIME = 3;
    public static final String UTF_8 = "UTF-8";
    private static String appCookie;
    private static String appUserAgent;

    /**
     * 获取动弹列表
     *
     * @param uid
     * @param pageIndex
     * @param pageSize
     * @return
     * @throws AppException
     */
    public static TweetList getTweetList(AppContext appContext, final int uid, final int pageIndex, final int pageSize) throws AppException {
        String newUrl = _MakeURL(URLs.TWEET_LIST, new HashMap<String, Object>() {{
            put("uid", uid);
            put("pageIndex", pageIndex);
            put("pageSize", pageSize);
        }});

        try {
            return TweetList.parse(http_get(appContext, newUrl));
        } catch (Exception e) {
            if (e instanceof AppException)
                throw (AppException) e;
            throw AppException.network(e);
        }
    }

    /**
     * get请求URL
     *
     * @param url
     * @throws AppException
     */
    private static InputStream http_get(AppContext appContext, String url) throws AppException {
        String cookie = getCookie(appContext);
        String userAgent = getUserAgent(appContext);

        HttpClient httpClient = null;
        GetMethod httpGet = null;

        String responseBody = "";
        int time = 0;
        do {
            try {
                httpClient = getHttpClient();
                httpGet = getHttpGet(url, cookie, userAgent);
                int statusCode = httpClient.executeMethod(httpGet);
                if (statusCode != HttpStatus.SC_OK) {
                    throw AppException.http(statusCode);
                }
                responseBody = httpGet.getResponseBodyAsString();
                break;
            } catch (HttpException e) {
                time++;
                if (time < RETRY_TIME) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                    }
                    continue;
                }
                // 发生致命的异常，可能是协议不对或者返回的内容有问题
                e.printStackTrace();
                throw AppException.http(e);
            } catch (IOException e) {
                time++;
                if (time < RETRY_TIME) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                    }
                    continue;
                }
                // 发生网络异常
                e.printStackTrace();
                throw AppException.network(e);
            } finally {
                // 释放连接
                httpGet.releaseConnection();
                httpClient = null;
            }
        } while (time < RETRY_TIME);

        //responseBody = responseBody.replaceAll("\\p{Cntrl}", "\r\n");
        if (responseBody.contains("result") && responseBody.contains("errorCode") && appContext.containsProperty("user.uid")) {
            try {
                Result res = Result.parse(new ByteArrayInputStream(responseBody.getBytes()));
                if (res.getErrorCode() == 0) {
                    appContext.Logout();
                    appContext.getUnLoginHandler().sendEmptyMessage(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ByteArrayInputStream(responseBody.getBytes());
    }

    private static GetMethod getHttpGet(String url, String cookie, String userAgent) {
        GetMethod httpGet = new GetMethod(url);
        // 设置 请求超时时间
        httpGet.getParams().setSoTimeout(TIMEOUT_SOCKET);
        httpGet.setRequestHeader("Host", URLs.HOST);
        httpGet.setRequestHeader("Connection", "Keep-Alive");
        httpGet.setRequestHeader("Cookie", cookie);
        httpGet.setRequestHeader("User-Agent", userAgent);
        return httpGet;
    }

    private static HttpClient getHttpClient() {
        HttpClient httpClient = new HttpClient();
        // 设置 HttpClient 接收 Cookie,用与浏览器一样的策略
        httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        // 设置 默认的超时重试处理策略
        httpClient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());
        // 设置 连接超时时间
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(TIMEOUT_CONNECTION);
        // 设置 读数据超时时间
        httpClient.getHttpConnectionManager().getParams().setSoTimeout(TIMEOUT_SOCKET);
        // 设置 字符集
        httpClient.getParams().setContentCharset(UTF_8);
        return httpClient;
    }

    private static String getUserAgent(AppContext appContext) {
        if (appUserAgent == null || appUserAgent == "") {
            StringBuilder ua = new StringBuilder("OSChina.NET");
            ua.append('/' + appContext.getPackageInfo().versionName + '_' + appContext.getPackageInfo().versionCode);//App版本
            ua.append("/Android");//手机系统平台
            ua.append("/" + android.os.Build.VERSION.RELEASE);//手机系统版本
            ua.append("/" + android.os.Build.MODEL); //手机型号
            ua.append("/" + appContext.getAppId());//客户端唯一标识
            appUserAgent = ua.toString();
        }
        return appUserAgent;
    }

    private static String getCookie(AppContext appContext) {
        if (appCookie == null || appCookie == "") {
            appCookie = appContext.getProperty("cookie");
        }
        return appCookie;
    }

    private static String _MakeURL(String p_url, Map<String, Object> params) {
        StringBuilder url = new StringBuilder(p_url);
        if (url.indexOf("?") < 0)
            url.append('?');
        for (String name : params.keySet()) {
            url.append('&');
            url.append(name);
            url.append('=');
            url.append(String.valueOf(params.get(name)));
            //不做URLEncoder处理
            //url.append(URLEncoder.encode(String.valueOf(params.get(name)), UTF_8));
        }
        return url.toString().replace("?&", "?");
    }


    public static void cleanCookie() {
        appCookie = "";
    }

    /**
     * 登录， 自动处理cookie
     *
     * @param >url
     * @param username
     * @param pwd
     * @return
     * @throws AppException
     */
    public static User login(AppContext appContext, String username, String pwd) throws AppException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("username", username);
        params.put("pwd", pwd);
        params.put("keep_login", 1);

        String loginurl = URLs.LOGIN_VALIDATE_HTTP;
        if (appContext.isHttpsLogin()) {
            loginurl = URLs.LOGIN_VALIDATE_HTTPS;
        }

        try {
            return User.parse(_post(appContext, loginurl, params, null));
        } catch (Exception e) {
            if (e instanceof AppException)
                throw (AppException) e;
            throw AppException.network(e);
        }
    }


    /**
     * 公用post方法
     *
     * @param url
     * @param params
     * @param files
     * @throws AppException
     */
    private static InputStream _post(AppContext appContext, String url, Map<String, Object> params, Map<String, File> files) throws AppException {
        //System.out.println("post_url==> "+url);
        String cookie = getCookie(appContext);
        String userAgent = getUserAgent(appContext);

        HttpClient httpClient = null;
        PostMethod httpPost = null;

        //post表单参数处理
        int length = (params == null ? 0 : params.size()) + (files == null ? 0 : files.size());
        Part[] parts = new Part[length];
        int i = 0;
        if (params != null)
            for (String name : params.keySet()) {
                parts[i++] = new StringPart(name, String.valueOf(params.get(name)), UTF_8);
                //System.out.println("post_key==> "+name+"    value==>"+String.valueOf(params.get(name)));
            }
        if (files != null)
            for (String file : files.keySet()) {
                try {
                    parts[i++] = new FilePart(file, files.get(file));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                //System.out.println("post_key_file==> "+file);
            }

        String responseBody = "";
        int time = 0;
        do {
            try {
                httpClient = getHttpClient();
                httpPost = getHttpPost(url, cookie, userAgent);
                httpPost.setRequestEntity(new MultipartRequestEntity(parts, httpPost.getParams()));
                int statusCode = httpClient.executeMethod(httpPost);
                if (statusCode != HttpStatus.SC_OK) {
                    throw AppException.http(statusCode);
                } else if (statusCode == HttpStatus.SC_OK) {
                    Cookie[] cookies = httpClient.getState().getCookies();
                    String tmpcookies = "";
                    for (Cookie ck : cookies) {
                        tmpcookies += ck.toString() + ";";
                    }
                    //保存cookie
                    if (appContext != null && tmpcookies != "") {
                        appContext.setProperty("cookie", tmpcookies);
                        appCookie = tmpcookies;
                    }
                }
                responseBody = httpPost.getResponseBodyAsString();
                //System.out.println("XMLDATA=====>"+responseBody);
                break;
            } catch (HttpException e) {
                time++;
                if (time < RETRY_TIME) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                    }
                    continue;
                }
                // 发生致命的异常，可能是协议不对或者返回的内容有问题
                e.printStackTrace();
                throw AppException.http(e);
            } catch (IOException e) {
                time++;
                if (time < RETRY_TIME) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                    }
                    continue;
                }
                // 发生网络异常
                e.printStackTrace();
                throw AppException.network(e);
            } finally {
                // 释放连接
                httpPost.releaseConnection();
                httpClient = null;
            }
        } while (time < RETRY_TIME);

        responseBody = responseBody.replaceAll("\\p{Cntrl}", "");
        if (responseBody.contains("result") && responseBody.contains("errorCode") && appContext.containsProperty("user.uid")) {
            try {
                Result res = Result.parse(new ByteArrayInputStream(responseBody.getBytes()));
                if (res.getErrorCode() == 0) {
                    appContext.Logout();
                    appContext.getUnLoginHandler().sendEmptyMessage(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ByteArrayInputStream(responseBody.getBytes());
    }

    private static PostMethod getHttpPost(String url, String cookie, String userAgent) {
        PostMethod httpPost = new PostMethod(url);
        // 设置 请求超时时间
        httpPost.getParams().setSoTimeout(TIMEOUT_SOCKET);
        httpPost.setRequestHeader("Host", URLs.HOST);
        httpPost.setRequestHeader("Connection", "Keep-Alive");
        httpPost.setRequestHeader("Cookie", cookie);
        httpPost.setRequestHeader("User-Agent", userAgent);
        return httpPost;
    }

    /**
     * 获取动态列表
     *
     * @param uid
     * @param catalog   1最新动态  2@我  3评论  4我自己
     * @param pageIndex
     * @param pageSize
     * @return
     * @throws AppException
     */
    public static ActiveList getActiveList(AppContext appContext, final int uid, final int catalog, final int pageIndex, final int pageSize) throws AppException {
        String newUrl = _MakeURL(URLs.ACTIVE_LIST, new HashMap<String, Object>() {{
            put("uid", uid);
            put("catalog", catalog);
            put("pageIndex", pageIndex);
            put("pageSize", pageSize);
        }});

        try {
            return ActiveList.parse(http_get(appContext, newUrl));
        } catch (Exception e) {
            if (e instanceof AppException)
                throw (AppException) e;
            throw AppException.network(e);
        }
    }

    /**
     * 检查版本更新
     * @param 。url
     * @return
     */
    public static Update checkVersion(AppContext appContext) throws AppException {
        try {
            return Update.parse(http_get(appContext, URLs.UPDATE_VERSION));
        } catch (Exception e) {
            if (e instanceof AppException)
                throw (AppException) e;
            throw AppException.network(e);
        }
    }
    /**
     * 获取网络图片
     * @param url
     * @return
     */
    public static Bitmap getNetBitmap(String url) throws AppException {
        HttpClient httpClient = null;
        GetMethod httpGet = null;
        Bitmap bitmap = null;
        int time = 0;
        do{
            try
            {
                httpClient = getHttpClient();
                httpGet = getHttpGet(url, null, null);
                int statusCode = httpClient.executeMethod(httpGet);
                if (statusCode != HttpStatus.SC_OK) {
                    throw AppException.http(statusCode);
                }
                InputStream inStream = httpGet.getResponseBodyAsStream();
                bitmap = BitmapFactory.decodeStream(inStream);
                inStream.close();
                break;
            } catch (HttpException e) {
                time++;
                if(time < RETRY_TIME) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {}
                    continue;
                }
                // 发生致命的异常，可能是协议不对或者返回的内容有问题
                e.printStackTrace();
                throw AppException.http(e);
            } catch (IOException e) {
                time++;
                if(time < RETRY_TIME) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {}
                    continue;
                }
                // 发生网络异常
                e.printStackTrace();
                throw AppException.network(e);
            } finally {
                // 释放连接
                httpGet.releaseConnection();
                httpClient = null;
            }
        }while(time < RETRY_TIME);
        return bitmap;
    }

    /**
     * 获取资讯列表
     * @param /url
     * @param catalog
     * @param pageIndex
     * @param pageSize
     * @return
     * @throws AppException
     */
    public static NewsList getNewsList(AppContext appContext, final int catalog, final int pageIndex, final int pageSize) throws AppException {
        String newUrl = _MakeURL(URLs.NEWS_LIST, new HashMap<String, Object>(){{
            put("catalog", catalog);
            put("pageIndex", pageIndex);
            put("pageSize", pageSize);
        }});
        try{
            return NewsList.parse(http_get(appContext, newUrl));
        }catch(Exception e){
            if(e instanceof AppException)
                throw (AppException)e;
            throw AppException.network(e);
        }
    }
    /**
     * 清空通知消息
     * @param uid
     * @param type 1:@我的信息 2:未读消息 3:评论个数 4:新粉丝个数
     * @return
     * @throws AppException
     */
    public static Result noticeClear(AppContext appContext, int uid, int type) throws AppException {
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("uid", uid);
        params.put("type", type);

        try{
            return Result.parse(_post(appContext, URLs.NOTICE_CLEAR, params, null));
        }catch(Exception e){
            if(e instanceof AppException)
                throw (AppException)e;
            throw AppException.network(e);
        }
    }

    /**
     * 获取帖子列表
     * @param 。、、url
     * @param catalog
     * @param pageIndex
     * @return
     * @throws AppException
     */
    public static PostList getPostList(AppContext appContext, final int catalog, final int pageIndex, final int pageSize) throws AppException {
        String newUrl = _MakeURL(URLs.POST_LIST, new HashMap<String, Object>(){{
            put("catalog", catalog);
            put("pageIndex", pageIndex);
            put("pageSize", pageSize);
        }});

        try{
            return PostList.parse(http_get(appContext, newUrl));
        }catch(Exception e){
            if(e instanceof AppException)
                throw (AppException)e;
            throw AppException.network(e);
        }
    }
    /**
     * 获取博客列表
     * @param type 推荐：recommend 最新：latest
     * @param pageIndex
     * @param pageSize
     * @return
     * @throws AppException
     */
    public static BlogList getBlogList(AppContext appContext, final String type, final int pageIndex, final int pageSize) throws AppException {
        String newUrl = _MakeURL(URLs.BLOG_LIST, new HashMap<String, Object>(){{
            put("type", type);
            put("pageIndex", pageIndex);
            put("pageSize", pageSize);
        }});

        try{
            return BlogList.parse(http_get(appContext, newUrl));
        }catch(Exception e){
            if(e instanceof AppException)
                throw (AppException)e;
            throw AppException.network(e);
        }
    }

    /**
     * 获取留言列表
     * @param uid
     * @param pageIndex
     * @return
     * @throws AppException
     */
    public static MessageList getMessageList(AppContext appContext, final int uid, final int pageIndex, final int pageSize) throws AppException {
        String newUrl = _MakeURL(URLs.MESSAGE_LIST, new HashMap<String, Object>(){{
            put("uid", uid);
            put("pageIndex", pageIndex);
            put("pageSize", pageSize);
        }});

        try{
            return MessageList.parse(http_get(appContext, newUrl));
        }catch(Exception e){
            if(e instanceof AppException)
                throw (AppException)e;
            throw AppException.network(e);
        }
    }
    /**
     * 检查是否有可下载的欢迎界面图片
     * @param appContext
     * @return
     * @throws AppException
     */
    public static void checkBackGround(AppContext appContext) throws AppException {
        try{
            WellcomeImage update = WellcomeImage.parse(http_get(appContext, URLs.UPDATE_VERSION));
            String filePath = FileUtils.getAppCache(appContext, "welcomeback");
            // 如果没有图片的链接地址则返回
            if(StringUtils.isEmpty(update.getDownloadUrl())) {
                return;
            }
            if(update.isUpdate()) {
                String url = update.getDownloadUrl();
                String fileName = update.getStartDate().replace("-", "") + "-" + update.getEndDate().replace("-", "");
                List<File> files = FileUtils.listPathFiles(filePath);
                if (!files.isEmpty()) {
                    if(files.get(0).getName().equalsIgnoreCase(fileName)) {
                        return;
                    }
                }
                Bitmap photo = getNetBitmap(url);
                ImageUtils.saveImageToSD(appContext,
                        filePath + fileName + ".png", photo, 100);
            } else {
                FileUtils.clearFileWithPath(filePath);
            }
        }catch(Exception e){
            if(e instanceof AppException)
                throw (AppException)e;
            throw AppException.network(e);
        }
    }
    /**
     * 获取用户通知信息
     * @param uid
     * @return
     * @throws AppException
     */
    public static Notice getUserNotice(AppContext appContext, int uid) throws AppException {
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("uid", uid);

        try{
            return Notice.parse(_post(appContext, URLs.USER_NOTICE, params, null));
        }catch(Exception e){
            if(e instanceof AppException)
                throw (AppException)e;
            throw AppException.network(e);
        }
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
    public static SearchList getSearchList(AppContext appContext, String catalog, String content, int pageIndex, int pageSize) throws AppException {
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("catalog", catalog);
        params.put("content", content);
        params.put("pageIndex", pageIndex);
        params.put("pageSize", pageSize);

        try{
            return SearchList.parse(_post(appContext, URLs.SEARCH_LIST, params, null));
        }catch(Exception e){
            if(e instanceof AppException)
                throw (AppException)e;
            throw AppException.network(e);
        }
    }
    /**
     * 发帖子
     * @param post （uid、title、catalog、content、isNoticeMe）
     * @return
     * @throws AppException
     */
    public static Result pubPost(AppContext appContext, Post post) throws AppException {
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("uid", post.getAuthorId());
        params.put("title", post.getTitle());
        params.put("catalog", post.getCatalog());
        params.put("content", post.getBody());
        params.put("isNoticeMe", post.getIsNoticeMe());

        try{
            return http_post(appContext, URLs.POST_PUB, params, null);
        }catch(Exception e){
            if(e instanceof AppException)
                throw (AppException)e;
            throw AppException.network(e);
        }
    }

    /**
     * post请求URL
     * @param url
     * @param params
     * @param files
     * @throws AppException
     * @throws IOException
     * @throws
     */
    private static Result http_post(AppContext appContext, String url, Map<String, Object> params, Map<String,File> files) throws AppException, IOException {
        return Result.parse(_post(appContext, url, params, files));
    }

    /**
     * 发动弹
     * @param 、Tweet-uid & msg & image
     * @return
     * @throws AppException
     */
    public static Result pubTweet(AppContext appContext, Tweet tweet) throws AppException {
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("uid", tweet.getAuthorId());
        params.put("msg", tweet.getBody());

        Map<String, File> files = new HashMap<String, File>();
        if(tweet.getImageFile() != null)
            files.put("img", tweet.getImageFile());
        if (tweet.getAmrFile() != null)
            files.put("amr", tweet.getAmrFile());

        try{
            return http_post(appContext, URLs.TWEET_PUB, params, files);
        }catch(Exception e){
            if(e instanceof AppException)
                throw (AppException)e;
            throw AppException.network(e);
        }
    }
    /**
     * 软件分类列表
     * @param tag 第一级:0  第二级:tag
     * @return
     * @throws AppException
     */
    public static SoftwareCatalogList getSoftwareCatalogList(AppContext appContext,final int tag) throws AppException {
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("tag", tag);

        try{
            return SoftwareCatalogList.parse(_post(appContext, URLs.SOFTWARECATALOG_LIST, params, null));
        }catch(Exception e){
            if(e instanceof AppException)
                throw (AppException)e;
            throw AppException.network(e);
        }
    }
    /**
     * 软件列表
     * @param searchTag 软件分类  推荐:recommend 最新:time 热门:view 国产:list_cn
     * @param pageIndex
     * @param pageSize
     * @return
     * @throws AppException
     */
    public static SoftwareList getSoftwareList(AppContext appContext,final String searchTag,final int pageIndex,final int pageSize) throws AppException {
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("searchTag", searchTag);
        params.put("pageIndex", pageIndex);
        params.put("pageSize", pageSize);

        try{
            return SoftwareList.parse(_post(appContext, URLs.SOFTWARE_LIST, params, null));
        }catch(Exception e){
            if(e instanceof AppException)
                throw (AppException)e;
            throw AppException.network(e);
        }
    }

    /**
     * 软件分类的软件列表
     * @param searchTag 从softwarecatalog_list获取的tag
     * @param pageIndex
     * @param pageSize
     * @return
     * @throws AppException
     */
    public static SoftwareList getSoftwareTagList(AppContext appContext,final int searchTag,final int pageIndex,final int pageSize) throws AppException {
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("searchTag", searchTag);
        params.put("pageIndex", pageIndex);
        params.put("pageSize", pageSize);

        try{
            return SoftwareList.parse(_post(appContext, URLs.SOFTWARETAG_LIST, params, null));
        }catch(Exception e){
            if(e instanceof AppException)
                throw (AppException)e;
            throw AppException.network(e);
        }
    }

    /**
     * 二维码扫描签到
     * @param appContext
     * @param barcode
     * @return
     * @throws AppException
     */
    public static String signIn(AppContext appContext, Barcode barcode) throws AppException {
        try{
            return StringUtils.toConvertString(http_get(appContext, barcode.getUrl()));
        }catch(Exception e){
            if(e instanceof AppException)
                throw (AppException)e;
            throw AppException.network(e);
        }
    }
    
    /**
     * 获取资讯的详情
     * @param 、url
     * @param news_id
     * @return
     * @throws AppException
     */
    public static News getNewsDetail(AppContext appContext, final int news_id) throws AppException {
        String newUrl = _MakeURL(URLs.NEWS_DETAIL, new HashMap<String, Object>(){{
            put("id", news_id);
        }});
        
        try{
            return News.parse(http_get(appContext, newUrl));
        }catch(Exception e){
            if(e instanceof AppException)
                throw (AppException)e;
            throw AppException.network(e);
        }
    }
    /**
     * 用户删除收藏
     * @param uid 用户UID
     * @param objid 比如是新闻ID 或者问答ID 或者动弹ID
     * @param type 1:软件 2:话题 3:博客 4:新闻 5:代码
     * @return
     * @throws AppException
     */
    public static Result delFavorite(AppContext appContext, int uid, int objid, int type) throws AppException {
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("uid", uid);
        params.put("objid", objid);
        params.put("type", type);
        
        try{
            return http_post(appContext, URLs.FAVORITE_DELETE, params, null);
        }catch(Exception e){
            if(e instanceof AppException)
                throw (AppException)e;
            throw AppException.network(e);
        }
    }
}
