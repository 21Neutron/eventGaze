package com.group9.eventgaze.controller;

import com.group9.eventgaze.entity.EventCategory;
import com.group9.eventgaze.service.EventCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/eventgaze/category")
public class EventCategoryController {

    @Autowired
    private EventCategoryService eventCategoryService;

//    For getting a list of all events Category

    @GetMapping("/getAll")
    public ResponseEntity<List<EventCategory>> getAll(){

        List<EventCategory> eventCategories = eventCategoryService.getAllCategory();

        if (eventCategories.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }else {
            return new ResponseEntity<>(eventCategories,HttpStatus.OK);
        }
    }


}
