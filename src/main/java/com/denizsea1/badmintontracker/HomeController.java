package com.denizsea1.badmintontracker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {
    @Value("${spring.application.name}$")
    String appName;


    @RequestMapping("/")
    public String index(){
        System.out.println("Name der App: " + appName);
        return "index.html";
    }
}
