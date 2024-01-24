package com.meshcentral.agent.control;

import android.annotation.SuppressLint;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.meshcentral.agent.R;

public class KeyboardCustom extends InputMethodService implements KeyboardView.OnKeyboardActionListener{
    private static final String TAG = "KeyboardCustom";
    @SuppressLint("StaticFieldLeak")
    private static KeyboardCustom keyboardCustom;
    private Keyboard keyboardQWERT;
    private Keyboard keyboardSpecial;
    boolean isDefaultKeyboard = true;
    private KeyboardView keyboardView;

    @Override
    public View onCreateInputView() {
        Log.d(TAG, "onCreateInputView: ");
        keyboardCustom = this;
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_view, null);
        isDefaultKeyboard = true;
        keyboardQWERT = new Keyboard(this, R.xml.keyboard_qwert);
        keyboardSpecial = new Keyboard(this, R.xml.keyboard_special);
        keyboardView.setKeyboard(keyboardQWERT);
        keyboardView.setOnKeyboardActionListener(this);
        return keyboardView;
    }

    private void switchKeyboardLayout() {
        if (keyboardQWERT == null || keyboardSpecial == null) {
            return;
        }
        if (isDefaultKeyboard) {
            keyboardView.setKeyboard(keyboardSpecial);
        } else {
            keyboardView.setKeyboard(keyboardQWERT);
        }
        isDefaultKeyboard = !isDefaultKeyboard;
    }

    public static KeyboardCustom getKeyboard() {
        return keyboardCustom;
    }

    public void commitChar(String character) {
        if (keyboardCustom == null) {
            return;
        }

        InputConnection inputConnection = keyboardCustom.getCurrentInputConnection();
        if (inputConnection == null) {
            Log.e(TAG, "commitChar, inputConnection null");
            return;
        }

        inputConnection.commitText(character, 1);
    }

    public void performBackspace() {
        InputConnection inputConnection = keyboardCustom.getCurrentInputConnection();
        if (inputConnection == null) {
            Log.e(TAG, "commitChar, inputConnection null");
            return;
        }
        CharSequence selectedText = inputConnection.getSelectedText(0);

        if (TextUtils.isEmpty(selectedText)) {
            inputConnection.deleteSurroundingText(1, 0);
        } else {
            inputConnection.commitText("", 1);
        }
    }

    public void performEnter() {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null) {
            return;
        }
        final EditorInfo editorInfo = getCurrentInputEditorInfo();
        final int imeOptionsActionId = InputTypeUtils.getImeOptionsActionIdFromEditorInfo(editorInfo);
        if (InputTypeUtils.IME_ACTION_CUSTOM_LABEL == imeOptionsActionId) {
            inputConnection.performEditorAction(editorInfo.actionId);
        } else if (EditorInfo.IME_ACTION_NONE != imeOptionsActionId) {
            inputConnection.performEditorAction(imeOptionsActionId);
        } else {
            inputConnection.commitText(String.valueOf((char) 10), 1);
        }
    }

    @Override
    public void onPress(int primaryCode) {

    }

    @Override
    public void onRelease(int primaryCode) {

    }

    @Override
    public void onKey(int code, int[] keyCodes) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            switch(code) {
                case Keyboard.KEYCODE_DELETE:
                    CharSequence selectedText = inputConnection.getSelectedText(0);

                    if (TextUtils.isEmpty(selectedText)) {
                        inputConnection.deleteSurroundingText(1, 0);
                    } else {
                        inputConnection.commitText("", 1);
                    }

                    break;
                case Keyboard.KEYCODE_SHIFT:
                    keyboardView.setShifted(!keyboardView.isShifted());
                    break;
                case Keyboard.KEYCODE_MODE_CHANGE:
                    switchKeyboardLayout();
                    break;
                case 10: // Enter
                    performEnter();
                    break;
                default :
                    int unicode = code;
                    if (code >= 97 && code <= 122 && keyboardView.isShifted()) { // a-z
                        unicode = unicode - 32;
                    }
                    char character = (char) unicode;
                    inputConnection.commitText(String.valueOf(character), 1);
            }
        }
    }

    @Override
    public void onText(CharSequence text) {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }
}
