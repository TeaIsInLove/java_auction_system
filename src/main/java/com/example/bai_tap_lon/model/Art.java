package com.example.bai_tap_lon.model;

import java.time.LocalDateTime;

public class Art extends Item {
    private String artist;
    private int creationYear;

    public Art(String name, String description, double startingPrice,
               LocalDateTime startTime, LocalDateTime endTime,
               String artist, int creationYear) {
        super(name, description, startingPrice, startTime, endTime);
        this.artist = artist;
        this.creationYear = creationYear;
    }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public int getCreationYear() { return creationYear; }
    public void setCreationYear(int creationYear) { this.creationYear = creationYear; }

    @Override
    public String getItemCategory() {
        return "Tác Phẩm Nghệ Thuật (Art)";
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println("Tác giả: " + this.artist);
        System.out.println("Năm sáng tác: " + this.creationYear);
        System.out.println("--------------------------");
    }
}