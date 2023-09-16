package com.midas.studycase.brokerageapi.repository;

import com.midas.studycase.brokerageapi.model.entity.UserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserEntityRepository extends CrudRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    @Override
    List<UserEntity> findAll();
}
