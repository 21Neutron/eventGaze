package com.group9.eventgaze.repository;

import com.group9.eventgaze.entity.Publishers;
import com.group9.eventgaze.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PublishersRepo extends JpaRepository<Publishers,Long> {

    Optional<Publishers> findByUsers(Users user);
}
