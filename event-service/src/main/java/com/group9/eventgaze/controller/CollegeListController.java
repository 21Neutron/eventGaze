package com.group9.eventgaze.controller;


import com.group9.eventgaze.entity.Colleges;
import com.group9.eventgaze.service.CollegesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/eventgaze/collegeList")
public class CollegeListController {

    @Autowired
    private CollegesService collegesService;

//    For fetching all colleges in a list

    @GetMapping("/getAll")
    public ResponseEntity<List<Colleges>> getAllColleges(){
        List<Colleges> colleges = collegesService.getCollegeList();

        if (colleges.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }else {
            return new ResponseEntity<>(colleges,HttpStatus.OK);
        }
    }

//    For fetching college by collegeId

    @GetMapping("/id{myId}")
    public ResponseEntity<Colleges> getCollegeById(@PathVariable Long myId){
        Colleges college = collegesService.getCollegeById(myId);
        if (college != null) {
            return ResponseEntity.ok(college);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }


}
