package com.example.logistics.management.entity;

public class Address {

    private String city;
    private String country;
    private String address;

    public Address() {
    }

    public Address(String city, String country, String address) {
        this.city = city;
        this.country = country;
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getAddress() {
        return address;
    }
}
