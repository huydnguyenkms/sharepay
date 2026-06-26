package com.sharepay.common;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;

/**
 * Currency-aware money helpers. Amounts are split in the smallest currency unit so no
 * money is ever lost or created: the rounding remainder is distributed deterministically
 * to the participants with the largest fractional parts (ties broken by position).
 */
public final class MoneyUtil {

    private MoneyUtil() {
    }

    public static int scaleFor(String currencyCode) {
        try {
            int digits = Currency.getInstance(currencyCode).getDefaultFractionDigits();
            return digits >= 0 ? digits : 2;
        } catch (IllegalArgumentException | NullPointerException ex) {
            return 2;
        }
    }

    public static BigDecimal round(BigDecimal amount, String currencyCode) {
        return amount.setScale(scaleFor(currencyCode), RoundingMode.HALF_UP);
    }

    public static BigDecimal zero(String currencyCode) {
        return BigDecimal.ZERO.setScale(scaleFor(currencyCode), RoundingMode.UNNECESSARY);
    }

    /**
     * Distribute {@code total} across the given weights, proportionally, returning amounts
     * that sum exactly to {@code total} (rounded to the currency's scale). Weights must be
     * non-negative and not all zero.
     */
    public static List<BigDecimal> distributeProportionally(BigDecimal total,
                                                            List<BigDecimal> weights,
                                                            String currencyCode) {
        int scale = scaleFor(currencyCode);
        int n = weights.size();
        if (n == 0) {
            return List.of();
        }

        BigDecimal weightSum = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (weightSum.signum() <= 0) {
            throw new IllegalArgumentException("Sum of weights must be positive");
        }

        // Work in minor units (integers) to guarantee an exact split.
        BigInteger totalMinor = total.movePointRight(scale).setScale(0, RoundingMode.HALF_UP).toBigIntegerExact();

        long[] base = new long[n];
        BigDecimal[] fraction = new BigDecimal[n];
        BigInteger allocated = BigInteger.ZERO;
        BigDecimal totalMinorDec = new BigDecimal(totalMinor);

        for (int i = 0; i < n; i++) {
            BigDecimal exact = totalMinorDec.multiply(weights.get(i))
                    .divide(weightSum, 10, RoundingMode.HALF_UP);
            BigDecimal floor = exact.setScale(0, RoundingMode.FLOOR);
            base[i] = floor.longValueExact();
            fraction[i] = exact.subtract(floor);
            allocated = allocated.add(BigInteger.valueOf(base[i]));
        }

        long remainder = totalMinor.subtract(allocated).longValueExact();

        // Hand out leftover minor units to the largest fractional parts first.
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            order.add(i);
        }
        order.sort(Comparator.<Integer>comparingDouble(i -> fraction[i].doubleValue()).reversed());
        for (int k = 0; k < remainder; k++) {
            base[order.get(k % n)]++;
        }

        List<BigDecimal> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            result.add(BigDecimal.valueOf(base[i]).movePointLeft(scale).setScale(scale, RoundingMode.UNNECESSARY));
        }
        return result;
    }
}
