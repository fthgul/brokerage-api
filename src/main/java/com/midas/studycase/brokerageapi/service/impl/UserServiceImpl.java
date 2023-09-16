package com.midas.studycase.brokerageapi.service.impl;

import com.midas.studycase.brokerageapi.exception.UserAlreadyExistsException;
import com.midas.studycase.brokerageapi.exception.UserNotFoundException;
import com.midas.studycase.brokerageapi.model.entity.UserEntity;
import com.midas.studycase.brokerageapi.model.mapper.UserMapper;
import com.midas.studycase.brokerageapi.model.request.CreateUserRequest;
import com.midas.studycase.brokerageapi.model.response.CreateUserResponse;
import com.midas.studycase.brokerageapi.model.response.GetUserDetailResponse;
import com.midas.studycase.brokerageapi.model.response.GetUserResponseList;
import com.midas.studycase.brokerageapi.repository.UserEntityRepository;
import com.midas.studycase.brokerageapi.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserEntityRepository userEntityRepository;

    @Override
    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        if (userEntityRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException(request.getEmail());
        }
        UserEntity userEntity = UserMapper.INSTANCE.toUserEntity(request);
        userEntityRepository.save(userEntity);
        return UserMapper.INSTANCE.userEntityToCreateUserResponse(userEntity);
    }

    @Override
    public GetUserResponseList listAllUsers() {
        return UserMapper.INSTANCE.toGetUserResponseList(userEntityRepository.findAll());
    }


    @Override
    public GetUserDetailResponse getUserById(Long userId) {
        return userEntityRepository.findById(userId)
                .map(UserMapper.INSTANCE::userEntityToGetUserDetailResponse)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }
}
