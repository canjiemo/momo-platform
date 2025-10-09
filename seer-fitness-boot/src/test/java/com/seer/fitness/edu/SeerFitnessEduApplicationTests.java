package com.seer.fitness.edu;

import com.seer.fitness.system.utils.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = SeerFitnessEduApplication.class)
class SeerFitnessEduApplicationTests {


    @Autowired
    private PasswordUtil passwordUtil;

    @Test
    void tese1(){
        String s = passwordUtil.encryptPassword("Aa123456!");
        System.out.println(s);
    }

}
