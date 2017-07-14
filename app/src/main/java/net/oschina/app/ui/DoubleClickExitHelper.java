package net.oschina.app.ui;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.widget.Toast;

import net.oschina.app.AppManager;
import net.oschina.app.R;

/**
 * 双击退出
 * Created by zlx on 2017/7/14.
 */

public class DoubleClickExitHelper {

    private Activity mActivity;
    private Handler mHandler;
    private boolean isOnKeyBacking;
    private Toast mBackToast;

    public DoubleClickExitHelper(Activity activity) {
        mActivity = activity;
        mHandler = new Handler(Looper.getMainLooper());
    }

    //activity onkeydown 事件

    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode != KeyEvent.KEYCODE_BACK) {
            return false;

        }
        if (isOnKeyBacking) {
            mHandler.removeCallbacks(onBackTimeRunnable);

            if (mBackToast != null) {
                mBackToast.cancel();
            }


            // 退出

            AppManager.getAppManager().AppExit(mActivity);
            return true;
        } else {
            isOnKeyBacking = true;
            if (mBackToast == null) {

                mBackToast = Toast.makeText(mActivity, R.string.back_exit_tips, 2000);

            }
            mBackToast.show();
            mHandler.postDelayed(onBackTimeRunnable,2000);


        return true;
        }
    }

    private Runnable onBackTimeRunnable = new Runnable() {
        @Override
        public void run() {


            isOnKeyBacking = false;

            if (mBackToast != null) {

                mBackToast.cancel();
            }


        }
    };
}
