package com.besedka.dto;

import com.besedka.model.Cabin;

import java.math.BigDecimal;
import java.util.List;

public record CabinResponse(
        Long id,
        String name,
        String location,
        BigDecimal pricePerHour,
        String description,
        List<String> photos) {

    public static CabinResponse from(Cabin cabin) {
        return new CabinResponse(
                cabin.getId(),
                cabin.getName(),
                cabin.getLocation(),
                cabin.getPricePerHour(),
                cabin.getDescription(),
                cabin.getPhotos()
        );
    }
}
