package com.lcb.one.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Toast;

import com.lcb.one.R;
import com.lcb.one.base.BaseActivity;
import com.lcb.one.listener.PermissionListener;
import com.lcb.one.util.Logs;

import java.util.List;

public class PermissionActivity extends BaseActivity implements View.OnClickListener {
    private static final int REQUEST_CODE = 1;
    String tag = "PermissionActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);
        initView();
    }

    private void initView() {
        findViewById(R.id.back_permission).setOnClickListener(this);
        findViewById(R.id.permission_btn).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_permission:
                finish();
                break;
            case R.id.permission_btn:
//                doByActivity();
                doByBaseActivity();
                break;
        }
    }

    /**
     * 通过本类方法查询权限
     */
    private void doByActivity() {
        boolean isRefuse = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE);
        Logs.d("    53   " + isRefuse + "      " + ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE));


        //检查权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            //进入到这里代表没有权限.
            Logs.e("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz");
            if (isRefuse) {
                //已经禁止提示了
                msg_one();
                Toast.makeText(this, "您已禁止该权限，需要重新开启。", Toast.LENGTH_SHORT).show();
                Logs.v("11111111111111111111111111111111111111111111");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CODE);
                Logs.v("2222222222222222222222222222222222222");
            }

        } else {
            call();
        }
    }


    /**
     * 打电话
     */
    public void call() {
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            Uri uri = Uri.parse("tel:" + "10086");
            intent.setData(uri);
            startActivity(intent);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE:
                Logs.v("3333333333333333333333333333333333333333333333");
                boolean isGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                Logs.d(tag + "  94  " + grantResults[0]);

                if (grantResults.length > 0 && isGranted) {
                    Logs.w("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
                    //用户同意授权
                    call();
                } else {
                    Logs.w("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
                    //用户拒绝授权
                    msg_one();
                }
                break;
        }
    }


    /**
     * 第一种警告的样式
     */
    private void msg_one() {
        //当拒绝了授权后，为提升用户体验，可以以弹窗的方式引导用户到设置中去进行设置
        new AlertDialog.Builder(this)
                .setTitle("警告")
                .setMessage("需要开启权限才能使用此功能")
                .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //引导用户到设置中去进行设置
                        Intent intent = new Intent();
                        intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                        intent.setData(Uri.fromParts("package", getPackageName(), null));
                        startActivity(intent);
                    }
                })
                .setNegativeButton("取消", null)
                .create()
                .show();
    }


    /**
     * 第二种警告样式
     */
    private void msg_two() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(" 如果未授权将导致一些功能无法实现！").setTitle("警告");
        builder.setPositiveButton("去手动授权", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();

    }


    private void doByBaseActivity() {
        requestRunPermisssion(new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.CAMERA}, new PermissionListener() {
            @Override
            public void onGranted() {
                //表示所有权限都授权了
                Toast.makeText(PermissionActivity.this, "所有权限都授权了，可以搞事情了", Toast.LENGTH_SHORT).show();
                //我们可以执行打电话的逻辑
                call();
            }

            @Override
            public void onDenied(List<String> deniedPermission) {
                String s = "被拒绝的权限：";
                for (String permission : deniedPermission) {
                    s += permission + "\n";
                }
                Toast.makeText(PermissionActivity.this, s, Toast.LENGTH_SHORT).show();

                //提示用户到系统去设置权限
                msg_one();
            }
        });
    }


}
