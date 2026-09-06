package com.group9.eventgaze.repository;


import com.group9.eventgaze.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepo extends JpaRepository<Users, Long> {

    Optional<Users>findByUserEmail(String email);

}
