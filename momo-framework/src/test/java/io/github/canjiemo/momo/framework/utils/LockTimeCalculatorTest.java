package io.github.canjiemo.momo.framework.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 渐进式锁定时间计算逻辑单元测试
 * <p>
 * 使用参数化版本 calculateProgressiveLockMinutes(failAttempts, maxFailCount, baseMinutes, multiplier, maxMinutes)
 * 不依赖 Spring 容器。
 */
class LockTimeCalculatorTest {

    private LockTimeCalculator calculator;

    // 默认配置：maxFailCount=5, base=30min, 倍增×2, 上限1440min
    private static final int MAX_FAIL = 5;
    private static final int BASE = 30;
    private static final double MULT = 2.0;
    private static final int MAX = 1440;

    @BeforeEach
    void setUp() {
        calculator = new LockTimeCalculator();
        // calculateProgressiveLockMinutes 不使用注入的 config，可直接 new
    }

    @Test
    @DisplayName("首次触发锁定（failAttempts == maxFailCount）→ 锁定 baseMinutes")
    void firstLock_equalsBase() {
        // overAttempts = 5 - 5 = 0 → 30 * 2^0 = 30
        long result = calculator.calculateProgressiveLockMinutes(5, MAX_FAIL, BASE, MULT, MAX);
        assertEquals(30L, result);
    }

    @Test
    @DisplayName("失败 6 次 → 锁定 60 分钟（30 × 2^1）")
    void sixFailures_60min() {
        long result = calculator.calculateProgressiveLockMinutes(6, MAX_FAIL, BASE, MULT, MAX);
        assertEquals(60L, result);
    }

    @Test
    @DisplayName("失败 7 次 → 锁定 120 分钟（30 × 2^2）")
    void sevenFailures_120min() {
        long result = calculator.calculateProgressiveLockMinutes(7, MAX_FAIL, BASE, MULT, MAX);
        assertEquals(120L, result);
    }

    @Test
    @DisplayName("失败 8 次 → 锁定 240 分钟（30 × 2^3）")
    void eightFailures_240min() {
        long result = calculator.calculateProgressiveLockMinutes(8, MAX_FAIL, BASE, MULT, MAX);
        assertEquals(240L, result);
    }

    @Test
    @DisplayName("超大失败次数 → 结果被上限 1440 截断")
    void overflow_cappedAtMax() {
        // 30 * 2^10 = 30720 → 超过 1440，应返回 1440
        long result = calculator.calculateProgressiveLockMinutes(15, MAX_FAIL, BASE, MULT, MAX);
        assertEquals(1440L, result);
    }

    @Test
    @DisplayName("倍增系数 = 1.0 → 每次锁定时间相同（固定 base）")
    void multiplierOne_alwaysBase() {
        long r1 = calculator.calculateProgressiveLockMinutes(5, 5, 30, 1.0, 1440);
        long r2 = calculator.calculateProgressiveLockMinutes(8, 5, 30, 1.0, 1440);
        long r3 = calculator.calculateProgressiveLockMinutes(20, 5, 30, 1.0, 1440);
        assertEquals(30L, r1);
        assertEquals(30L, r2);
        assertEquals(30L, r3);
    }

    @Test
    @DisplayName("自定义 base=10, 倍增×3, 上限 100")
    void customParams() {
        // failAttempts=4, maxFail=3 → overAttempts=1 → 10 * 3^1 = 30
        long result = calculator.calculateProgressiveLockMinutes(4, 3, 10, 3.0, 100);
        assertEquals(30L, result);
    }
}
