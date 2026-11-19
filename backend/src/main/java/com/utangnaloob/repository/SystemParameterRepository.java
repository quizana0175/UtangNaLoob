package com.utangnaloob.repository;

import com.utangnaloob.entity.SystemParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemParameterRepository extends JpaRepository<SystemParameter, Long> {
    Optional<SystemParameter> findByParamKey(String paramKey);
    Boolean existsByParamKey(String paramKey);
}
