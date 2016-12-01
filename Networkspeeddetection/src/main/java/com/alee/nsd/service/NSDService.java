/*   
 * Copyright (c) 2010-2016 drowtram. All Rights Reserved.   
 *   
 * This software is the confidential and proprietary information of   
 * Founder. You shall not disclose such Confidential Information   
 * and shall use it only in accordance with the terms of the agreements   
 * you entered into with Founder.   
 *   
 */  
package com.alee.nsd.service;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alee.nsd.R;
import com.alee.nsd.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class NSDService extends Service {

	public static final String NET_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
	private LayoutParams wmParams;
	private WindowManager mWindowManager;
	private long preDownRxBytes = 0;
	private long preUpRxBytes = 0;
    private boolean isShwoUpNet = false;
    private List<Long> bytes = new ArrayList<>();
	private TextView net_up_tv;
	private TextView net_down_tv;
	private View view;
	private KeyguardManager keyguardManager;
	private boolean flag;

	private Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
                List<Long> nets = (List<Long>) msg.obj;
                if (nets.size() == 1) { //只显示下载速度
                    float net1 = nets.get(0)/(1024 * 3f);//乘以3是因为获取的是3秒内增加的总流量。
                    if (net1 >= 1000) {
                        net1 /= 1024;
                        if (net1 < 10) {
							net_down_tv.setText(String.format("%.2f", net1) + "M/s");
                        } else if (net1 < 100) {
							net_down_tv.setText(String.format("%.1f", net1) + "M/s");
                        } else {
							net_down_tv.setText(" "+(int)net1 + "M/s"); //加空格解决数据长度不一引起的跳动
                        }

                    } else {
                        if (net1 < 10) {
							net_down_tv.setText(String.format("%.2f", net1) + "K/s");
                        } else if (net1 < 100) {
							net_down_tv.setText(String.format("%.1f", net1) + "K/s");
                        } else {
							net_down_tv.setText(" "+(int)net1 + "K/s");
                        }
                    }
                } else { //显示下载和上传速度
                    float net1 = nets.get(0)/(1024 * 3f);//乘以3是因为获取的是3秒内增加的总流量。
                    float net2 = nets.get(1)/(1024 * 3f);//乘以3是因为获取的是3秒内增加的总流量。
                    //上传速度
                    if (net2 >= 1000) {
                        net2 /= 1024;
                        if (net2 < 10) {
                            net_up_tv.setText(String.format("%.2f", net2) + "M/s↑");
                        } else if (net2 < 100) {
							net_up_tv.setText(String.format("%.1f", net2) + "M/s↑");
                        } else {
							net_up_tv.setText(" "+(int)net2 + "M/s↑"); //加空格解决数据长度不一引起的跳动
                        }

                    } else {
                        if (net2 < 10) {
							net_up_tv.setText(" "+String.format("%.2f", net2) + "K/s↑");
                        } else if (net2 < 100) {
							net_up_tv.setText(" "+String.format("%.1f", net2) + "K/s↑");
                        } else {
							net_up_tv.setText("  "+(int)net2 + "K/s↑");
                        }
                    }
                    //下载速度
                    if (net1 >= 1000) {
                        net1 /= 1024;
                        if (net1 < 10) {
							net_down_tv.setText(String.format("%.2f", net1) + "M/s↓");
                        } else if (net1 < 100) {
							net_down_tv.setText(String.format("%.1f", net1) + "M/s↓");
                        } else {
                            net_down_tv.setText("  "+(int)net1 + "M/s↓"); //加空格解决数据长度不一引起的跳动
                        }

                    } else {
                        if (net1 < 10) {
                            net_down_tv.setText(" "+String.format("%.2f", net1) + "K/s↓");
                        } else if (net1 < 100) {
                            net_down_tv.setText(" "+String.format("%.1f", net1) + "K/s↓");
                        } else {
                            net_down_tv.setText("  "+(int)net1 + "K/s↓");
                        }
                    }
                }
				break;
			}
		};
	};
	
	private Runnable mRunnable = new Runnable() {
		
		@Override
		public void run() {
			//每3秒循环获取一次数据，减少系统资源消耗
			refresh();
			mHandler.postDelayed(mRunnable, 3000);
		}
	};

	/**
	 * 屏幕广播
	 */
	private BroadcastReceiver mLockReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
				//开屏 (此时的开屏有可能正在解锁，或已经解锁)
				flag = keyguardManager.inKeyguardRestrictedInputMode();
				if(!flag) { //此时为已解锁状态
					if (getSharedPreferences("Alee", MODE_PRIVATE).getBoolean("isHideOnLock", false) && getNetworkState(context)) {
						//显示
						if (view.getVisibility() != View.VISIBLE) {
							view.setVisibility(View.VISIBLE);
						}
					}
				}
//				Log.d("zhouchuan", flag ? "正在解锁":"已解锁");
			} else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
				//锁屏状态
				if (getSharedPreferences("Alee", MODE_PRIVATE).getBoolean("isHideOnLock", false)) {
					//隐藏
					if (view.getVisibility() != View.GONE) {
						view.setVisibility(View.GONE);
					}
				}
//				Log.d("zhouchuan","锁屏");
			} else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
				//解锁状态
				if (getSharedPreferences("Alee", MODE_PRIVATE).getBoolean("isHideOnLock", false) && getNetworkState(context)) {
					//显示
					if (view.getVisibility() != View.VISIBLE) {
						view.setVisibility(View.VISIBLE);
					}
				}
//				Log.d("zhouchuan","解锁");
			}
		}
	};
	
	/**
	 * 网络状态广播
	 */
	private BroadcastReceiver mNetReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (NET_ACTION.equals(intent.getAction())) {
				if (getNetworkState(context)) {
					if (view.getVisibility() != View.VISIBLE) {
						view.setVisibility(View.VISIBLE);
					}
				} else {
					if (view.getVisibility() != View.GONE) {
						view.setVisibility(View.GONE);
					}
				}
			}
		}
		
	};

	@Override
	public IBinder onBind(Intent intent) {
		return new ServiceBinder();
	}

	@Override
	public void onCreate() {
		super.onCreate();
        registerLockBroadcastReceiver();
        registerNetBroadcastReceiver();
		createFloatView();
		init();
	}

	/**
	 * 初始化用户数据
	 */
	private void init() {
		keyguardManager = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
		isShwoUpNet = getSharedPreferences("Alee", MODE_PRIVATE).getBoolean("isShwoUpNet", false);
		if (isShwoUpNet) {
			net_up_tv.setVisibility(View.VISIBLE);
			net_down_tv.setIncludeFontPadding(false);
		}
		setShowShadow(getSharedPreferences("Alee", MODE_PRIVATE).getBoolean("isShowShadow", false));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mHandler.post(mRunnable);
		return super.onStartCommand(intent, flags, startId);
	}
	
	/**
	 * 获取网络状态
	 * @param context  
	 * @author drowtram
	 */
	private boolean getNetworkState(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm != null) {
			NetworkInfo info = cm.getActiveNetworkInfo();
			if (info != null && info.isConnectedOrConnecting()) {
				//有网
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 创建悬浮窗
	 *   
	 * @author drowtram
	 */
	private void createFloatView() {
		wmParams = new LayoutParams();
		mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
		//设置window type
		wmParams.type = LayoutParams.TYPE_SYSTEM_ERROR/* | LayoutParams.TYPE_PHONE*/;
		//设置图片格式，透明背景
		wmParams.format = PixelFormat.TRANSPARENT;
		//设置浮动窗不可聚焦
		wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE|LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		//调整悬浮窗显示的停靠位置为右侧置顶
		wmParams.gravity = Gravity.TOP | Gravity.RIGHT;
		//设置x，y初始值，相对于gravity 以屏幕右上角为原点
		wmParams.x = getSharedPreferences("Alee", MODE_PRIVATE).getInt("textPosition", 235);
		wmParams.y = 0;
//		wmParams.y = 11;
		//设置悬浮窗长宽
		wmParams.width = LayoutParams.WRAP_CONTENT;
		wmParams.height = Utils.getStatusBarHight(this);
		//添加布局
		view = View.inflate(this,R.layout.net_layout,null);
		view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,Utils.getStatusBarHight(this)));
		net_up_tv = (TextView) view.findViewById(R.id.net_up_tv);
		net_down_tv = (TextView) view.findViewById(R.id.net_down_tv);
		setTextSize(getSharedPreferences("Alee", MODE_PRIVATE).getInt("textSize",14));
		mWindowManager.addView(view, wmParams);
	}

	private void registerLockBroadcastReceiver() {
		IntentFilter mFilter = new IntentFilter();
		mFilter.addAction(Intent.ACTION_SCREEN_OFF);
		mFilter.addAction(Intent.ACTION_SCREEN_ON);
		mFilter.addAction(Intent.ACTION_USER_PRESENT);
		registerReceiver(mLockReceiver, mFilter);
	}
	
	private void registerNetBroadcastReceiver() {
		IntentFilter mFilter = new IntentFilter(NET_ACTION);
		registerReceiver(mNetReceiver, mFilter);
	}
	/**
	 * 刷新一次数据信息
	 *   
	 * @author drowtram
	 */
	private void refresh() {
        //清空之前数据
        bytes.clear();
		//获取总的接收字节数(下载流量)，包含Mobile和WiFi等
		long curDownRxBytes = TrafficStats.getTotalRxBytes();
		if (preDownRxBytes == 0) {
            preDownRxBytes = curDownRxBytes;
		}
		long downBytes = curDownRxBytes - preDownRxBytes;
		preDownRxBytes = curDownRxBytes;
        bytes.add(downBytes);
        //计算上传流量
        if (isShwoUpNet) {
            //获取总的发送字节数(上传流量)，包含Mobile和WiFi等
            long curUpRxBytes = TrafficStats.getTotalTxBytes();
            if(preUpRxBytes == 0) {
                preUpRxBytes = curUpRxBytes;
            }
            long upBytes = curUpRxBytes - preUpRxBytes;
            preUpRxBytes = curUpRxBytes;
            bytes.add(upBytes);
        }
        mHandler.obtainMessage(1, bytes).sendToTarget();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (view != null) {
			mWindowManager.removeView(view);
		}
		mHandler.removeCallbacks(mRunnable);
		unregisterReceiver(mLockReceiver);
		unregisterReceiver(mNetReceiver);
    }

	/**
	 * 调用者获取到服务的实例
	 */
	public class ServiceBinder extends Binder{

		public NSDService getService() {
			return NSDService.this;
		}

	}

	/**
	 * 外部设置显示颜色
	 * @param color 颜色色值
     */
	public void setTextColor(int color) {
		net_up_tv.setTextColor(color);
		net_down_tv.setTextColor(color);
	}

	/**
	 * 外部设置显示位置
	 * @param positon
     */
	public void setTextPositon(int positon) {
		wmParams.x = positon;
		mWindowManager.updateViewLayout(view, wmParams);
	}

	/**
	 * 外部设置字体大小
	 * @param size
     */
	public void setTextSize(int size) {
		net_up_tv.setTextSize(size);
		net_down_tv.setTextSize(size);
	}

    /**
     * 外部设置显示上传流量
     * @param isShwoUpNet
     */
    public void setShwoUpNet(boolean isShwoUpNet) {
        this.isShwoUpNet = isShwoUpNet;
		if (isShwoUpNet) {
			net_up_tv.setVisibility(View.VISIBLE);
			net_down_tv.setIncludeFontPadding(false);
		} else {
			net_up_tv.setVisibility(View.GONE);
			net_down_tv.setIncludeFontPadding(true);
		}
    }

	/**
	 * 外部设置显示阴影
	 * @param isShowShadow
     */
	public void setShowShadow(boolean isShowShadow) {
		if (isShowShadow) {
			net_up_tv.setShadowLayer(1,1,1,0x7f000000);
			net_down_tv.setShadowLayer(1,1,1,0x7f000000);
		} else {
			net_up_tv.setShadowLayer(0,0,0,0x00000000);
			net_down_tv.setShadowLayer(0,0,0,0x00000000);
		}
	}
}
