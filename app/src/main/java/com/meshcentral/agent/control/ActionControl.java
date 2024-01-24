package com.meshcentral.agent.control;

import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;

import okio.ByteString;

public class ActionControl {
    private static final String TAG = "ActionControl";
    private static ActionControl instance;
    private Long mousePressedStart = null;
    private ArrayList<MouseAction> mouseActions = new ArrayList<>();

    public static ActionControl getInstance() {
        if (instance == null) {
            instance = new ActionControl();
        }
        return instance;
    }

    public void dispatchTouchData(ByteString msg, int cmdsize) {
        if (cmdsize == 10) { // Mouse drag and press actions
            int actionFlag = msg.getByte(5) & 0xFF;
            int x = (msg.getByte(6) & 0xFF) * 256 + (msg.getByte(7) & 0xFF);
            int y = (msg.getByte(8) & 0xFF) * 256 + (msg.getByte(9) & 0xFF);
            switch (actionFlag) {
                case 0: // Mouse move between down and up
                    if (mousePressedStart != null) {
                        mouseActions.add(new MouseAction(x, y, MotionEvent.ACTION_MOVE));
                    }
                    break;
                case 2: // Action mouse down
                    mouseActions.clear();
                    mousePressedStart = SystemClock.uptimeMillis();
                    mouseActions.add(new MouseAction(x, y, MotionEvent.ACTION_DOWN));
                    break;
                case 4: // Action mouse up
                    try {
                        mouseActions.add(new MouseAction(x, y, MotionEvent.ACTION_UP));
                        if (mouseActions.size() == 2) {
                            MouseControl.getService().performPress(mouseActions.get(1).getX(), mouseActions.get(1).getY());
                        } else if (mouseActions.size() > 2) {
                            MouseControl.getService().performSwipe(mouseActions.get(0).getX(), mouseActions.get(0).getY(),
                                    mouseActions.get(mouseActions.size()-1).getX(),
                                    mouseActions.get(mouseActions.size()-1).getY());
                        }
                        mouseActions.clear();
                        mousePressedStart = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    public void dispatchKeyboardData(ByteString msg, boolean isLegacyKeyboard) {
        try {
            boolean isKeyDown = (msg.getByte(4) & 0xFF) == 0;
            if (!isKeyDown) {
                return;
            }
            if (isLegacyKeyboard) {
                int legacyUnicode = msg.getByte(5) & 0xFF;
                switch (legacyUnicode) {
                    case 13: // Enter
                        KeyboardCustom.getKeyboard().performEnter();
                        break;
                    case 8: // Backspace
                        KeyboardCustom.getKeyboard().performBackspace();
                        break;
                    case 9: //Tab
                    case 16: //Shift;
                    case 17: // Ctrl
                    case 18: // Alt
                    case 37: // Arrow left
                    case 38: // Arrow up
                    case 39: // Arrow right
                    case 40: // Arrow down
                    case 20: // Caps lock
                    case 27: // Escape
                    case 65: // Select all text Ctrl + a
                    default:
                        Log.e(TAG, "onKeyboardEvent, legacy unicode not supported " + legacyUnicode);
                        break;
                }
                return;
            };

            // Unicode
            char charKey = (char) (msg.getByte(6) & 0xFF);
            KeyboardCustom.getKeyboard().commitChar(String.valueOf(charKey));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
