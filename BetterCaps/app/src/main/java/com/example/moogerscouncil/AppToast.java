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
 */
public final class AppToast {

    public static final int LENGTH_SHORT = Toast.LENGTH_SHORT;
    public static final int LENGTH_LONG = Toast.LENGTH_LONG;

    private AppToast() {}

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

        int displayMs = (duration == Toast.LENGTH_LONG) ? 3500 : 2000;
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
