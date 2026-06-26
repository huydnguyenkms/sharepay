package com.sharepay.split;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Computes how a net shareable amount is divided among participants. Implementations must
 * return shares that sum exactly to {@code net} (rounded to the currency's scale).
 */
public interface SplitStrategy {

    /**
     * @param net      the amount to divide (expense amount minus sponsorship)
     * @param inputs   participants and their per-method input values
     * @param currency ISO currency code, controls rounding scale
     * @return ordered map of memberId to share amount, summing exactly to {@code net}
     */
    Map<Long, BigDecimal> computeShares(BigDecimal net, List<SplitInput> inputs, String currency);
}
