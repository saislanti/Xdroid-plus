package isc.whu.defender.trafmon;

import isc.whu.defender.R;
import isc.whu.defender.common.CommonTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * 流量监控器设置界面
 * 设置获取流量信息的时间间隔
 * @author Shao
 * @since 2012-02-02
 */
// TODO 点击每项查看具体信息
public class MonitorActivity extends Activity {
	
	private static final String TIME_INTERVAL_KEY = "time_interval";

	private Spinner mSpinner;
	private Button mBtnApply;
	private ListView mListView;
	private long timeInterval;
	private ArrayList<HashMap<String, Object>> dataList;
	private SharedPreferences prefs;
	
	// TODO 增加加载对话框
	ProgressDialog progressDlg;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.net_monitor);
		
		// 初始化各个控件
		mSpinner = (Spinner) findViewById(R.id.spinner_timne_interval);
		mBtnApply = (Button) findViewById(R.id.btn_apply);
		mListView = (ListView) findViewById(R.id.listview);
		
		// 获取首选项对象
		prefs = CommonTools.getPreferences(MonitorActivity.this);
		
		// 构造时间间隔选择spinner
		ArrayAdapter adapter = ArrayAdapter.createFromResource(this, 
				R.array.time_interval, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinner.setAdapter(adapter);
		
		// 使用listview显示程序列表的流量情况
		dataList = new ArrayList<HashMap<String, Object>>();
		getData();		
		SimpleAdapter adapter2 = new SimpleAdapter(this, 
				dataList,
				R.layout.list_cell,
				new String[] {"app_name", "bytes_total", "bytes_sent", "bytes_received"},
				new int[] {R.id.uid, R.id.bytes_total, R.id.bytes_sent, R.id.bytes_received}
		);
		mListView.setAdapter(adapter2);
		
		// 设置动作响应
		mSpinner.setOnItemSelectedListener(new SpinnerItemSelectedListener());
		mBtnApply.setOnClickListener(new ButtonClickListener());
		mListView.setOnItemClickListener(itemClickListener);
		
		// 设置mSpinner的值
		int pos = prefs.getInt(TIME_INTERVAL_KEY, -1);
		System.out.println("之前保存的设置值: " + pos);
		if (pos != -1)
			mSpinner.setSelection(pos);
		
	}
	
	private OnItemClickListener itemClickListener = new OnItemClickListener() {

		public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
			// TODO Auto-generated method stub
			String str = String.format("pos: %d, id: %d", pos, id);
			System.out.println(str);
		}
		
	};
	
	/**
	 * 获取将要在listview中显示的数据
	 */
	private void getData() {
		PackageManager packManager = this.getPackageManager();
		List<PackageInfo> packInfo = packManager.getInstalledPackages(0);
		Iterator<PackageInfo> it = packInfo.iterator();
		
		// 首先增加一行标题栏
		HashMap<String, Object> map1 = new HashMap<String, Object>();
		map1.put("app_name", "程序名");
		map1.put("bytes_sent", "发送");
		map1.put("bytes_received", "接收");
		map1.put("bytes_total", "总共");
		dataList.add(map1);
		
		PackageInfo p = null;
		while (it.hasNext()) {
			p = it.next();
			if ((p.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) 
				continue;
			
			HashMap<String, Object> map = new HashMap<String, Object>();
			int uid = p.applicationInfo.uid;
			long dataSent = TrafficStats.getUidTxBytes(uid);
			long dataReceived = TrafficStats.getUidRxBytes(uid);
			
			map.put("app_name",
					p.applicationInfo.loadLabel(packManager).toString());
			/* 获取程序的图标有问题
			map.put("app_icon", 
					p.applicationInfo.loadIcon(packManager));
			*/
			map.put("bytes_sent", dataSent);
			map.put("bytes_received", dataReceived);
			map.put("bytes_total", dataSent + dataReceived);
			
			dataList.add(map);
		}
	}
	
	
	private class SpinnerItemSelectedListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
			timeInterval = 1000L;
			
			switch (pos) {
			case 0: // 4 hours 
//				timeInterval *= 60 * 60 * 4;
				timeInterval *= 120; // Make time interval not too long for test
				break;
			case 1: // 6 hours
				timeInterval *= 60 * 60 * 6;
				break;
			case 2: // 12 hours
				timeInterval *= 60 * 60 * 12;
				break;
			default:
			}
//			Logger.write("select time interval(ms): " + timeInterval);
		}
		
		public void onNothingSelected(AdapterView<?> parent) {
			System.out.println("Nothing selected");
		}
	}

	private class ButtonClickListener implements OnClickListener {
		public void onClick(View v) {
//			Logger.write("TrafficMonitor, Apply button clicked.");
			
			// 检查当前保存的设置是否与之前相同，若相同则直接返回
			int savedPos = prefs.getInt(TIME_INTERVAL_KEY, -1);
			int selectedPos = mSpinner.getSelectedItemPosition();
			if (savedPos == selectedPos)
				return;
			
			// 创建广播发送器
			Intent intent = new Intent(MonitorActivity.this, AlarmReceiver.class);
			intent.setAction(AlarmReceiver.ALARM_ACTION);
			PendingIntent sender = PendingIntent.getBroadcast(
					MonitorActivity.this, 0, intent, 0);
			AlarmManager am = (AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
			// 取消之前的设置
			am.cancel(sender);
			
			long triggerTime = System.currentTimeMillis() + 1000;
			am.setRepeating(AlarmManager.RTC, triggerTime, timeInterval, sender);
			
			// 保存设置

			System.out.println("Selected item pos: " + selectedPos);
			prefs.edit().putInt(TIME_INTERVAL_KEY, selectedPos).commit();
			
			showToast("应用成功");
		}
	}
	
	/**
	 * 显示toast提示
	 * @param str toast显示的内容
	 */
	private void showToast(String str) {
		Toast.makeText(MonitorActivity.this, str, Toast.LENGTH_SHORT).show();
	}
	
}