package com.sharepay.split;

import com.sharepay.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SplitStrategyTest {

    private BigDecimal sum(Map<Long, BigDecimal> shares) {
        return shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void equalSplitDistributesRemainderWithoutLosingMoney() {
        var shares = new EqualSplitStrategy().computeShares(
                new BigDecimal("1000"),
                List.of(new SplitInput(1L, null), new SplitInput(2L, null), new SplitInput(3L, null)),
                "VND");

        assertThat(sum(shares)).isEqualByComparingTo("1000");
        // 1000 / 3 -> one member gets 334, the others 333
        assertThat(shares.values()).contains(new BigDecimal("334"));
        assertThat(shares.values().stream().filter(v -> v.compareTo(new BigDecimal("333")) == 0).count()).isEqualTo(2);
    }

    @Test
    void equalSplitWithCentsKeepsTwoDecimals() {
        var shares = new EqualSplitStrategy().computeShares(
                new BigDecimal("10.00"),
                List.of(new SplitInput(1L, null), new SplitInput(2L, null), new SplitInput(3L, null)),
                "USD");
        assertThat(sum(shares)).isEqualByComparingTo("10.00");
        shares.values().forEach(v -> assertThat(v.scale()).isEqualTo(2));
    }

    @Test
    void exactSplitValidatesTotal() {
        var strategy = new ExactSplitStrategy();
        var ok = strategy.computeShares(new BigDecimal("1000"),
                List.of(new SplitInput(1L, new BigDecimal("600")), new SplitInput(2L, new BigDecimal("400"))),
                "VND");
        assertThat(sum(ok)).isEqualByComparingTo("1000");

        assertThatThrownBy(() -> strategy.computeShares(new BigDecimal("1000"),
                List.of(new SplitInput(1L, new BigDecimal("600")), new SplitInput(2L, new BigDecimal("300"))),
                "VND")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void percentageSplitMustSumTo100() {
        var strategy = new PercentageSplitStrategy();
        var shares = strategy.computeShares(new BigDecimal("1000"),
                List.of(new SplitInput(1L, new BigDecimal("50")),
                        new SplitInput(2L, new BigDecimal("30")),
                        new SplitInput(3L, new BigDecimal("20"))),
                "VND");
        assertThat(shares.get(1L)).isEqualByComparingTo("500");
        assertThat(shares.get(2L)).isEqualByComparingTo("300");
        assertThat(shares.get(3L)).isEqualByComparingTo("200");

        assertThatThrownBy(() -> strategy.computeShares(new BigDecimal("1000"),
                List.of(new SplitInput(1L, new BigDecimal("50")), new SplitInput(2L, new BigDecimal("40"))),
                "VND")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void weightSplitIsProportional() {
        var shares = new WeightSplitStrategy().computeShares(new BigDecimal("900"),
                List.of(new SplitInput(1L, new BigDecimal("2")), new SplitInput(2L, new BigDecimal("1"))),
                "VND");
        assertThat(shares.get(1L)).isEqualByComparingTo("600");
        assertThat(shares.get(2L)).isEqualByComparingTo("300");
        assertThat(sum(shares)).isEqualByComparingTo("900");
    }
}
