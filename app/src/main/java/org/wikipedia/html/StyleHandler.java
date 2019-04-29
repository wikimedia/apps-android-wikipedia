package org.wikipedia.html;

import androidx.annotation.NonNull;

public class StyleHandler {

    private static String buildJavaScriptString(@NonNull String script, @NonNull String value) {
        return script + " = '" + value + "'";
    }

    public static String setContentDivTopMargin(float topMargin) {
        return buildJavaScriptString("document.getElementById('content').style.marginTop", topMargin + "px");
    }

    public static String setBodyTopPadding(float topMargin) {
        return buildJavaScriptString("document.body.style.paddingTop", topMargin + "px");
    }

    public static String setBodyBottomPadding(float topMargin) {
        return buildJavaScriptString("document.body.style.paddingBottom", topMargin + "px");
    }
}
