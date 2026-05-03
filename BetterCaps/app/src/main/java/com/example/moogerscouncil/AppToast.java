package com.example.moogerscouncil;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Branded in-app notification that overlays the current Activity window.
 * Uses DecorView injection instead of Toast.setView() (deprecated API 30+).
 *
 * <p>Cross-screen messages: call {@link #scheduleForNextActivity} before navigating away,
 * then call {@link #consumePending} in the destination activity's {@code onResume}.
 * This survives the activity transition because the message is held in a static field
 * until the next activity is ready to display it.</p>
 */
public final class AppToast {

    public static final int LENGTH_SHORT = Toast.LENGTH_SHORT;
    public static final int LENGTH_LONG = Toast.LENGTH_LONG;

    private static volatile String pendingMessage;
    private static volatile int pendingDuration;

    private AppToast() {}

    /**
     * Queues a message to be shown by the next activity that calls
     * {@link #consumePending(Context)}. Use this instead of {@link #show} when the
     * current activity is about to finish so the toast survives the screen transition.
     */
    public static void scheduleForNextActivity(CharSequence message, int duration) {
        pendingMessage = message.toString();
        pendingDuration = duration;
    }

    public static void scheduleForNextActivity(Context context, int messageRes, int duration) {
        scheduleForNextActivity(context.getString(messageRes), duration);
    }

    /**
     * Shows and clears any pending toast queued by {@link #scheduleForNextActivity}.
     * Call this from {@code onResume} of every activity that is a navigation target.
     */
    public static void consumePending(Context context) {
        String msg = pendingMessage;
        int dur = pendingDuration;
        if (msg != null) {
            pendingMessage = null;
            pendingDuration = 0;
            show(context, msg, dur);
        }
    }

    public static void show(Context context, CharSequence message, int duration) {
        Activity activity = resolveActivity(context);
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        ViewGroup root = (ViewGroup) activity.getWindow().getDecorView();
        View view = LayoutInflater.from(activity).inflate(R.layout.layout_custom_toast, root, false);
        ((TextView) view.findViewById(R.id.toastMessage)).setText(message);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        params.bottomMargin = dp(activity, 100);
        view.setLayoutParams(params);

        root.addView(view);
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(220).start();

        int displayMs = (duration == Toast.LENGTH_LONG) ? 5000 : 2000;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (view.getParent() != null) {
                view.animate().alpha(0f).setDuration(220)
                        .withEndAction(() -> root.removeView(view))
                        .start();
            }
        }, displayMs);
    }

    public static void show(Context context, int messageRes, int duration) {
        show(context, context.getString(messageRes), duration);
    }

    private static Activity resolveActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    private static int dp(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }
}
