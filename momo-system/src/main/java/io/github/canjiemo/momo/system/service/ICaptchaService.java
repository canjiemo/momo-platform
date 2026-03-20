package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.momo.system.dto.CaptchaConfigResponse;
import io.github.canjiemo.momo.system.dto.CaptchaResponse;

public interface ICaptchaService {

    CaptchaResponse generateCaptcha();

    boolean verifyCaptcha(String captchaId, String userInput);

    CaptchaConfigResponse getCaptchaConfig();
}
