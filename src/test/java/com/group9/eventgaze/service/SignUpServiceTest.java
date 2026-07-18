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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignUpServiceTest {

    @Mock
    private UsersRepo usersRepo;

    @Mock
    private StudentsRepo studentsRepo;

    @Mock
    private PublishersRepo publishersRepo;

    @Mock
    private CollegesRepo collegesRepo;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @InjectMocks
    private SignUpService signUpService;

    private StudentSignUpDTO studentDTO;
    private PublisherSignUpDTO publisherDTO;
    private Colleges college;

    @BeforeEach
    void setUp() {
        college = new Colleges();
        college.setCollegeId(1L);
        college.setCollegeName("Test College");

        studentDTO = new StudentSignUpDTO();
        studentDTO.setUserEmail("student@test.com");
        studentDTO.setUserPassword("password123");
        studentDTO.setFirstName("John");
        studentDTO.setLastName("Doe");
        studentDTO.setCollegeId(1L);

        publisherDTO = new PublisherSignUpDTO();
        publisherDTO.setUserEmail("publisher@test.com");
        publisherDTO.setUserPassword("secret123");
        publisherDTO.setPublisherOrgName("Tech Society");
    }

    @Test
    @DisplayName("registerStudent: Successful student registration")
    void testRegisterStudent_Success() {
        when(usersRepo.findByUserEmail("student@test.com")).thenReturn(Optional.empty());
        when(collegesRepo.findById(1L)).thenReturn(Optional.of(college));
        when(bCryptPasswordEncoder.encode("password123")).thenReturn("encodedPassword123");

        Users savedUser = new Users();
        savedUser.setUserId(10L);
        savedUser.setUserEmail("student@test.com");
        savedUser.setUserRole("STUDENT");
        when(usersRepo.save(any(Users.class))).thenReturn(savedUser);

        signUpService.registerStudent(studentDTO);

        ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
        verify(usersRepo).save(userCaptor.capture());
        Users capturedUser = userCaptor.getValue();
        assertEquals("student@test.com", capturedUser.getUserEmail());
        assertEquals("encodedPassword123", capturedUser.getUserPassword());
        assertEquals("STUDENT", capturedUser.getUserRole());

        ArgumentCaptor<Students> studentCaptor = ArgumentCaptor.forClass(Students.class);
        verify(studentsRepo).save(studentCaptor.capture());
        Students capturedStudent = studentCaptor.getValue();
        assertEquals("John", capturedStudent.getFirstName());
        assertEquals("Doe", capturedStudent.getLastName());
        assertEquals(college, capturedStudent.getCollege());
        assertEquals(savedUser, capturedStudent.getUsers());
    }

    @Test
    @DisplayName("registerStudent: Throws exception when email is already registered")
    void testRegisterStudent_EmailAlreadyExists() {
        Users existingUser = new Users();
        existingUser.setUserEmail("student@test.com");
        when(usersRepo.findByUserEmail("student@test.com")).thenReturn(Optional.of(existingUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                signUpService.registerStudent(studentDTO));
        assertEquals("Email is already registered", ex.getMessage());

        verify(usersRepo, never()).save(any());
        verify(studentsRepo, never()).save(any());
    }

    @Test
    @DisplayName("registerStudent: Throws exception when email is null or blank")
    void testRegisterStudent_InvalidEmail() {
        studentDTO.setUserEmail(null);
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                signUpService.registerStudent(studentDTO));
        assertEquals("Email cannot be null or blank", ex1.getMessage());

        studentDTO.setUserEmail("   ");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                signUpService.registerStudent(studentDTO));
        assertEquals("Email cannot be null or blank", ex2.getMessage());

        verify(usersRepo, never()).save(any());
    }

    @Test
    @DisplayName("registerStudent: Throws exception when password is null or blank")
    void testRegisterStudent_InvalidPassword() {
        studentDTO.setUserPassword(null);
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                signUpService.registerStudent(studentDTO));
        assertEquals("Password cannot be null or blank", ex1.getMessage());

        studentDTO.setUserPassword("   ");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                signUpService.registerStudent(studentDTO));
        assertEquals("Password cannot be null or blank", ex2.getMessage());

        verify(usersRepo, never()).save(any());
    }

    @Test
    @DisplayName("registerStudent: Throws exception when DTO is null")
    void testRegisterStudent_NullDTO() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                signUpService.registerStudent(null));
        assertEquals("Student sign up data cannot be null", ex.getMessage());
    }

    @Test
    @DisplayName("registerStudent: Throws exception when college ID is null or invalid")
    void testRegisterStudent_InvalidCollegeId() {
        studentDTO.setCollegeId(null);
        when(usersRepo.findByUserEmail(studentDTO.getUserEmail())).thenReturn(Optional.empty());

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                signUpService.registerStudent(studentDTO));
        assertEquals("Invalid College ID", ex1.getMessage());

        studentDTO.setCollegeId(999L);
        when(collegesRepo.findById(999L)).thenReturn(Optional.empty());
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                signUpService.registerStudent(studentDTO));
        assertEquals("Invalid College ID", ex2.getMessage());

        verify(usersRepo, never()).save(any());
    }

    @Test
    @DisplayName("registerPublisher: Successful publisher registration")
    void testRegisterPublisher_Success() {
        when(usersRepo.findByUserEmail("publisher@test.com")).thenReturn(Optional.empty());
        when(bCryptPasswordEncoder.encode("secret123")).thenReturn("encodedSecret123");

        Users savedUser = new Users();
        savedUser.setUserId(20L);
        savedUser.setUserEmail("publisher@test.com");
        savedUser.setUserRole("PUBLISHER");
        when(usersRepo.save(any(Users.class))).thenReturn(savedUser);

        signUpService.registerPublisher(publisherDTO);

        ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
        verify(usersRepo).save(userCaptor.capture());
        Users capturedUser = userCaptor.getValue();
        assertEquals("publisher@test.com", capturedUser.getUserEmail());
        assertEquals("encodedSecret123", capturedUser.getUserPassword());
        assertEquals("PUBLISHER", capturedUser.getUserRole());

        ArgumentCaptor<Publishers> publisherCaptor = ArgumentCaptor.forClass(Publishers.class);
        verify(publishersRepo).save(publisherCaptor.capture());
        Publishers capturedPublisher = publisherCaptor.getValue();
        assertEquals("Tech Society", capturedPublisher.getPublisherOrgName());
        assertEquals(savedUser, capturedPublisher.getUsers());
    }

    @Test
    @DisplayName("registerPublisher: Throws exception when email is already registered")
    void testRegisterPublisher_EmailAlreadyExists() {
        Users existingUser = new Users();
        existingUser.setUserEmail("publisher@test.com");
        when(usersRepo.findByUserEmail("publisher@test.com")).thenReturn(Optional.of(existingUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                signUpService.registerPublisher(publisherDTO));
        assertEquals("Email is already registered", ex.getMessage());

        verify(usersRepo, never()).save(any());
        verify(publishersRepo, never()).save(any());
    }

    @Test
    @DisplayName("registerPublisher: Throws exception when email is null or blank")
    void testRegisterPublisher_InvalidEmail() {
        publisherDTO.setUserEmail(null);
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                signUpService.registerPublisher(publisherDTO));
        assertEquals("Email cannot be null or blank", ex1.getMessage());

        publisherDTO.setUserEmail("   ");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                signUpService.registerPublisher(publisherDTO));
        assertEquals("Email cannot be null or blank", ex2.getMessage());

        verify(usersRepo, never()).save(any());
    }

    @Test
    @DisplayName("registerPublisher: Throws exception when password is null or blank")
    void testRegisterPublisher_InvalidPassword() {
        publisherDTO.setUserPassword(null);
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                signUpService.registerPublisher(publisherDTO));
        assertEquals("Password cannot be null or blank", ex1.getMessage());

        publisherDTO.setUserPassword("   ");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                signUpService.registerPublisher(publisherDTO));
        assertEquals("Password cannot be null or blank", ex2.getMessage());

        verify(usersRepo, never()).save(any());
    }

    @Test
    @DisplayName("registerPublisher: Throws exception when DTO is null")
    void testRegisterPublisher_NullDTO() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                signUpService.registerPublisher(null));
        assertEquals("Publisher sign up data cannot be null", ex.getMessage());
    }
}
