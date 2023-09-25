package com.midas.studycase.brokerageapi.controller;

import com.midas.studycase.brokerageapi.model.request.CreateUserRequest;
import com.midas.studycase.brokerageapi.model.response.CreateUserResponse;
import com.midas.studycase.brokerageapi.model.response.GetUserDetailResponse;
import com.midas.studycase.brokerageapi.model.response.GetUserResponseList;
import com.midas.studycase.brokerageapi.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    @PostMapping
    public ResponseEntity<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping
    public ResponseEntity<GetUserResponseList> listAllUsers() {
        return ResponseEntity.status(HttpStatus.OK).body(userService.listAllUsers());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<GetUserDetailResponse> getUserDetails(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }
}

