package net.oschina.app;

import android.os.Bundle;
import android.view.KeyEvent;

import net.oschina.app.ui.BaseActivity;
import net.oschina.app.ui.DoubleClickExitHelper;

public class MainActivity extends BaseActivity {

    private DoubleClickExitHelper clickExitHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        clickExitHelper = new DoubleClickExitHelper(this);


    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean flag = true;


        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //是否退出應用
            return clickExitHelper.onKeyDown(keyCode, event);
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            // 展示快捷栏&判断是否登录


        } else if (keyCode == KeyEvent.KEYCODE_SEARCH) {

        } else {

            flag = super.onKeyDown(keyCode, event);
        }


        return flag;
    }
}
