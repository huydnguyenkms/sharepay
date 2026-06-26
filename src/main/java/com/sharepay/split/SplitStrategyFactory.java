package com.sharepay.split;

import com.sharepay.domain.enums.SplitMethod;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SplitStrategyFactory {

    private final Map<SplitMethod, SplitStrategy> strategies = Map.of(
            SplitMethod.EQUAL, new EqualSplitStrategy(),
            SplitMethod.EXACT, new ExactSplitStrategy(),
            SplitMethod.PERCENTAGE, new PercentageSplitStrategy(),
            SplitMethod.WEIGHT, new WeightSplitStrategy()
    );

    public SplitStrategy forMethod(SplitMethod method) {
        SplitStrategy strategy = strategies.get(method);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported split method: " + method);
        }
        return strategy;
    }
}
