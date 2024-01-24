package com.meshcentral.agent.control;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class MouseControl extends AccessibilityService {
    private static MouseControl mouseControl;
    public static MouseControl getService() {
        return mouseControl;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED |
                AccessibilityEvent.TYPE_VIEW_FOCUSED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        info.notificationTimeout = 100;
        this.setServiceInfo(info);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mouseControl = this;
    }

    public boolean performSwipe(int x0, int y0, int x1, int y1) {
        final int DURATION = 100;

        Path path = new Path();
        path.moveTo(x0, y0);
        path.lineTo(x1, y1);
        GestureDescription.StrokeDescription pathStroke =
                new GestureDescription.StrokeDescription(path, 0, DURATION);
        GestureDescription.Builder pathBuilder = new GestureDescription.Builder();
        pathBuilder.addStroke(pathStroke);
        return dispatchGesture(pathBuilder.build(), null, null);
    }

    public boolean performPress(int x, int y) {
        final int DURATION = 1;

        Path clickPath = new Path();
        clickPath.moveTo(x, y);
        GestureDescription.StrokeDescription clickStroke =
                new GestureDescription.StrokeDescription(clickPath, 0, DURATION);
        GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
        clickBuilder.addStroke(clickStroke);
        return dispatchGesture(clickBuilder.build(), null, null);
    }
}
