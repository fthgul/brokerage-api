package com.midas.studycase.brokerageapi.model.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GetUserResponseList {
    private List<User> users;


    @Getter
    @Setter
    @NoArgsConstructor
    public static class User{
        private long id;
        private String username;
        private String email;
    }
}

