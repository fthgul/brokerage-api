package com.midas.studycase.brokerageapi.service;

import com.midas.studycase.brokerageapi.model.request.CreateUserRequest;
import com.midas.studycase.brokerageapi.model.response.CreateUserResponse;
import com.midas.studycase.brokerageapi.model.response.GetUserDetailResponse;
import com.midas.studycase.brokerageapi.model.response.GetUserResponseList;

public interface UserService {
    CreateUserResponse createUser(CreateUserRequest request);
    GetUserResponseList listAllUsers();
    GetUserDetailResponse getUserById(Long userId);
}
