package com.sharepay.settlement;

import java.math.BigDecimal;

/** A single "who pays whom" instruction in a settlement plan. */
public record Transfer(Long fromMemberId, Long toMemberId, BigDecimal amount) {
}
