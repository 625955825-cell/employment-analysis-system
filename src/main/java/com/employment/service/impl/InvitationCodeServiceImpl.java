package com.employment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.employment.model.entity.InvitationCode;
import com.employment.repository.InvitationCodeRepository;
import com.employment.service.InvitationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationCodeServiceImpl implements InvitationCodeService {

    private final InvitationCodeRepository invitationCodeRepository;

    @Override
    public InvitationCode generateCode(int expireDays, String remark) {
        String code = generateUniqueCode();
        InvitationCode invitationCode = new InvitationCode();
        invitationCode.setCode(code);
        invitationCode.setExpiresTime(LocalDateTime.now().plusDays(expireDays));
        invitationCode.setStatus("unused");
        if (remark != null && !remark.trim().isEmpty()) {
            invitationCode.setRemark(remark);
        }
        return invitationCodeRepository.save(invitationCode);
    }

    @Override
    @Transactional
    public List<InvitationCode> generateBatch(int count, int expireDays, String remark) {
        List<InvitationCode> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            codes.add(generateCode(expireDays, remark));
        }
        return codes;
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = IdUtil.simpleUUID().substring(0, 8).toUpperCase();
        } while (invitationCodeRepository.existsByCode(code));
        return code;
    }

    @Override
    public List<InvitationCode> list() {
        return invitationCodeRepository.findAll().stream()
                .sorted(Comparator.comparing(InvitationCode::getCreateTime).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public InvitationCode getById(Long id) {
        return invitationCodeRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        invitationCodeRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        invitationCodeRepository.deleteAllByIdInBatch(ids);
    }

    @Override
    @Transactional
    public void markAsUsed(Long codeId, Long userId, String username) {
        invitationCodeRepository.findById(codeId).ifPresent(ic -> {
            ic.setStatus("used");
            ic.setUsedBy(userId);
            ic.setUsedUsername(username);
            ic.setUsedTime(LocalDateTime.now());
            invitationCodeRepository.save(ic);
        });
    }
}
