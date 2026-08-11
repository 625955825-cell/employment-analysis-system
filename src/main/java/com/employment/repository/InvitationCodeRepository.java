package com.employment.repository;

import com.employment.model.entity.InvitationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvitationCodeRepository extends JpaRepository<InvitationCode, Long> {
    Optional<InvitationCode> findByCode(String code);
    Optional<InvitationCode> findByCodeAndStatus(String code, String status);
    boolean existsByCode(String code);
}
