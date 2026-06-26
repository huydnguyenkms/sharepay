package com.sharepay.split;

import com.sharepay.common.MoneyUtil;
import com.sharepay.exception.BadRequestException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WeightSplitStrategy implements SplitStrategy {

    @Override
    public Map<Long, BigDecimal> computeShares(BigDecimal net, List<SplitInput> inputs, String currency) {
        if (inputs.isEmpty()) {
            throw new BadRequestException("At least one participant is required");
        }
        for (SplitInput input : inputs) {
            if (input.value() == null || input.value().signum() <= 0) {
                throw new BadRequestException("Weight split requires a positive weight per participant");
            }
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
