package com.group9.eventgaze.service;

import com.group9.eventgaze.entity.Colleges;
import com.group9.eventgaze.repository.CollegesRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CollegesService {

    @Autowired
    private CollegesRepo collegesRepo;

    public List<Colleges> getCollegeList(){
        return collegesRepo.findAll();
    }

    public Colleges getCollegeById(Long id) {
        return collegesRepo.findById(id).orElse(null);
    }

    public Colleges findById(Long collegeId) {
        return collegesRepo.findById(collegeId)
                .orElseThrow(() -> new EntityNotFoundException("College not found"));
    }

}
