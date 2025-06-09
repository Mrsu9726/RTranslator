package nie.translator.rtranslator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == Intent.ACTION_BOOT_COMPLETED ||
                intent.getAction() == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            // 启动前台 Activity 示例（仅当 App 被系统允许启动时）
            Intent startIntent = new Intent(context, LoadingActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startIntent);
            // 如果你要启动 Service，建议使用 JobIntentService 或 WorkManager
        }
    }
}
