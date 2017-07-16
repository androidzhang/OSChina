package net.oschina.app.common;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import net.oschina.app.MainActivity;
import net.oschina.app.R;
import net.oschina.app.adapter.GridViewFaceAdapter;
import net.oschina.app.application.AppContext;
import net.oschina.app.bean.Active;
import net.oschina.app.bean.News;
import net.oschina.app.bean.Notice;
import net.oschina.app.bean.Result;
import net.oschina.app.bean.Tweet;
import net.oschina.app.bean.URLs;
import net.oschina.app.ui.About;
import net.oschina.app.ui.FeedBack;
import net.oschina.app.ui.LoginDialog;
import net.oschina.app.ui.QuestionPub;
import net.oschina.app.ui.Search;
import net.oschina.app.ui.Setting;
import net.oschina.app.ui.TweetPub;
import net.oschina.app.ui.UserInfo;
import net.oschina.app.widget.LinkView;
import net.oschina.app.widget.MyQuickAction;
import net.oschina.app.widget.QuickAction;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 应用程序UI工具包：封装UI相关的一些操作
 * Created by zlx on 2017/7/14.
 */

public class UIHelper {

    public final static int LISTVIEW_ACTION_INIT = 0x01;
    public final static int LISTVIEW_ACTION_REFRESH = 0x02;
    public final static int LISTVIEW_ACTION_SCROLL = 0x03;
    public final static int LISTVIEW_ACTION_CHANGE_CATALOG = 0x04;

    public final static int LISTVIEW_DATA_MORE = 0x01;
    public final static int LISTVIEW_DATA_LOADING = 0x02;
    public final static int LISTVIEW_DATA_FULL = 0x03;
    public final static int LISTVIEW_DATA_EMPTY = 0x04;

    public final static int LISTVIEW_DATATYPE_NEWS = 0x01;
    public final static int LISTVIEW_DATATYPE_BLOG = 0x02;
    public final static int LISTVIEW_DATATYPE_POST = 0x03;
    public final static int LISTVIEW_DATATYPE_TWEET = 0x04;
    public final static int LISTVIEW_DATATYPE_ACTIVE = 0x05;
    public final static int LISTVIEW_DATATYPE_MESSAGE = 0x06;
    public final static int LISTVIEW_DATATYPE_COMMENT = 0x07;

    public final static int REQUEST_CODE_FOR_RESULT = 0x01;
    public final static int REQUEST_CODE_FOR_REPLY = 0x02;
    /**
     * 表情图片匹配
     */
    private static Pattern facePattern = Pattern
            .compile("\\[{1}([0-9]\\d*)\\]{1}");

    /**
     * 弹出Toast消息
     *
     * @param msg
     */
    public static void ToastMessage(Context cont, String msg) {
        Toast.makeText(cont, msg, Toast.LENGTH_SHORT).show();
    }

    public static void ToastMessage(Context cont, int msg) {
        Toast.makeText(cont, msg, Toast.LENGTH_SHORT).show();
    }

    public static void ToastMessage(Context cont, String msg, int time) {
        Toast.makeText(cont, msg, time).show();
    }

    /**
     * 发送通知广播
     *
     * @param context
     * @param notice
     */
    public static void sendBroadCast(Context context, Notice notice) {
        if (!((AppContext) context.getApplicationContext()).isLogin()
                || notice == null)
            return;
        Intent intent = new Intent("net.oschina.app.action.APPWIDGET_UPDATE");
        intent.putExtra("atmeCount", notice.getAtmeCount());
        intent.putExtra("msgCount", notice.getMsgCount());
        intent.putExtra("reviewCount", notice.getReviewCount());
        intent.putExtra("newFansCount", notice.getNewFansCount());
        context.sendBroadcast(intent);
    }

    /**
     * 显示登录页面
     *
     * @param >activity
     */
    public static void showLoginDialog(Context context) {
        Intent intent = new Intent(context, LoginDialog.class);
        if (context instanceof MainActivity)
            intent.putExtra("LOGINTYPE", LoginDialog.LOGIN_MAIN);
        else if (context instanceof Setting)
            intent.putExtra("LOGINTYPE", LoginDialog.LOGIN_SETTING);
        else
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }


    /**
     * 点击返回监听事件
     *
     * @param activity
     * @return
     */
    public static View.OnClickListener finish(final Activity activity) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                activity.finish();
            }
        };
    }


    /**
     * 用户登录或注销
     *
     * @param activity
     */
    public static void loginOrLogout(Activity activity) {
        AppContext ac = (AppContext) activity.getApplication();
        if (ac.isLogin()) {
            ac.Logout();
            ToastMessage(activity, "已退出登录");
        } else {
            showLoginDialog(activity);
        }
    }

    /**
     * 显示我的资料
     *
     * @param context
     */
    public static void showUserInfo(Activity context) {
        AppContext ac = (AppContext) context.getApplicationContext();
        if (!ac.isLogin()) {
            showLoginDialog(context);
        } else {
            Intent intent = new Intent(context, UserInfo.class);
            context.startActivity(intent);
        }
    }

    public static void changeSettingIsLoadImage(Activity activity, boolean b) {
        AppContext ac = (AppContext) activity.getApplication();
        ac.setConfigLoadimage(b);
    }

    /**
     * 清除app缓存
     *
     * @param activity
     */
    public static void clearAppCache(Activity activity) {
        final AppContext ac = (AppContext) activity.getApplication();
        final Handler handler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    ToastMessage(ac, "缓存清除成功");
                } else {
                    ToastMessage(ac, "缓存清除失败");
                }
            }
        };
        new Thread() {
            public void run() {
                Message msg = new Message();
                try {
                    ac.clearAppCache();
                    msg.what = 1;
                } catch (Exception e) {
                    e.printStackTrace();
                    msg.what = -1;
                }
                handler.sendMessage(msg);
            }
        }.start();
    }

    /**
     * 显示用户反馈
     *
     * @param context
     */
    public static void showFeedBack(Context context) {
        Intent intent = new Intent(context, FeedBack.class);
        context.startActivity(intent);
    }

    /**
     * 显示关于我们
     *
     * @param context
     */
    public static void showAbout(Context context) {
        Intent intent = new Intent(context, About.class);
        context.startActivity(intent);
    }

    /**
     * 显示搜索界面
     *
     * @param context
     */
    public static void showSearch(Context context) {
        Intent intent = new Intent(context, Search.class);
        context.startActivity(intent);
    }

    /**
     * 显示我要提问页面
     *
     * @param context
     */
    public static void showQuestionPub(Context context) {
        Intent intent = new Intent(context, QuestionPub.class);
        context.startActivity(intent);
    }

    /**
     * 显示动弹一下页面
     *
     * @param context
     */
    public static void showTweetPub(Activity context) {
        Intent intent = new Intent(context, TweetPub.class);
        context.startActivityForResult(intent, REQUEST_CODE_FOR_RESULT);
    }

    /**
     * 快捷栏显示登录与登出
     *
     * @param activity
     * @param qa
     */
    public static void showSettingLoginOrLogout(Activity activity,
                                                QuickAction qa) {
        if (((AppContext) activity.getApplication()).isLogin()) {
            qa.setIcon(MyQuickAction.buildDrawable(activity,
                    R.mipmap.ic_menu_logout));
            qa.setTitle(activity.getString(R.string.main_menu_logout));
        } else {
            qa.setIcon(MyQuickAction.buildDrawable(activity,
                    R.mipmap.ic_menu_login));
            qa.setTitle(activity.getString(R.string.main_menu_login));
        }
    }

    public static void showSoftware(MainActivity mainActivity) {

//        Intent intent = new Intent(mainActivity, SoftwareLib.class);
//        mainActivity.startActivity(intent);

    }

    public static void showCapture(MainActivity mainActivity) {
//        Intent intent = new Intent(mainActivity, CaptureActivity.class);
//        mainActivity.startActivity(intent);

    }

    public static void showSetting(Context context) {

        Intent intent = new Intent(context, Setting.class);
        context.startActivity(intent);
    }

    /**
     * 新闻超链接点击跳转
     *
     * @param context
     * @param 。newsId
     * @param 。newsType
     * @param 。objId
     */
    public static void showNewsRedirect(Context context, News news) {
        String url = news.getUrl();
        // url为空-旧方法
        if (StringUtils.isEmpty(url)) {
            int newsId = news.getId();
            int newsType = news.getNewType().type;
            String objId = news.getNewType().attachment;
            switch (newsType) {
                case News.NEWSTYPE_NEWS:
                    showNewsDetail(context, newsId);
                    break;
                case News.NEWSTYPE_SOFTWARE:
                    showSoftwareDetail(context, objId);
                    break;
                case News.NEWSTYPE_POST:
                    showQuestionDetail(context, StringUtils.toInt(objId));
                    break;
                case News.NEWSTYPE_BLOG:
                    showBlogDetail(context, StringUtils.toInt(objId));
                    break;
            }
        } else {
            showUrlRedirect(context, url);
        }
    }

    /**
     * url跳转
     *
     * @param context
     * @param url
     */
    public static void showUrlRedirect(Context context, String url) {
        URLs urls = URLs.parseURL(url);
        if (urls != null) {
            showLinkRedirect(context, urls.getObjType(), urls.getObjId(),
                    urls.getObjKey());
        } else {
            openBrowser(context, url);
        }
    }

    public static void showLinkRedirect(Context context, int objType,
                                        int objId, String objKey) {
        switch (objType) {
            case URLs.URL_OBJ_TYPE_NEWS:
                showNewsDetail(context, objId);
                break;
            case URLs.URL_OBJ_TYPE_QUESTION:
                showQuestionDetail(context, objId);
                break;
            case URLs.URL_OBJ_TYPE_QUESTION_TAG:
                showQuestionListByTag(context, objKey);
                break;
            case URLs.URL_OBJ_TYPE_SOFTWARE:
                showSoftwareDetail(context, objKey);
                break;
            case URLs.URL_OBJ_TYPE_ZONE:
                showUserCenter(context, objId, objKey);
                break;
            case URLs.URL_OBJ_TYPE_TWEET:
                showTweetDetail(context, objId);
                break;
            case URLs.URL_OBJ_TYPE_BLOG:
                showBlogDetail(context, objId);
                break;
            case URLs.URL_OBJ_TYPE_OTHER:
                openBrowser(context, objKey);
                break;
        }
    }

    /**
     * 显示动弹详情及评论
     *
     * @param context
     * @param tweetId
     */
    public static void showTweetDetail(Context context, int tweetId) {
//        Intent intent = new Intent(context, TweetDetail.class);
//        intent.putExtra("tweet_id", tweetId);
//        context.startActivity(intent);
    }

    /**
     * 显示用户动态
     *
     * @param context
     * @param 。uid
     * @param hisuid
     * @param hisname
     */
    public static void showUserCenter(Context context, int hisuid,
                                      String hisname) {
//        Intent intent = new Intent(context, UserCenter.class);
//        intent.putExtra("his_id", hisuid);
//        intent.putExtra("his_name", hisname);
//        context.startActivity(intent);
    }

    /**
     * 显示相关Tag帖子列表
     *
     * @param context
     * @param tag
     */
    public static void showQuestionListByTag(Context context, String tag) {
//        Intent intent = new Intent(context, QuestionTag.class);
//        intent.putExtra("post_tag", tag);
//        context.startActivity(intent);
    }

    /**
     * 打开浏览器
     *
     * @param context
     * @param url
     */
    public static void openBrowser(Context context, String url) {
        try {
            Uri uri = Uri.parse(url);
            Intent it = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(it);
        } catch (Exception e) {
            e.printStackTrace();
            ToastMessage(context, "无法浏览此网页", 500);
        }
    }

    /**
     * 显示博客详情
     *
     * @param context
     * @param blogId
     */
    public static void showBlogDetail(Context context, int blogId) {
//        Intent intent = new Intent(context, BlogDetail.class);
//        intent.putExtra("blog_id", blogId);
//        context.startActivity(intent);
    }

    /**
     * 显示帖子详情
     *
     * @param context
     * @param postId
     */
    public static void showQuestionDetail(Context context, int postId) {
//        Intent intent = new Intent(context, QuestionDetail.class);
//        intent.putExtra("post_id", postId);
//        context.startActivity(intent);
    }


    /**
     * 显示软件详情
     *
     * @param context
     * @param ident
     */
    public static void showSoftwareDetail(Context context, String ident) {
//        Intent intent = new Intent(context, SoftwareDetail.class);
//        intent.putExtra("ident", ident);
//        context.startActivity(intent);
    }


    /**
     * 显示新闻详情
     *
     * @param context
     * @param newsId
     */
    public static void showNewsDetail(Context context, int newsId) {
//        Intent intent = new Intent(context, NewsDetail.class);
//        intent.putExtra("news_id", newsId);
//        context.startActivity(intent);
    }

    public static void showImageZoomDialog(Context context, String imgUrl) {
//        Intent intent = new Intent(context, ImageZoomDialog.class);
//        intent.putExtra("img_url", imgUrl);
//        context.startActivity(intent);
    }

    /**
     * 组合动态的动作文本
     *
     * @param objecttype
     * @param objectcatalog
     * @param objecttitle
     * @return
     */
    @SuppressLint("NewApi")
    public static SpannableString parseActiveAction(String author,
                                                    int objecttype, int objectcatalog, String objecttitle) {
        String title = "";
        int start = 0;
        int end = 0;
        if (objecttype == 32 && objectcatalog == 0) {
            title = "加入了开源中国";
        } else if (objecttype == 1 && objectcatalog == 0) {
            title = "添加了开源项目 " + objecttitle;
        } else if (objecttype == 2 && objectcatalog == 1) {
            title = "在讨论区提问：" + objecttitle;
        } else if (objecttype == 2 && objectcatalog == 2) {
            title = "发表了新话题：" + objecttitle;
        } else if (objecttype == 3 && objectcatalog == 0) {
            title = "发表了博客 " + objecttitle;
        } else if (objecttype == 4 && objectcatalog == 0) {
            title = "发表一篇新闻 " + objecttitle;
        } else if (objecttype == 5 && objectcatalog == 0) {
            title = "分享了一段代码 " + objecttitle;
        } else if (objecttype == 6 && objectcatalog == 0) {
            title = "发布了一个职位：" + objecttitle;
        } else if (objecttype == 16 && objectcatalog == 0) {
            title = "在新闻 " + objecttitle + " 发表评论";
        } else if (objecttype == 17 && objectcatalog == 1) {
            title = "回答了问题：" + objecttitle;
        } else if (objecttype == 17 && objectcatalog == 2) {
            title = "回复了话题：" + objecttitle;
        } else if (objecttype == 17 && objectcatalog == 3) {
            title = "在 " + objecttitle + " 对回帖发表评论";
        } else if (objecttype == 18 && objectcatalog == 0) {
            title = "在博客 " + objecttitle + " 发表评论";
        } else if (objecttype == 19 && objectcatalog == 0) {
            title = "在代码 " + objecttitle + " 发表评论";
        } else if (objecttype == 20 && objectcatalog == 0) {
            title = "在职位 " + objecttitle + " 发表评论";
        } else if (objecttype == 101 && objectcatalog == 0) {
            title = "回复了动态：" + objecttitle;
        } else if (objecttype == 100) {
            title = "更新了动态";
        }
        title = author + " " + title;
        SpannableString sp = new SpannableString(title);
        // 设置用户名字体大小、加粗、高亮
        sp.setSpan(new AbsoluteSizeSpan(14, true), 0, author.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0,
                author.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new ForegroundColorSpan(Color.parseColor("#0e5986")), 0,
                author.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        // 设置标题字体大小、高亮
        if (!StringUtils.isEmpty(objecttitle)) {
            start = title.indexOf(objecttitle);
            if (objecttitle.length() > 0 && start > 0) {
                end = start + objecttitle.length();
                sp.setSpan(new AbsoluteSizeSpan(14, true), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                sp.setSpan(
                        new ForegroundColorSpan(Color.parseColor("#0e5986")),
                        start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return sp;
    }

    /**
     * 组合动态的回复文本
     *
     * @param name
     * @param body
     * @return
     */
    public static SpannableString parseActiveReply(String name, String body) {
        SpannableString sp = new SpannableString(name + "：" + body);
        // 设置用户名字体加粗、高亮
        sp.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0,
                name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new ForegroundColorSpan(Color.parseColor("#0e5986")), 0,
                name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sp;
    }

    /**
     * 动态点击跳转到相关新闻、帖子等
     *
     * @param context
     * @param /id
     * @param /catalog 0其他 1新闻 2帖子 3动弹 4博客
     */
    public static void showActiveRedirect(Context context, Active active) {
        String url = active.getUrl();
        // url为空-旧方法
        if (StringUtils.isEmpty(url)) {
            int id = active.getObjectId();
            int catalog = active.getActiveType();
            switch (catalog) {
                case Active.CATALOG_OTHER:
                    // 其他-无跳转
                    break;
                case Active.CATALOG_NEWS:
                    showNewsDetail(context, id);
                    break;
                case Active.CATALOG_POST:
                    showQuestionDetail(context, id);
                    break;
                case Active.CATALOG_TWEET:
                    showTweetDetail(context, id);
                    break;
                case Active.CATALOG_BLOG:
                    showBlogDetail(context, id);
                    break;
            }
        } else {
            showUrlRedirect(context, url);
        }
    }

    /**
     * 组合消息文本
     *
     * @param name
     * @param body
     * @return
     */
    public static void parseMessageSpan(LinkView view, String name,
                                        String body, String action) {
        Spanned span = null;
        SpannableStringBuilder style = null;
        int start = 0;
        int end = 0;
        String content = null;
        if (StringUtils.isEmpty(action)) {
            content = name + "：" + body;
            span = Html.fromHtml(content);
            view.setText(span);
            end = name.length();
        } else {
            content = action + name + "：" + body;
            span = Html.fromHtml(content);
            view.setText(span);
            start = action.length();
            end = start + name.length();
        }
        view.setMovementMethod(LinkMovementMethod.getInstance());

        Spannable sp = (Spannable) view.getText();
        URLSpan[] urls = span.getSpans(0, sp.length(), URLSpan.class);

        style = new SpannableStringBuilder(view.getText());
        // style.clearSpans();// 这里会清除之前所有的样式
        for (URLSpan url : urls) {
            style.removeSpan(url);// 只需要移除之前的URL样式，再重新设置
            LinkView.MyURLSpan myURLSpan = view.new MyURLSpan(url.getURL());
            style.setSpan(myURLSpan, span.getSpanStart(url),
                    span.getSpanEnd(url), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // 设置用户名字体加粗、高亮
        style.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start,
                end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        style.setSpan(new ForegroundColorSpan(Color.parseColor("#0e5986")),
                start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        view.setText(style);
    }

    /**
     * 显示留言对话页面
     *
     * @param context
     * @param /catalog
     * @param friendid
     */
    public static void showMessageDetail(Context context, int friendid,
                                         String friendname) {
//        Intent intent = new Intent(context, MessageDetail.class);
//        intent.putExtra("friend_name", friendname);
//        intent.putExtra("friend_id", friendid);
//        context.startActivity(intent);
    }

    /**
     * 获取TextWatcher对象
     *
     * @param context
     * @param /tmlKey
     * @return
     */
    public static TextWatcher getTextWatcher(final Activity context,
                                             final String temlKey) {
        return new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                // 保存当前EditText正在编辑的内容
                ((AppContext) context.getApplication()).setProperty(temlKey,
                        s.toString());
            }

            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            public void afterTextChanged(Editable s) {
            }
        };
    }

    /**
     * 编辑器显示保存的草稿
     *
     * @param context
     * @param editer
     * @param temlKey
     */
    public static void showTempEditContent(Activity context, EditText editer,
                                           String temlKey) {
        String tempContent = ((AppContext) context.getApplication())
                .getProperty(temlKey);
        if (!StringUtils.isEmpty(tempContent)) {
            SpannableStringBuilder builder = parseFaceByText(context,
                    tempContent);
            editer.setText(builder);
            editer.setSelection(tempContent.length());// 设置光标位置
        }
    }

    /**
     * 将[12]之类的字符串替换为表情
     *
     * @param context
     * @param content
     */
    public static SpannableStringBuilder parseFaceByText(Context context,
                                                         String content) {
        SpannableStringBuilder builder = new SpannableStringBuilder(content);
        Matcher matcher = facePattern.matcher(content);
        while (matcher.find()) {
            // 使用正则表达式找出其中的数字
            int position = StringUtils.toInt(matcher.group(1));
            int resId = 0;
            try {
                if (position > 65 && position < 102)
                    position = position - 1;
                else if (position > 102)
                    position = position - 2;
                resId = GridViewFaceAdapter.getImageIds()[position];
                Drawable d = context.getResources().getDrawable(resId);
                d.setBounds(0, 0, 35, 35);// 设置表情图片的显示大小
                ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
                builder.setSpan(span, matcher.start(), matcher.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } catch (Exception e) {
            }
        }
        return builder;
    }

    /**
     * 清除文字
     * @param cont
     * @param editer
     */
    public static void showClearWordsDialog(final Context cont,
                                            final EditText editer, final TextView numwords) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cont);
        builder.setTitle(R.string.clearwords);
        builder.setPositiveButton(R.string.sure,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        // 清除文字
                        editer.setText("");
                        numwords.setText("160");
                    }
                });
        builder.setNegativeButton(R.string.cancle,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.show();
    }
    /**
     * 发送广播-发布动弹
     *
     * @param context
     * @param 、notice
     */
    public static void sendBroadCastTweet(Context context, int what,
                                          Result res, Tweet tweet) {
        if (res == null && tweet == null)
            return;
        Intent intent = new Intent("net.oschina.app.action.APP_TWEETPUB");
        intent.putExtra("MSG_WHAT", what);
        if (what == 1)
            intent.putExtra("RESULT", res);
        else
            intent.putExtra("TWEET", tweet);
        context.sendBroadcast(intent);
    }
    /**
     * 显示路径选择对话框
     *
     * @param context
     */
//    public static void showFilePathDialog(Activity context,
//                                          ChooseCompleteListener listener) {
//        new PathChooseDialog(context, listener).show();
//    }
}
