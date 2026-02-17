package com.besedka.controller;

import com.besedka.dto.CabinResponse;
import com.besedka.repository.CabinRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cabins")
public class CabinController {

    private final CabinRepository cabinRepository;

    public CabinController(CabinRepository cabinRepository) {
        this.cabinRepository = cabinRepository;
    }

    @GetMapping
    public List<CabinResponse> list() {
        return cabinRepository.findByActiveTrue()
                .stream()
                .map(CabinResponse::from)
                .toList();
    }
}
