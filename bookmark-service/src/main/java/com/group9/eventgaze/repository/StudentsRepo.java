package com.group9.eventgaze.repository;

import com.group9.eventgaze.entity.Students;
import com.group9.eventgaze.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentsRepo extends JpaRepository<Students, Long> {

    Optional<Students> findByUsers(Users user);

    Optional<Students> findByUsers_UserId(Long userId);
}
