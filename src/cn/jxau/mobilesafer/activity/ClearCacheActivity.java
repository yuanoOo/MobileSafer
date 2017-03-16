package cn.jxau.mobilesafer.activity;

import java.lang.reflect.Method;
import java.util.List;

import cn.jxau.mobilesafer.R;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.IPackageStatsObserver.Stub;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageStats;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ClearCacheActivity extends Activity{
	protected static final int UPDATE_PROGRESS = 100;
	protected static final int FINISH = 101;
	protected static final int UPDATE_CACHE = 102;
	protected static final String tag = "ClearCacheActivity";
	protected static final int REMOVE_ALLVIEW = 103;
	
	private Button bt_clear_cache;
	private ProgressBar pb_bar;
	private TextView tv_name;
	private LinearLayout ll_parent;
	private Stub mStatsObserver;
	private PackageManager mPM;
	private int index = 0;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_PROGRESS:
				tv_name.setText((String)msg.obj);
				break;
			case FINISH:
				tv_name.setText("扫描完成");
				break;
			case UPDATE_CACHE:
				Log.i(tag, "aaaaaaaaaaaaaaaaaaaaa");
				final CacheInfo cacheInfo = (CacheInfo) msg.obj;
				//一旦检查到带有缓存的应用,就需要将其对应条目添加到线性布局中
				View view = View.inflate(getApplicationContext(),R.layout.cache_item_view, null);
				
				ImageView iv_icon = (ImageView) view.findViewById(R.id.iv_icon);
				TextView tv_name = (TextView) view.findViewById(R.id.tv_name);
				TextView tv_cache_size = (TextView)view.findViewById(R.id.tv_cache_size);
				ImageView iv_delete = (ImageView)view.findViewById(R.id.iv_delete);
				
				iv_icon.setBackgroundDrawable(cacheInfo.drawable);
				tv_name.setText(cacheInfo.name);
				tv_cache_size.setText("缓存大小:"+Formatter.formatFileSize(getApplicationContext(),cacheInfo.cacheSize));
				iv_delete.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						/*try {
							Class<?> clazz = Class.forName("android.content.pm.PackageManager");
							Method method = clazz.getMethod("deleteApplicationCacheFiles",
									String.class,IPackageDataObserver.class);
							method.invoke(mPM,cacheInfo.packageName,new IPackageDataObserver.Stub() {
								@Override
								public void onRemoveCompleted(String packageName, boolean succeeded)
										throws RemoteException {
									
								}
							});
						} catch (Exception e) {
							e.printStackTrace();
						}*/
						Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
						intent.setData(Uri.parse("package:"+cacheInfo.packageName));
						startActivity(intent);
					}
				});
				
				ll_parent.addView(view,0);
				break;
			case REMOVE_ALLVIEW:
				ll_parent.removeAllViews();
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_clear_cache);
		
		initUI();
		initData();
	}

	private void initData() {
		new Thread(){
			public void run() {
				//1.获取所有安装在手机上的应用
				mPM = getPackageManager();
				//2.获取所有安装在手机上的应用集合
				List<PackageInfo> packageInfos = mPM.getInstalledPackages(0);
				pb_bar.setMax(packageInfos.size());
				for (PackageInfo packageInfo : packageInfos) {
					String packageName = packageInfo.packageName;
					String name = packageInfo.applicationInfo.loadLabel(mPM).toString();
					
					try {
						Class<?> clazz = Class.forName("android.content.pm.PackageManager");
						Method method = clazz.getMethod("getPackageSizeInfo", String.class,IPackageStatsObserver.class);
						method.invoke(mPM,packageName,mStatsObserver);
					} catch (Exception e) {
						e.printStackTrace();
					}
						
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					index++;
					pb_bar.setProgress(index);
					
					Message msg = Message.obtain();
					msg.what = UPDATE_PROGRESS;
					msg.obj = name;
					mHandler.sendMessage(msg);
				}
				Message msg = Message.obtain();
				msg.what = FINISH;
				mHandler.sendMessage(msg);
			};
		}.start();
		
		mStatsObserver = new IPackageStatsObserver.Stub(){
			@Override
			public void onGetStatsCompleted(PackageStats pStats,
					boolean succeeded) throws RemoteException {
				if(pStats.cacheSize>0){
					CacheInfo cacheInfo = new CacheInfo();
					cacheInfo.cacheSize = pStats.cacheSize;
//					String strCacheSize = Formatter.formatFileSize(getApplicationContext(), cacheSize);
					cacheInfo.packageName = pStats.packageName;
					try {
						ApplicationInfo applicationInfo = mPM.getApplicationInfo(pStats.packageName, 0);
						cacheInfo.name = applicationInfo.loadLabel(mPM).toString();
						cacheInfo.drawable = applicationInfo.loadIcon(mPM);
					} catch (NameNotFoundException e) {
						e.printStackTrace();
					}
					Message msg = Message.obtain();
					msg.what = UPDATE_CACHE;
					msg.obj = cacheInfo;
					mHandler.sendMessage(msg);
				}
				
			}
		};
	}
	
	public class CacheInfo{
		public String name;
		public String packageName;
		public long cacheSize;
		public Drawable drawable;
	}
	
	private void initUI() {
		bt_clear_cache = (Button) findViewById(R.id.bt_clear_cache);
		pb_bar = (ProgressBar) findViewById(R.id.pb_bar);
		tv_name = (TextView) findViewById(R.id.tv_name);
		ll_parent = (LinearLayout) findViewById(R.id.ll_parent);
		
		bt_clear_cache.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				try {
					Class<?> clazz = Class.forName("android.content.pm.PackageManager");
					Method method = clazz.getMethod("freeStorageAndNotify", long.class,IPackageDataObserver.class);
					method.invoke(mPM,Long.MAX_VALUE,new IPackageDataObserver.Stub() {
						@Override
						public void onRemoveCompleted(String packageName, boolean succeeded)
								throws RemoteException {
							Message msg = Message.obtain();
							msg.what = REMOVE_ALLVIEW;
							mHandler.sendMessage(msg);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
