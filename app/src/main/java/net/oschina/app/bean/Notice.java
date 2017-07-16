package net.oschina.app.bean;
import android.util.Xml;
import net.oschina.app.common.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
/**
 * 通知信息实体类
 * Created by zlx on 2017/7/14.
 */
public class Notice implements Serializable {
    public final static String UTF8 = "UTF-8";
    public final static String NODE_ROOT = "oschina";
    public final static int TYPE_ATME = 1;
    public final static int TYPE_MESSAGE = 2;
    public final static int TYPE_COMMENT = 3;
    public final static int TYPE_NEWFAN = 4;
    private int atmeCount;
    private int msgCount;
    private int reviewCount;
    private int newFansCount;
    public int getAtmeCount() {
        return atmeCount;
    }
    public void setAtmeCount(int atmeCount) {
        this.atmeCount = atmeCount;
    }
    public int getMsgCount() {
        return msgCount;
    }
    public void setMsgCount(int msgCount) {
        this.msgCount = msgCount;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public int getNewFansCount() {
        return newFansCount;
    }

    public void setNewFansCount(int newFansCount) {
        this.newFansCount = newFansCount;
    }

    public static Notice parse(InputStream inputStream) {
        Notice notice = null;
        //获得XmlPullParser解析器
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(inputStream, UTF8);
            //获得解析到的事件类别，这里有开始文档，结束文档，开始标签，结束标签，文本等等事件。
            int eventType = parser.getEventType();
            //一直循环，直到文档结束
            if (eventType != parser.END_DOCUMENT) {
                String tag = parser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        //通知信息
                        if (tag.equalsIgnoreCase("notice")) {
                            notice = new Notice();
                        } else if (notice != null) {
                            if (tag.equalsIgnoreCase("atmeCount")) {
                                notice.setAtmeCount(StringUtils.toInt(parser.nextText(), 0));
                            } else if (tag.equalsIgnoreCase("msgCount")) {
                                notice.setMsgCount(StringUtils.toInt(parser.nextText(), 0));
                            } else if (tag.equalsIgnoreCase("reviewCount")) {
                                notice.setReviewCount(StringUtils.toInt(parser.nextText(), 0));
                            } else if (tag.equalsIgnoreCase("newFansCount")) {
                                notice.setNewFansCount(StringUtils.toInt(parser.nextText(), 0));
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }

                //如果xml没有结束，则导航到下一个节点
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return notice;
    }
}
