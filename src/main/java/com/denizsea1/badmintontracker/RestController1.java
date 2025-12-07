package com.denizsea1.badmintontracker;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class RestController1 {

    @GetMapping("/api/hello")
    public String hello() {
        return "Hallo SEA1!";
    }
}
