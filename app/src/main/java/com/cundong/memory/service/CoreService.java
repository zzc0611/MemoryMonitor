package com.cundong.memory.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.cundong.memory.Constants;
import com.cundong.memory.MainActivity;
import com.cundong.memory.R;
import com.cundong.memory.util.MemoryUtil;
import com.premnirmal.Magnet.IconCallback;
import com.premnirmal.Magnet.Magnet;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 类说明： 	后台轮询service 每1秒钟更新一次通知栏 更新内存
 * 
 * @date 	2015-4-18
 * @version 1.0
 */
public class CoreService extends Service implements IconCallback {

	private static final String TAG = "CoreService";

	private Timer mTimer;
	
	private Magnet mMagnet;

	private View mIconView = null;
	private View mMemoryClearView;

	private TextView mDescView;
	private ImageButton mClearBtn, mSettingBtn;

	private InnerHandler mHandler;

	@Override
	public void onCreate() {
		super.onCreate();

		mIconView = getIconView();
		mDescView = (TextView) mIconView.findViewById(R.id.content);
		mMemoryClearView = mIconView.findViewById(R.id.memory_clear_view);
		mMemoryClearView.setVisibility(Constants.SHOW_MEMORY_CLEAR ? View.VISIBLE : View.GONE);

		mClearBtn = (ImageButton) mIconView.findViewById(R.id.clear_btn);
		mClearBtn.setOnClickListener( new OnClickListener(){

			@Override
			public void onClick(View v) {
				
				Toast.makeText(getApplicationContext(), "clearMemory", Toast.LENGTH_SHORT).show();
				MemoryUtil.clearMemory(getApplicationContext());
			}
		});
		
		mSettingBtn = (ImageButton) mIconView.findViewById(R.id.setting_btn);
		mSettingBtn.setOnClickListener( new OnClickListener(){

			@Override
			public void onClick(View v) {
				Toast.makeText(getApplicationContext(), "test Setting", Toast.LENGTH_SHORT).show();
			}
		});

		mMagnet = new Magnet.Builder(this)
			.setIconView(mIconView)
			.setIconCallback(this)
			.setRemoveIconResId(R.drawable.trash)
			.setRemoveIconShadow(R.drawable.bottom_shadow)
			.setShouldFlingAway(true)
			.setShouldStickToWall(true)
			.setRemoveIconShouldBeResponsive(true)
			.setInitialPosition(-100, -200)
			.build();
		
		mMagnet.show();

		mHandler = new InnerHandler(this);
	}

	private View getIconView() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return inflater.inflate(R.layout.float_view, null);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (mTimer == null) {
			mTimer = new Timer();
			mTimer.scheduleAtFixedRate(new RefreshTask(), 0, 1000);
		}

		int action = intent != null ? intent.getIntExtra("action", 0) : 0;
		if (action == 2) {
			mMagnet.destroy();
			this.stopSelf();
		}
		
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {

		super.onDestroy();

		mTimer.cancel();
		mTimer = null;
	}

	class RefreshTask extends TimerTask {

		@Override
		public void run() {

			String usedPercentValue = MemoryUtil.getUsedPercentValue();
			long availableMemory = MemoryUtil.getAvailableMemory();
			HashMap<String, Long> totalPssMap = MemoryUtil.getTotalPss(Constants.PROCESS_NAME_LIST);

			float memory = availableMemory / (float) 1024 / (float) 1024;
			DecimalFormat decimalFormat = new DecimalFormat("##0.00");
			
			final String[] content = new String[] {
					getString(R.string.used_percent_value, usedPercentValue),
					getString(R.string.available_memory, decimalFormat.format(memory)), getString(R.string.total_pss)};

			StringBuffer sb = new StringBuffer();
			sb.append(content[0]).append(",").append(content[1]).append("\r\n").append(content[2]);

			Iterator iterator = totalPssMap.entrySet().iterator();
			while(iterator.hasNext()) {
				Map.Entry entry = (Map.Entry) iterator.next();
				entry.getKey();
				sb.append(entry.getKey()).append("=").append("\r\n").append(entry.getValue()).append("\r\n");
			}

			Bundle data = new Bundle();
			data.putString("content", sb.toString());
			
			Message message = mHandler.obtainMessage(1);
            message.what = 1;   
            message.setData(data);

			mHandler.sendMessage(message);
		}
	}

	@Override
	public void onFlingAway() {
		Log.i(TAG, "onFlingAway");
	}

	@Override
	public void onMove(float x, float y) {
		Log.i(TAG, "onMove(" + x + "," + y + ")");
	}

	@Override
	public void onIconClick(View icon, float iconXPose, float iconYPose) {
		Log.i(TAG, "onIconClick(..)");

		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	@Override
	public void onIconDestroyed() {
		Log.i(TAG, "onIconDestroyed()");
		stopSelf();
	}

	private static class InnerHandler extends Handler {

		private WeakReference<CoreService> ref;

		public InnerHandler(CoreService service) {
			ref = new WeakReference<>(service);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			CoreService service = ref.get();
			if (service == null) {
				return;
			}

			switch (msg.what) {
				case 1:
					Bundle data = msg.getData();
					String content = data.getString("content");
					service.mDescView.setText(content);

					break;
			}
		}
	}
}