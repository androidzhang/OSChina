package net.oschina.app.ui;

import net.oschina.app.MainActivity;
import net.oschina.app.R;
import net.oschina.app.application.AppContext;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
/**
 * 通知信息广播接收器
 * @author liux (http://my.oschina.net/liux)
 * @version 1.0
 * @created 2012-4-16
 */
public class BroadCast extends BroadcastReceiver {
	private final static int NOTIFICATION_ID = R.layout.activity_main;
	private static int lastNoticeCount;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String ACTION_NAME = intent.getAction();
		if("net.oschina.app.action.APPWIDGET_UPDATE".equals(ACTION_NAME))
		{
			int atmeCount = intent.getIntExtra("atmeCount", 0);//@我
			int msgCount = intent.getIntExtra("msgCount", 0);//留言
			int reviewCount = intent.getIntExtra("reviewCount", 0);//评论
			int newFansCount = intent.getIntExtra("newFansCount", 0);//新粉丝
			int activeCount = atmeCount + reviewCount + msgCount + newFansCount;//信息总数
			//动态-总数
			if(MainActivity.bv_active != null){
				if(activeCount > 0){
					MainActivity.bv_active.setText(activeCount+"");
					MainActivity.bv_active.show();
				}else{
					MainActivity.bv_active.setText("");
					MainActivity.bv_active.hide();
				}
			}
			//@我
			if(MainActivity.bv_atme != null){
				if(atmeCount > 0){
					MainActivity.bv_atme.setText(atmeCount+"");
					MainActivity.bv_atme.show();
				}else{
					MainActivity.bv_atme.setText("");
					MainActivity.bv_atme.hide();
				}
			}
			//评论
			if(MainActivity.bv_review != null){
				if(reviewCount > 0){
					MainActivity.bv_review.setText(reviewCount+"");
					MainActivity.bv_review.show();
				}else{
					MainActivity.bv_review.setText("");
					MainActivity.bv_review.hide();
				}
			}
			//留言
			if(MainActivity.bv_message != null){
				if(msgCount > 0){
					MainActivity.bv_message.setText(msgCount+"");
					MainActivity.bv_message.show();
				}else{
					MainActivity.bv_message.setText("");
					MainActivity.bv_message.hide();
				}
			}
			
			//通知栏显示
			this.notification(context, activeCount);
		}
	}

	private void notification(Context context, int noticeCount){
		//创建 NotificationManager
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		String contentTitle = "开源中国";
		String contentText = "您有 " + noticeCount + " 条最新信息";
		int _lastNoticeCount;
		
		//判断是否发出通知信息
		if(noticeCount == 0)
		{
			notificationManager.cancelAll();
			lastNoticeCount = 0;
			return;
		}
		else if(noticeCount == lastNoticeCount)
		{
			return;
		}
		else
		{
			_lastNoticeCount = lastNoticeCount;
			lastNoticeCount = noticeCount;
		}
		
		
		if(noticeCount > _lastNoticeCount)
		{
			String noticeTitle = "您有 " + (noticeCount-_lastNoticeCount) + " 条最新信息";
		}
		
		//设置点击通知跳转
		Intent intent = new Intent(context, MainActivity.class);
		intent.putExtra("NOTICE", true);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK); 
		
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		//设置最新信息
		//新建一个Notification管理器;
//API level 11
		Notification.Builder builder = new Notification.Builder(context);//新建Notification.Builder对象
//PendingIntent点击通知后所跳转的页面
		builder.setContentTitle(contentTitle);
		builder.setContentText(contentText);
		builder.setSmallIcon(R.mipmap.ic_launcher);
		builder.setContentIntent(contentIntent);//执行intent
		Notification notification = builder.getNotification();//将builder对象转换为普通的notification
		notification.flags |= Notification.FLAG_AUTO_CANCEL;//点击通知后通知消失
		notificationManager.notify(1,notification);//运行notification
		
		//设置点击清除通知
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		
		if(noticeCount > _lastNoticeCount) 
		{
			//设置通知方式
			notification.defaults |= Notification.DEFAULT_LIGHTS;
			
			//设置通知音-根据app设置是否发出提示音
			if(((AppContext)context.getApplicationContext()).isAppSound())
				notification.sound = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.notificationsound);
			
			//设置振动 <需要加上用户权限android.permission.VIBRATE>
			//notification.vibrate = new long[]{100, 250, 100, 500};
		}
		
		//发出通知
		notificationManager.notify(NOTIFICATION_ID, notification);		
	}
	
}
