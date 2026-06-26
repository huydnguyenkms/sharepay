package com.sharepay.split;

import java.math.BigDecimal;

/**
 * One participant's split input. The meaning of {@code value} depends on the split method:
 * ignored for EQUAL, exact money for EXACT, a percentage for PERCENTAGE, a weight for WEIGHT.
 */
public record SplitInput(Long memberId, BigDecimal value) {
}
