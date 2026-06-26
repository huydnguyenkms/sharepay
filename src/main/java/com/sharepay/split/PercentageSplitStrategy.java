package com.sharepay.split;

import com.sharepay.common.MoneyUtil;
import com.sharepay.exception.BadRequestException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PercentageSplitStrategy implements SplitStrategy {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    @Override
    public Map<Long, BigDecimal> computeShares(BigDecimal net, List<SplitInput> inputs, String currency) {
        if (inputs.isEmpty()) {
            throw new BadRequestException("At least one participant is required");
        }
        BigDecimal pctSum = BigDecimal.ZERO;
        for (SplitInput input : inputs) {
            if (input.value() == null || input.value().signum() < 0) {
                throw new BadRequestException("Percentage split requires a non-negative percent per participant");
            }
            pctSum = pctSum.add(input.value());
        }
        if (pctSum.subtract(HUNDRED).abs().compareTo(TOLERANCE) > 0) {
            throw new BadRequestException("Percentages must sum to 100 (got " + pctSum + ")");
        }

        List<BigDecimal> weights = inputs.stream().map(SplitInput::value).collect(Collectors.toList());
        List<BigDecimal> amounts = MoneyUtil.distributeProportionally(net, weights, currency);

        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 0; i < inputs.size(); i++) {
            result.put(inputs.get(i).memberId(), amounts.get(i));
        }
        return result;
    }
}
