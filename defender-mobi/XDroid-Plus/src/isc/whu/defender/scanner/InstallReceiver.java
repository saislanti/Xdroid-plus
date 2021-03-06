package isc.whu.defender.scanner;

import isc.whu.defender.MainActivity;
import isc.whu.defender.R;
import isc.whu.defender.common.CommonTools;
import isc.whu.defender.common.Logger;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class InstallReceiver extends BroadcastReceiver {

	private Context context;

	@Override
	public void onReceive(Context context, Intent intent) {
		this.context = context;

		SharedPreferences prefs = CommonTools.getPreferences(context);

		if (!prefs.getBoolean("enable_install_monitor", true)) // 若安装程序检测功能未开启
			return; // 函数结束

		if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
			String packageName = intent.getDataString();
			packageName = packageName.substring(packageName.indexOf(':') + 1);
			System.out.println(packageName + " has been installed");
			boolean safe = scanPermission(packageName);
			showInstallNotification(packageName, safe);
		}
	}

	/**
	 * 根据app的包名扫描权限
	 * 
	 * @param packageName
	 *            应用程序的包名
	 * @return true 可认为此程序安全 false 有安全风险 TODO 算法上需要修改，目前太简陋了，糊弄鬼呢！！
	 */
	private boolean scanPermission(String packageName) {
		PackageManager packMgr = context.getPackageManager();
		try {
			PackageInfo pInfo = packMgr.getPackageInfo(packageName,
					PackageManager.GET_PERMISSIONS);
			String permissions[] = pInfo.requestedPermissions;

			if (permissions == null) // 没有声明任何权限，视为安全
				return true;

			int value = 0;
			for (String permission : permissions) {
				System.out.println(permission);
				Integer integer = (Integer) CommonTools.SensitivePermissionsMap
						.get(permission);
				value += (integer == null ? 0 : integer);
			}

			System.out.println("value: " + value);
			Logger.write(packageName + " value: " + value);

			if (value < CommonTools.THRESHOLD) {
				return true;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return false;
	}

	/**
	 * 显示通知栏消息
	 */
	private void showInstallNotification(String packageName, boolean isSafe) {
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		// 定义通知栏展现的内容信息
		int icon = isSafe ? R.drawable.icon : R.drawable.unsafe_warning; // 有无风险图标不同
		String format = context
				.getString(isSafe ? R.string.text_app_safe_format
						: R.string.text_app_unsafe_format);
		CharSequence tickerText = String.format(format, packageName);
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);

		// 定义下拉通知栏时要展现的内容信息
		CharSequence title = tickerText;
		CharSequence text = "点击查看详细内容";
		Intent notificationIntent = new Intent(context, MainActivity.class);
		// 点击通知栏图标会进入某个界面
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(context, title, text, contentIntent);

		// 生成标题栏消息通知
		mNotificationManager.notify(1, notification);
	}

}
