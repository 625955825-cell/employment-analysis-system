package com.employment.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

/**
 * 已禁用：自动初始化数据的功能已移除。
 * 所有数据由 init.sql 导入，不再由后端启动时自动生成。
 * 如需手动初始化，可通过管理员界面调用 API。
 */
// @Component  // 已禁用，不再作为 Spring Bean 加载
@Slf4j
public class DataInitRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        // 自动初始化已禁用，所有数据由 init.sql 导入
    }
}
