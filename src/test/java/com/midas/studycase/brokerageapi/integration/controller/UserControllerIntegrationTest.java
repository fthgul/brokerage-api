package com.midas.studycase.brokerageapi.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.midas.studycase.brokerageapi.TestBrokerageApiApplication;
import com.midas.studycase.brokerageapi.model.entity.UserEntity;
import com.midas.studycase.brokerageapi.model.request.CreateUserRequest;
import com.midas.studycase.brokerageapi.repository.UserEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = TestBrokerageApiApplication.class)
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(locations = "classpath:application-test.properties")
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserEntityRepository userEntityRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        initializeUser();
    }

    private void initializeUser() {
        userEntityRepository.deleteAll();
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("test@testExample.com");
        userEntity.setUsername("test_1_user");
        userEntityRepository.save(userEntity);
    }

    @Test
    public void shouldListAllUsers() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void shouldReturnUserDetails() throws Exception {
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void shouldCreateUser() throws Exception {
        CreateUserRequest createUserRequest = createCreateUserRequest();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    private CreateUserRequest createCreateUserRequest() {
        return new CreateUserRequest("John_Doe_2", "john.doe_2@example.com");
    }
}
