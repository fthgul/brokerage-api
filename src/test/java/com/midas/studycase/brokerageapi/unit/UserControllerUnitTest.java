package com.midas.studycase.brokerageapi.unit;

import com.midas.studycase.brokerageapi.controller.UserController;
import com.midas.studycase.brokerageapi.exception.UserAlreadyExistsException;
import com.midas.studycase.brokerageapi.exception.UserNotFoundException;
import com.midas.studycase.brokerageapi.model.response.GetUserDetailResponse;
import com.midas.studycase.brokerageapi.model.response.GetUserResponseList;
import com.midas.studycase.brokerageapi.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerUnitTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    public void testListAllUsers() {
        GetUserResponseList responseList = new GetUserResponseList(); // Dummy list
        when(userService.listAllUsers()).thenReturn(responseList);

        ResponseEntity<GetUserResponseList> response = userController.listAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseList, response.getBody());
    }

    @Test
    public void testGetUserDetails() {
        GetUserDetailResponse userDetails = new GetUserDetailResponse(1L, "JohnV2",  "john.doe@example.com");
        when(userService.getUserById(1L)).thenReturn(userDetails);

        ResponseEntity<GetUserDetailResponse> response = userController.getUserDetails(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userDetails, response.getBody());
    }

    @Test
    public void testGetUserDetails_UserAlreadyExists() {
        // Given
        String email = "test@example.com";
        when(userService.getUserById(1L)).thenThrow(new UserAlreadyExistsException(email));

        // When
        Exception exception = assertThrows(UserAlreadyExistsException.class, () -> {
            userController.getUserDetails(1L);
        });

        // Then
        assertTrue(exception.getMessage().contains("User with email " + email + " already exists."));
    }

    @Test
    public void testGetUserDetails_UserNotFound() {
        // Given
        when(userService.getUserById(2L)).thenThrow(new UserNotFoundException("User not found"));

        // When
        Exception exception = assertThrows(UserNotFoundException.class, () -> {
            userController.getUserDetails(2L);
        });

        // Then
        assertTrue(exception.getMessage().contains("User not found"));
    }


}
