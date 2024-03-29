package com.foodies.dirtydining.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class BaseController {

    private final Logger logger = LoggerFactory.getLogger(BaseController.class);

    @GetMapping("/")
    public String ping() {
        return "Welcome to the Dirty Dining API";
    }

}
