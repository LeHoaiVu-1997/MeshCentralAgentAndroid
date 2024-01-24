package com.meshcentral.agent.control;

import android.view.inputmethod.EditorInfo;

public class InputTypeUtils {
    public static final int IME_ACTION_CUSTOM_LABEL = EditorInfo.IME_MASK_ACTION + 1;

    public static int getImeOptionsActionIdFromEditorInfo(final EditorInfo editorInfo) {
        if ((editorInfo.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            return EditorInfo.IME_ACTION_NONE;
        } else if (editorInfo.actionLabel != null) {
            return IME_ACTION_CUSTOM_LABEL;
        } else {
            return editorInfo.imeOptions & EditorInfo.IME_MASK_ACTION;
        }
    }
}
