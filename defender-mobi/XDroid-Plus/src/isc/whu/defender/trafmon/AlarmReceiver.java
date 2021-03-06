package isc.whu.defender.trafmon;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.net.TrafficStats;

/**
 * 接受定时发出的广播，接收到广播后
 * 1. 获取每个应用的流量数据等信息
 * 2. 保存流量数据到文件中
 * 3. 由于每次重启后流量清零，需要建立数据库存储每个应用的总流量
 * @author Shao
 *
 */
public class AlarmReceiver extends BroadcastReceiver {
	public static final String ALARM_ACTION = "android.action.custom.ALARM_ACTION";
	
	private Context context; 		// context of this receiver
	private ArrayList<AppInfo> appinfo = new ArrayList<AppInfo>();

	@Override
	/**
	 * 接收到定时发送的广播
	 */
	public void onReceive(Context context, Intent intent) {
		this.context = context;
		
//		Logger.write("alarm action received.");
		
		if (intent.getAction().equals(ALARM_ACTION)) {
			getInstalledAppInfo(false);
			saveAppInfo();
		}
		
	}
	
	/**
	 * 获得已安装应用程序的基本信息
	 * 
	 * @param getSysApp
	 *            true 包括系统应用，即/system/app中的应用程序 。false 不包括系统应用
	 * @return 包含多个应用程序的信息的表
	 */
	private ArrayList<AppInfo> getInstalledAppInfo(boolean getSysApp) {
		List<PackageInfo> packs = context.getPackageManager().getInstalledPackages(0);

		Iterator<PackageInfo> it = packs.iterator();
		PackageInfo p;
		while (it.hasNext()) {
			AppInfo info = new AppInfo();
			p = it.next();
			// if (p.applicationInfo.uid <= 10000)
			if ((!getSysApp)
					&& (p.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0)
				continue;

			info.appName = p.applicationInfo.loadLabel(context.getPackageManager())
					.toString();
			info.packageName = p.packageName;
			info.uid = p.applicationInfo.uid;
			info.versionCode = p.versionCode;
			info.appIcon = p.applicationInfo.loadIcon(context.getPackageManager());

			// 获得流量数据，当无流量时返回TrafficStats.UNSUPPORTED，也就是-1
			// 直接加1变为0简化计算
			// 同时，不为-1时，误差可以接受
			info.dataSent = TrafficStats.getUidTxBytes(info.uid) + 1;
			info.dataReceived = TrafficStats.getUidRxBytes(info.uid) + 1;

			appinfo.add(info);
		}

		return appinfo;
	}
	
	private void saveAppInfo() {
		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd,HH:mm");
		String fileName = sdf.format(c.getTimeInMillis());
		
		try {
			FileOutputStream os = context.openFileOutput(fileName, 0);
			OutputStreamWriter out = new OutputStreamWriter(os);
			
			Iterator<AppInfo> it = appinfo.iterator();
			AppInfo tmp;
			String line = String.format("%6s%15s%15s\n", "uid", "received", "sent");
			String data = line;
			while (it.hasNext()) {
				tmp = it.next();
				line = String.format("%6d%15d%15d\n", tmp.uid, tmp.dataReceived, tmp.dataSent);
				data += line;
			}

			out.write(data);
			out.close();
	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 输出应用的信息，测试时使用
	 */
	private void outputAppInfo() {
		Iterator<AppInfo> it = appinfo.iterator();
		AppInfo tmp;
		while (it.hasNext()) {
			tmp = it.next();
			System.out.println(tmp.uid + ": " + tmp.appName + "Sent: "
					+ tmp.dataSent + " Received: " + tmp.dataReceived);
		}
	}

}

/**
 * 存储应用的基本信息
 * 
 * @author Shao
 * 
 */
class AppInfo {
	public String appName;
	public String packageName;
	public int uid;
	public int versionCode;
	public Drawable appIcon;
	public long dataSent;
	public long dataReceived;
}
