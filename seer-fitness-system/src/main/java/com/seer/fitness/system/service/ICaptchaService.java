package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.CaptchaConfigResponse;
import com.seer.fitness.system.dto.CaptchaResponse;

public interface ICaptchaService {

    CaptchaResponse generateCaptcha();

    boolean verifyCaptcha(String captchaId, String userInput);

    CaptchaConfigResponse getCaptchaConfig();
}
