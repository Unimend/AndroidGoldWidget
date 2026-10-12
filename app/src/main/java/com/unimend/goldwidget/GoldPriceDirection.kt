package com.unimend.goldwidget

enum class GoldPriceDirection { RISING, FALLING, FLAT }

fun goldPriceDirection(previous: Double, current: Double, epsilon: Double = 0.01): GoldPriceDirection =
    when {
        current > previous + epsilon -> GoldPriceDirection.RISING
        current < previous - epsilon -> GoldPriceDirection.FALLING
        else -> GoldPriceDirection.FLAT
    }
