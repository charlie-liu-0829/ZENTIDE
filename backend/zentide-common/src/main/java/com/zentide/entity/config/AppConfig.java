package com.zentide.entity.config;

import com.zentide.utils.StringTools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component("appConfig")
public class AppConfig {
    /**
     * 文件目录
     */
    @Value("${project.folder:}")
    private String projectFolder;

    @Value("${admin.account:admin}")
    private String adminAccount;

    @Value("${admin.password:}")
    private String adminPassword;


    public String getProjectFolder() {
        // 始终返回绝对路径，避免 MultipartFile.transferTo 将相对路径解析到 Tomcat 临时目录。
        String configured = StringTools.isEmpty(projectFolder) ? "." : projectFolder;
        Path folder = Paths.get(configured).toAbsolutePath().normalize();
        return folder.toString() + File.separator;
    }

    public String getAdminAccount() {
        return adminAccount;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

}
