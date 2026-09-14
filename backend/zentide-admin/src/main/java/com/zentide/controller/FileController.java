package com.zentide.controller;

import com.zentide.constants.Constants;
import com.zentide.entity.config.AppConfig;
import com.zentide.entity.enums.ResponseCodeEnum;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.exception.BusinessException;
import com.zentide.utils.FileUtils;
import com.zentide.utils.StringTools;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Validated
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController extends ABaseController {

    @Resource
    private AppConfig appConfig;

    @Resource
    private FileUtils fileUtils;

    @RequestMapping("/uploadImage")
    public ResponseVO uploadCover(@NotNull MultipartFile file, Boolean createThumbnail) throws IOException {
        String filePath = fileUtils.uploadImage(file, createThumbnail);
        return getSuccessResponseVO(filePath);
    }

    @RequestMapping("/getResource")
    public void getResource(HttpServletResponse response, @NotEmpty String sourceName) {
        if (!StringTools.pathIsOk(sourceName)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String suffix = StringTools.getFileSuffix(sourceName);
        response.setContentType("image/" + suffix.replace(".", ""));
        response.setHeader("Cache-Control", "max-age=2592000");
        readFile(response, sourceName);
    }

    protected void readFile(HttpServletResponse response, String filePath) {
        if (!StringTools.pathIsOk(filePath)) {
            return;
        }
        File file = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + filePath);
        if (!file.exists()) {
            return;
        }
        try (OutputStream out = response.getOutputStream(); FileInputStream in = new FileInputStream(file)) {
            byte[] byteData = new byte[1024];
            int len = 0;
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            log.error("读取文件异常", e);
        }
    }
}
