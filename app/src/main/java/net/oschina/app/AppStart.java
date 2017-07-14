package net.oschina.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;

import net.oschina.app.common.FileUtils;
import net.oschina.app.common.StringUtils;

import java.io.File;
import java.util.List;

public class AppStart extends Activity {

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        View view = View.inflate(this, R.layout.start, null);


        LinearLayout welcome = (LinearLayout) view.findViewById(R.id.app_start_view);

        check(welcome);

        setContentView(view);

        //渐变展示启动屏

        AlphaAnimation aa = new AlphaAnimation(0, 1.0f);


        aa.setDuration(2000);


        view.startAnimation(aa);

        aa.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {


                goToMain();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    //检查是否需要换图片
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void check(LinearLayout view) {
        String path = FileUtils.getAppCache(this, "welcomeback");
        List<File> files = FileUtils.listPathFiles(path);

        if (!files.isEmpty()) {
            File f = files.get(0);
            long time[] = getTime(f.getName());
            long today = StringUtils.getToday();
            if (today >= time[0] && today <= time[1]) {
                view.setBackground(Drawable.createFromPath(f.getAbsolutePath()));
            }
        }


    }

    //分析显示的时间
    private long[] getTime(String time) {
        long res[] = new long[2];
        try {
            time = time.substring(0, time.indexOf("."));
            String t[] = time.split("-");
            res[0] = Long.parseLong(t[0]);
            if (t.length >= 2) {
                res[1] = Long.parseLong(t[1]);
            } else {
                res[1] = Long.parseLong(t[0]);
            }
        } catch (Exception e) {
        }
        return res;


    }
}
