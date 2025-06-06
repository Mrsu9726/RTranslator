package nie.translator.rtranslator.tools;

import android.os.Handler;
import android.os.Looper;
 
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
 
public class ThreadUtils {
    private static final String TAG = "ThreadUtils";
    private static ThreadUtils instance;
 
    private final ExecutorService fixedThreadPool;
    private final Executor mainThread;
 
    private ThreadUtils() {
        fixedThreadPool = Executors.newFixedThreadPool(8);
        mainThread = new MainThreadExecutor();
    }
 
    public static synchronized ThreadUtils getInstance() {
        if (instance == null) {
            instance = new ThreadUtils();
        }
        return instance;
    }
 
    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
 
        @Override
        public void execute(Runnable command) {
            mainThreadHandler.post(command);
        }
    }
 
    public void execute(Runnable runnable) {
        fixedThreadPool.execute(runnable);  // 不建议再阻塞式执行
    }
 
    public void executeMain(Runnable runnable) {
        mainThread.execute(runnable);
    }
 
    public static boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }
 
    /**
     * 异步执行任务 + 超时控制 + 回调
     */
    public <T> void executeWithTimeoutAsync(
            Callable<T> task,
            long timeout,
            TimeUnit unit,
            Consumer<T> onSuccess,
            Consumer<Throwable> onError
    ) {
        Future<T> future = fixedThreadPool.submit(task);
 
        new Thread(() -> {
            try {
                T result = future.get(timeout, unit);
                mainThread.execute(() -> onSuccess.accept(result));
            } catch (TimeoutException e) {
                future.cancel(true);
                mainThread.execute(() -> onError.accept(e));
            } catch (ExecutionException | InterruptedException e) {
                mainThread.execute(() -> onError.accept(e));
            }
        }).start();
    }
}