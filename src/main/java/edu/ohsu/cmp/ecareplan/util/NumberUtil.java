package edu.ohsu.cmp.ecareplan.util;

import java.math.BigDecimal;
import java.math.BigInteger;

public class NumberUtil {
    public static BigDecimal toBigDecimal(Number value) {
        if (value == null) return null;

        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof BigInteger integer) return new BigDecimal(integer);
        if (value instanceof Byte || value instanceof Short ||
                value instanceof Integer || value instanceof Long) {
            return BigDecimal.valueOf(value.longValue());
        }

        if (value instanceof Float || value instanceof Double) {
            return BigDecimal.valueOf(value.doubleValue());
        }

        return new BigDecimal(value.toString());
    }
}
