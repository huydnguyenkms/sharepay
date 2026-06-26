package com.sharepay.split;

import com.sharepay.common.MoneyUtil;
import com.sharepay.exception.BadRequestException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExactSplitStrategy implements SplitStrategy {

    @Override
    public Map<Long, BigDecimal> computeShares(BigDecimal net, List<SplitInput> inputs, String currency) {
        if (inputs.isEmpty()) {
            throw new BadRequestException("At least one participant is required");
        }
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (SplitInput input : inputs) {
            if (input.value() == null || input.value().signum() < 0) {
                throw new BadRequestException("Exact split requires a non-negative amount per participant");
            }
            BigDecimal share = MoneyUtil.round(input.value(), currency);
            result.put(input.memberId(), share);
            sum = sum.add(share);
        }
        if (sum.compareTo(MoneyUtil.round(net, currency)) != 0) {
            throw new BadRequestException(
                    "Exact split amounts (" + sum + ") must sum to the shareable amount (" + MoneyUtil.round(net, currency) + ")");
        }
        return result;
    }
}
