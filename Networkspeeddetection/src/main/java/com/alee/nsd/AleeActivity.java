package com.alee.nsd;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.alee.nsd.service.NSDService;
import com.alee.nsd.utils.SystemBarTintManager;
import com.alee.nsd.utils.Utils;
import com.gc.materialdesign.widgets.ColorSelector;
import com.gc.materialdesign.widgets.Dialog;

import java.util.List;

import me.xiaopan.switchbutton.SwitchButton;

public class AleeActivity extends AppCompatActivity {

    private final boolean close = true;
    private final boolean open = false;
    private Button about_bt,text_color_bt;
    private Toolbar toolbar;
    private SeekBar position_sb,size_sb;
    private SwitchButton turn_on_sb,lock_turn_on_sb,up_down_sb,show_shadow_sb;
    private NSDService nsdService;
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            nsdService = ((NSDService.ServiceBinder)iBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            nsdService = null;
        }
    };
    private SystemBarTintManager mTintManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alee);
        initImmerse();
        setImmerseStatusBar(R.drawable.status_bar_bg,false,this);
        findViewById();
        initData();
        setLisenter();
    }

    private void initImmerse() {
        /** 沉浸式状态栏  */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //必须在初始化SystemBarTintManager之前执行此方法
            setTranslucentStatus(true);
        }
        mTintManager = new SystemBarTintManager(this);
    }



    private void findViewById() {
        turn_on_sb = (SwitchButton) findViewById(R.id.turn_on_sb);
        lock_turn_on_sb = (SwitchButton) findViewById(R.id.lock_turn_on_sb);
        up_down_sb = (SwitchButton) findViewById(R.id.up_down_sb);
        show_shadow_sb = (SwitchButton) findViewById(R.id.show_shadow_sb);
        about_bt = (Button) findViewById(R.id.about_bt);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        text_color_bt = (Button) findViewById(R.id.text_color_bt);
        position_sb = (SeekBar) findViewById(R.id.position_sb);
        size_sb = (SeekBar) findViewById(R.id.size_sb);
    }

    private void initData() {
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        /* 初始化按钮状态 */
        turn_on_sb.setChecked(!isServiceWork());
        lock_turn_on_sb.setChecked(!getSharedPreferences("Alee", MODE_PRIVATE).getBoolean("isHideOnLock", false));
        up_down_sb.setChecked(!getSharedPreferences("Alee", MODE_PRIVATE).getBoolean("isShwoUpNet", false));
        show_shadow_sb.setChecked(!getSharedPreferences("Alee", MODE_PRIVATE).getBoolean("isShowShadow", false));
        position_sb.setProgress(getSharedPreferences("Alee", MODE_PRIVATE).getInt("textPosition",235));
        size_sb.setProgress(getSharedPreferences("Alee", MODE_PRIVATE).getInt("textSize",14));
        if (isServiceWork()) {
            Intent mIntent = new Intent(AleeActivity.this, NSDService.class);
            bindService(mIntent,conn,BIND_AUTO_CREATE);
        }
    }

    private void setLisenter() {

        about_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog dialog = new Dialog(AleeActivity.this,"关于","");
                dialog.show();
                TextView tv = dialog.getMessageTextView();
                tv.setText(Html.fromHtml("版本：v"+ Utils.getVersion(AleeActivity.this)+
                        "<br/>开发：<a href='http://weibo.com/p/1005052743635692/home?from=page_100505&mod=TAB&is_all=1#place'>Alee</a><br/>" +
                        "使用的库：<a href='https://github.com/navasmdc/MaterialDesignLibrary'>MaterialDesignLibrary</a><br/>" +
                        "<a href='https://github.com/xiaopansky/SwitchButton'>SwitchButton</a>"
                        ));
                tv.setMovementMethod(LinkMovementMethod.getInstance());
                dialog.getButtonAccept().setText("确定");
            }
        });

        text_color_bt.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                ColorSelector colorSelector = new ColorSelector(AleeActivity.this,
                        getSharedPreferences("Alee", MODE_PRIVATE).getInt("textColor", 0xFFE9E9E9),
                        new ColorSelector.OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int color) {
                        if (isServiceWork()) {
                            nsdService.setTextColor(color);
                            getSharedPreferences("Alee", MODE_PRIVATE).edit().putInt("textColor",color).apply();
                        }
                    }
                });
                colorSelector.show();
            }
        });

        turn_on_sb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b == open) {
                    //开启服务
                    Intent mIntent = new Intent(AleeActivity.this, NSDService.class);
                    startService(mIntent);
                    bindService(mIntent,conn,BIND_AUTO_CREATE);
                } else {
                    //关闭服务
                    Intent mIntent = new Intent(AleeActivity.this, NSDService.class);
                    stopService(mIntent);
                    unbindService(conn);
                }
            }
        });

        lock_turn_on_sb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                getSharedPreferences("Alee", MODE_PRIVATE).edit().putBoolean("isHideOnLock",!b).apply();
            }
        });

        show_shadow_sb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (isServiceWork()) {
                    nsdService.setShowShadow(!b);
                }
                getSharedPreferences("Alee", MODE_PRIVATE).edit().putBoolean("isShowShadow",!b).apply();
            }
        });

        up_down_sb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (isServiceWork()) {
                    nsdService.setShwoUpNet(!b);
                }
                getSharedPreferences("Alee", MODE_PRIVATE).edit().putBoolean("isShwoUpNet",!b).apply();
            }
        });

        position_sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (isServiceWork()) {
                    nsdService.setTextPositon(i);
                    getSharedPreferences("Alee", MODE_PRIVATE).edit().putInt("textPosition",i).apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        size_sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (isServiceWork()) {
                    nsdService.setTextSize(i);
                    getSharedPreferences("Alee", MODE_PRIVATE).edit().putInt("textSize",i).apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    }

    /**
     * 判断网速监测服务是否正在运行
     * @return true代表正在运行，false代表服务没有正在运行
     */
    public boolean isServiceWork() {
        boolean isWork = false;
        ActivityManager mAM = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> myList = mAM.getRunningServices(100);
        if (myList.size() <= 0) {
            return false;
        }
        for (int i = 0; i < myList.size(); i++) {
            String mName = myList.get(i).service.getClassName().toString();
            if (mName.equals("com.alee.nsd.service.NSDService")) {
                isWork = true;
                break;
            }
        }
        return isWork;
    }

    @TargetApi(19)
    protected void setTranslucentStatus(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    /**
     * 设置沉浸式状态栏
     * @note
     * @param resId 状态栏背景资源id
     * @param darkmode 是否设置为darkmode模式
     * @param activity Activity对象
     * @date 创建时间：2015年6月25日 下午3:11:37
     */
    protected void setImmerseStatusBar(int resId, boolean darkmode, Activity activity) {
        //开启沉浸式状态栏
        mTintManager.setStatusBarTintEnabled(true);
        // 设置状态栏背景
        mTintManager.setStatusBarTintResource(resId);
        // 设置状态栏的文字颜色
        mTintManager.setStatusBarDarkMode(darkmode, activity);
    }
}
