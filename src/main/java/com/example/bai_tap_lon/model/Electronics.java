package com.example.bai_tap_lon.model;

import java.time.LocalDateTime;

public class Electronics extends Item {
    private String brand;
    private int warrantyMonths;

    public Electronics(String name, String description, double startingPrice,
                       LocalDateTime startTime, LocalDateTime endTime,
                       String brand, int warrantyMonths) {
        // Gọi constructor của lớp cha (Item)
        super(name, description, startingPrice, startTime, endTime);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }

    @Override
    public String getItemCategory() {
        return "Đồ Điện Tử (Electronics)";
    }

    @Override
    public void printInfo() {
        super.printInfo(); // Tái sử dụng code in thông tin chung từ Item
        System.out.println("Thương hiệu: " + this.brand);
        System.out.println("Bảo hành: " + this.warrantyMonths + " tháng");
        System.out.println("--------------------------");
    }
}