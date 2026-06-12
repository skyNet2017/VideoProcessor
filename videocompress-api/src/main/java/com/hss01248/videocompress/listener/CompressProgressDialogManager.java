package com.hss01248.videocompress.listener;

import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.LogUtils;
import com.hss01248.videocompress.R;
import com.hss01248.videocompress.compare.CompressCompareActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single dialog listing all compress jobs (newest first). Item click opens compare when finished.
 */
public class CompressProgressDialogManager {

    private static final CompressProgressDialogManager INSTANCE = new CompressProgressDialogManager();
    /** Retain finished tasks in the list briefly, then drop to avoid unbounded growth. */
    private static final long TERMINAL_TASK_RETAIN_MS = 3 * 60 * 1000L;
    private static final int MAX_REFRESH_RETRY = 10;
    private static final long REFRESH_RETRY_DELAY_MS = 120L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int refreshRetryCount;
    private final Map<String, CompressProgressItem> items = new LinkedHashMap<>();
    private long joinOrderCounter;

    private Dialog dialog;
    private CompressProgressListAdapter adapter;
    private Activity boundActivity;

    public static CompressProgressDialogManager getInstance() {
        return INSTANCE;
    }

    /** Called when a task enters the serial queue. */
    public synchronized String registerQueued(String inputPath) {
        return registerQueued(inputPath, ActivityUtils.getTopActivity());
    }

    /** Called when a task enters the serial queue; {@code hostActivity} helps bind the dialog host. */
    public synchronized String registerQueued(String inputPath, Activity hostActivity) {
        bindHostActivity(hostActivity);
        joinOrderCounter++;
        long enqueueTime = System.currentTimeMillis();
        String taskKey = inputPath + "#" + joinOrderCounter;
        CompressProgressItem item = new CompressProgressItem(taskKey, inputPath, joinOrderCounter, enqueueTime);
        item.status = CompressProgressItem.Status.WAITING;
        items.put(taskKey, item);
        postRefresh();
        return taskKey;
    }

    /** Prefer a known host Activity (e.g. from {@code init} or compress listener). */
    public void bindHostActivity(Activity hostActivity) {
        if (isActivityAlive(hostActivity)) {
            boundActivity = hostActivity;
        }
    }

    public void onStart(String taskKey, String inputPath, String outPath) {
        mainHandler.post(() -> {
            CompressProgressItem item = ensureItem(taskKey, inputPath);
            item.status = CompressProgressItem.Status.RUNNING;
            item.outputPath = outPath;
            item.progress = 0;
            item.startTime = System.currentTimeMillis();
            refreshUi();
        });
    }

    public void onProgress(String taskKey, int progress) {
        mainHandler.post(() -> {
            CompressProgressItem item = items.get(taskKey);
            if (item == null) {
                return;
            }
            item.status = CompressProgressItem.Status.RUNNING;
            item.progress = progress;
            refreshUi();
        });
    }

    public void onFinish(String taskKey, String outputFilePath) {
        mainHandler.post(() -> {
            CompressProgressItem item = items.get(taskKey);
            if (item == null) {
                return;
            }
            item.status = CompressProgressItem.Status.SUCCESS;
            item.outputPath = outputFilePath;
            item.progress = 100;
            refreshUi();
            scheduleAutoRemove(taskKey);
        });
    }

    public void onError(String taskKey, String message) {
        mainHandler.post(() -> {
            CompressProgressItem item = items.get(taskKey);
            if (item == null) {
                return;
            }
            item.status = CompressProgressItem.Status.ERROR;
            item.errorMessage = message;
            refreshUi();
            scheduleAutoRemove(taskKey);
        });
    }

    public void onCancel(String taskKey) {
        mainHandler.post(() -> {
            CompressProgressItem item = items.get(taskKey);
            if (item == null) {
                return;
            }
            item.status = CompressProgressItem.Status.CANCELLED;
            refreshUi();
            scheduleAutoRemove(taskKey);
        });
    }

    /**
     * Show the task list dialog with current queue state (waiting, running, and recent finished jobs).
     * Safe to call when the dialog was dismissed or never auto-shown.
     */
    public void showTaskListDialog() {
        showTaskListDialog(null);
    }

    public void showTaskListDialog(Activity hostActivity) {
        mainHandler.post(() -> {
            bindHostActivity(hostActivity != null ? hostActivity : ActivityUtils.getTopActivity());
            refreshRetryCount = 0;
            refreshUi();
        });
    }

    private void scheduleAutoRemove(final String taskKey) {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                synchronized (CompressProgressDialogManager.this) {
                    items.remove(taskKey);
                }
                refreshUi();
                if (items.isEmpty()) {
                    dismissDialogQuietly();
                }
            }
        }, TERMINAL_TASK_RETAIN_MS);
    }

    private CompressProgressItem ensureItem(String taskKey, String inputPath) {
        CompressProgressItem item = items.get(taskKey);
        if (item == null) {
            joinOrderCounter++;
            item = new CompressProgressItem(taskKey, inputPath, joinOrderCounter, System.currentTimeMillis());
            items.put(taskKey, item);
        }
        return item;
    }

    private void postRefresh() {
        refreshRetryCount = 0;
        mainHandler.post(this::refreshUi);
    }

    private void scheduleRefreshRetry() {
        if (refreshRetryCount >= MAX_REFRESH_RETRY) {
            return;
        }
        refreshRetryCount++;
        mainHandler.postDelayed(this::refreshUi, REFRESH_RETRY_DELAY_MS);
    }

    private void refreshUi() {
        try {
            Activity activity = resolveActivity();
            if (activity == null) {
                scheduleRefreshRetry();
                return;
            }
            ensureDialog(activity);
            if (adapter != null) {
                adapter.setItems(new ArrayList<>(items.values()));
            }
            if (showDialogSafely(activity)) {
                refreshRetryCount = 0;
            }
        } catch (Throwable t) {
            LogUtils.w(t);
            scheduleRefreshRetry();
        }
    }

    private boolean showDialogSafely(Activity activity) {
        if (dialog == null) {
            scheduleRefreshRetry();
            return false;
        }
        if (dialog.isShowing()) {
            return true;
        }
        View decor = activity.getWindow().getDecorView();
        if (!decor.isAttachedToWindow()) {
            scheduleRefreshRetry();
            return false;
        }
        try {
            dialog.show();
            return true;
        } catch (WindowManager.BadTokenException e) {
            LogUtils.w("compress task dialog show failed, will retry: " + e.getMessage());
            dismissDialogQuietly();
            scheduleRefreshRetry();
            return false;
        }
    }

    private Activity resolveActivity() {
        if (isActivityAlive(boundActivity)) {
            return boundActivity;
        }
        Activity top = ActivityUtils.getTopActivity();
        if (isActivityAlive(top)) {
            boundActivity = top;
            return top;
        }
        return null;
    }

    private static boolean isActivityAlive(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        return Build.VERSION.SDK_INT < 17 || !activity.isDestroyed();
    }

    private void ensureDialog(Activity activity) {
        if (dialog != null && boundActivity == activity) {
            return;
        }
        dismissDialogQuietly();

        boundActivity = activity;
        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_compress_progress_list, null);
        ListView listView = content.findViewById(R.id.lv_compress_tasks);
        adapter = new CompressProgressListAdapter(activity);
        adapter.setItems(new ArrayList<>(items.values()));
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CompressProgressItem item = adapter.getItemAt(position);
                if (item == null || !item.canOpenCompare()) {
                    Toast.makeText(activity, R.string.vc_compress_tap_when_done, Toast.LENGTH_SHORT).show();
                    return;
                }
                Activity host = resolveActivity();
                if (host == null) {
                    return;
                }
                CompressCompareActivity.start(host, item.inputPath, item.outputPath, item.startTime);
            }
        });

        dialog = new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.vc_compress_list_title))
                .setView(content)
                .setCancelable(true)
                .create();
    }

    private void dismissDialogQuietly() {
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Throwable ignored) {
        }
        dialog = null;
        adapter = null;
    }
}
