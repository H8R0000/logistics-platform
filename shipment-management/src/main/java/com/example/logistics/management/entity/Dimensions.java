package com.example.logistics.management.entity;

import java.math.BigDecimal;

public class Dimensions {

    private BigDecimal length;

    private BigDecimal width;

    private BigDecimal height;

    public Dimensions() {
    }

    public Dimensions(BigDecimal length, BigDecimal width, BigDecimal height) {
        this.length = length;
        this.width = width;
        this.height = height;
    }

    public BigDecimal getLength() {
        return length;
    }

    public BigDecimal getWidth() {
        return width;
    }

    public BigDecimal getHeight() {
        return height;
    }
}
