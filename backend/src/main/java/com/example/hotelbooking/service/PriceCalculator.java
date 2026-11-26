package com.example.hotelbooking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** The single place where booking totals are computed. The client never sends a price. */
public final class PriceCalculator {

    private PriceCalculator() {
    }

    public static BigDecimal total(BigDecimal pricePerNight, int nights) {
        return pricePerNight.multiply(BigDecimal.valueOf(nights)).setScale(2, RoundingMode.HALF_UP);
    }
}
