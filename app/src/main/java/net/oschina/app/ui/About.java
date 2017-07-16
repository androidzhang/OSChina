package net.oschina.app.ui;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.oschina.app.R;
import net.oschina.app.common.UpdateManager;

/**
 * Created by zlx on 2017/7/14.
 */

public class About extends BaseActivity {

    private TextView mVersion;
    private Button mUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //获取客户端版本信息
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            mVersion = (TextView) findViewById(R.id.about_version);
            mVersion.setText("版本：" + info.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace(System.err);
        }
        mUpdate = (Button) findViewById(R.id.about_update);
        mUpdate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                UpdateManager.getUpdateManager().checkAppUpdate(About.this, true);
            }
        });
    }
}
