package com.group9.eventgaze.repository;

import com.group9.eventgaze.entity.Colleges;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollegesRepo extends JpaRepository<Colleges, Long> {
}
