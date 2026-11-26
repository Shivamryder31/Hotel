package com.example.hotelbooking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class PriceCalculatorTest {

    @Test
    void totalIsPricePerNightTimesNights() {
        assertEquals(new BigDecimal("19500.00"), PriceCalculator.total(new BigDecimal("6500.00"), 3));
    }

    @Test
    void totalKeepsTwoDecimalPlaces() {
        assertEquals(new BigDecimal("100.05"), PriceCalculator.total(new BigDecimal("33.35"), 3));
        assertEquals(new BigDecimal("50.00"), PriceCalculator.total(new BigDecimal("50"), 1));
    }
}
