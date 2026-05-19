package com.example.bai_tap_lon;

import com.example.bai_tap_lon.model.entity.item.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    private static final LocalDateTime START = LocalDateTime.now();
    private static final LocalDateTime END   = START.plusDays(3);

    @Test
    void createElectronics() {
        Item item = ItemFactory.createItem("electronics", "Phone", "Smartphone", 5_000_000, START, END, "Samsung");
        assertInstanceOf(Electronics.class, item);
        assertEquals("Phone", item.getName());
        assertEquals(5_000_000, item.getStartingPrice());
    }

    @Test
    void createArt() {
        Item item = ItemFactory.createItem("art", "Painting", "Oil on canvas", 2_000_000, START, END, "Picasso");
        assertInstanceOf(Art.class, item);
        assertEquals("Painting", item.getName());
    }

    @Test
    void createVehicle() {
        Item item = ItemFactory.createItem("vehicle", "Car", "SUV", 300_000_000, START, END, "Toyota");
        assertInstanceOf(Vehicle.class, item);
        assertEquals("Car", item.getName());
    }

    @Test
    void unknownTypeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.createItem("unknown", "X", "", 1000, START, END, ""));
    }

    @Test
    void caseInsensitiveType() {
        assertDoesNotThrow(
                () -> ItemFactory.createItem("ELECTRONICS", "TV", "4K", 10_000_000, START, END, "LG"));
    }

    @Test
    void startingPriceSetAsCurrentHighestBid() {
        Item item = ItemFactory.createItem("art", "Sculpture", "Modern", 3_000_000, START, END, "Artist");
        assertEquals(item.getStartingPrice(), item.getCurrentHighestBid());
    }
}
