package com.employment.controller;

import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.entity.InvitationCode;
import com.employment.service.InvitationCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/invitation-codes")
@RequiredArgsConstructor
public class InvitationCodeController {

    private final InvitationCodeService invitationCodeService;

    @GetMapping("/list")
    public Result<List<InvitationCode>> list() {
        return Result.success(invitationCodeService.list());
    }

    @PostMapping("/generate")
    @OperationLog(module = "注册码管理", content = "生成注册码")
    public Result<?> generate(
            @RequestParam(defaultValue = "1") Integer count,
            @RequestParam(defaultValue = "90") Integer expireDays,
            @RequestParam(required = false) String remark) {
        List<InvitationCode> codes = invitationCodeService.generateBatch(count, expireDays, remark);
        return Result.success("成功生成 " + codes.size() + " 个注册码", codes);
    }

    @GetMapping("/{id}")
    public Result<InvitationCode> getById(@PathVariable Long id) {
        return Result.success(invitationCodeService.getById(id));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "注册码管理", content = "删除注册码")
    public Result<?> delete(@PathVariable Long id) {
        invitationCodeService.delete(id);
        return Result.success("删除成功", null);
    }

    @DeleteMapping("/batch")
    @OperationLog(module = "注册码管理", content = "批量删除注册码")
    public Result<?> deleteBatch(@RequestBody List<Long> ids) {
        invitationCodeService.deleteBatch(ids);
        return Result.success("批量删除成功", null);
    }
}
