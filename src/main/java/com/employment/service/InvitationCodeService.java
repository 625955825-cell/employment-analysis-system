package com.employment.service;

import com.employment.model.entity.InvitationCode;

import java.util.List;

public interface InvitationCodeService {

    InvitationCode generateCode(int expireDays, String remark);

    List<InvitationCode> generateBatch(int count, int expireDays, String remark);

    List<InvitationCode> list();

    InvitationCode getById(Long id);

    void delete(Long id);

    void deleteBatch(List<Long> ids);

    void markAsUsed(Long codeId, Long userId, String username);
}
