package com.example.bai_tap_lon.model;

import java.time.LocalDateTime;

public class Vehicle extends Item {
    private String make;
    private String model;
    private double mileage;

    public Vehicle(String name, String description, double startingPrice,
                   LocalDateTime startTime, LocalDateTime endTime,
                   String make, String model, double mileage) {
        super(name, description, startingPrice, startTime, endTime);
        this.make = make;
        this.model = model;
        this.mileage = mileage;
    }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public double getMileage() { return mileage; }
    public void setMileage(double mileage) { this.mileage = mileage; }

    @Override
    public String getItemCategory() {
        return "Phương Tiện (Vehicle)";
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println("Hãng sản xuất: " + this.make);
        System.out.println("Dòng xe: " + this.model);
        System.out.println("Số km đã đi: " + this.mileage + " km");
        System.out.println("--------------------------");
    }
}