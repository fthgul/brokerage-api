package com.midas.studycase.brokerageapi.model.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateUserResponse {
    private long id;
    private String username;
    private String email;
}
