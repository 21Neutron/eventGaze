package com.group9.eventgaze.service;

import com.group9.eventgaze.entity.Colleges;
import com.group9.eventgaze.entity.Publishers;
import com.group9.eventgaze.entity.Students;
import com.group9.eventgaze.entity.Users;
import com.group9.eventgaze.entity.dto.PublisherSignUpDTO;
import com.group9.eventgaze.entity.dto.StudentSignUpDTO;
import com.group9.eventgaze.repository.CollegesRepo;
import com.group9.eventgaze.repository.PublishersRepo;
import com.group9.eventgaze.repository.StudentsRepo;
import com.group9.eventgaze.repository.UsersRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignUpService {
    @Autowired
    private UsersRepo usersRepo;

    @Autowired
    private StudentsRepo studentsRepo;

    @Autowired
    private PublishersRepo publishersRepo;

    @Autowired
    private CollegesRepo collegesRepo;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    public SignUpService() {
    }

    public SignUpService(UsersRepo usersRepo, StudentsRepo studentsRepo,
                         PublishersRepo publishersRepo, CollegesRepo collegesRepo,
                         BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.usersRepo = usersRepo;
        this.studentsRepo = studentsRepo;
        this.publishersRepo = publishersRepo;
        this.collegesRepo = collegesRepo;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Transactional
    public void registerStudent(StudentSignUpDTO studentDTO) {
        if (studentDTO == null) {
            throw new IllegalArgumentException("Student sign up data cannot be null");
        }
        if (studentDTO.getUserEmail() == null || studentDTO.getUserEmail().isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        if (studentDTO.getUserPassword() == null || studentDTO.getUserPassword().isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }
        if (usersRepo.findByUserEmail(studentDTO.getUserEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (studentDTO.getCollegeId() == null) {
            throw new IllegalArgumentException("Invalid College ID");
        }

        Colleges college = collegesRepo.findById(studentDTO.getCollegeId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid College ID"));

        Users user = new Users();
        user.setUserEmail(studentDTO.getUserEmail());
        user.setUserPassword(bCryptPasswordEncoder.encode((studentDTO.getUserPassword())));
        user.setUserRole("STUDENT");
        Users savedUser = usersRepo.save(user);

        //

        Students students = new Students();
        students.setFirstName(studentDTO.getFirstName());
        students.setLastName(studentDTO.getLastName());
        students.setCollege(college);
        students.setUsers(savedUser);
        studentsRepo.save(students);

    }

    @Transactional
    public void registerPublisher(PublisherSignUpDTO publisherSignUpDTO) {
        if (publisherSignUpDTO == null) {
            throw new IllegalArgumentException("Publisher sign up data cannot be null");
        }
        if (publisherSignUpDTO.getUserEmail() == null || publisherSignUpDTO.getUserEmail().isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        if (publisherSignUpDTO.getUserPassword() == null || publisherSignUpDTO.getUserPassword().isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }
        if (usersRepo.findByUserEmail(publisherSignUpDTO.getUserEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }

        Users users = new Users();
        users.setUserEmail(publisherSignUpDTO.getUserEmail());
        users.setUserPassword(bCryptPasswordEncoder.encode(publisherSignUpDTO.getUserPassword()));
        users.setUserRole("PUBLISHER");
        Users savedUser = usersRepo.save(users);

        //
        Publishers publishers = new Publishers();
        publishers.setPublisherOrgName(publisherSignUpDTO.getPublisherOrgName());
        publishers.setUsers(savedUser);
        publishersRepo.save(publishers);
    }

}
