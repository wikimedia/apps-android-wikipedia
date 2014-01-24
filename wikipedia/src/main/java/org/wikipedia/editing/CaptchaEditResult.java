package org.wikipedia.editing;

import org.wikipedia.Site;

// Handles only Image Captchas
public class CaptchaEditResult extends EditingResult {
    private final String captchaId;
    private final String captchaPath;

    public CaptchaEditResult(String captchaId, String captchaPath) {
        super("Failure");
        this.captchaId = captchaId;
        this.captchaPath = captchaPath;
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public String getCaptchaUrl(Site site) {
        return site.getFullUrl(captchaPath);
    }
}
