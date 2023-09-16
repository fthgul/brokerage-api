package com.midas.studycase.brokerageapi.model.mapper;

import com.midas.studycase.brokerageapi.model.entity.UserEntity;
import com.midas.studycase.brokerageapi.model.request.CreateUserRequest;
import com.midas.studycase.brokerageapi.model.response.CreateUserResponse;
import com.midas.studycase.brokerageapi.model.response.GetUserDetailResponse;
import com.midas.studycase.brokerageapi.model.response.GetUserResponseList;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
    UserEntity toUserEntity(CreateUserRequest request);
    CreateUserResponse userEntityToCreateUserResponse(UserEntity userEntity);

    GetUserDetailResponse userEntityToGetUserDetailResponse(UserEntity userEntity);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "email", target = "email")
    GetUserResponseList.User userEntityToGetUserResponse(UserEntity userEntity);

    default GetUserResponseList toGetUserResponseList(List<UserEntity> userEntities) {
        GetUserResponseList response = new GetUserResponseList();
        response.setUsers(userEntitiesToGetUserResponses(userEntities));
        return response;
    }

    List<GetUserResponseList.User> userEntitiesToGetUserResponses(List<UserEntity> userEntities);
}

