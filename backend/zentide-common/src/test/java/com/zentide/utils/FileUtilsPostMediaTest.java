package com.zentide.utils;

import com.zentide.constants.Constants;
import com.zentide.entity.config.AppConfig;
import com.zentide.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilsPostMediaTest {
    @TempDir
    Path tempDirectory;

    @Test
    void storesSupportedPostVideoInManagedFileDirectory() throws Exception {
        FileUtils fileUtils = configuredFileUtils();
        MockMultipartFile video = new MockMultipartFile("file", "现场记录.mp4", "video/mp4", new byte[]{0, 1, 2, 3});

        String storedName = fileUtils.uploadPostMedia(video, "video");

        assertTrue(storedName.matches("\\d{6}/[A-Za-z0-9]{30}\\.mp4"));
        assertTrue(Files.exists(tempDirectory.resolve(Constants.FILE_FOLDER_FILE).resolve(storedName)));
    }

    @Test
    void rejectsExecutableOrSvgContentAsInlineMedia() {
        FileUtils fileUtils = configuredFileUtils();
        MockMultipartFile svg = new MockMultipartFile("file", "payload.svg", "image/svg+xml", "<svg onload=alert(1)>".getBytes());

        assertThrows(BusinessException.class, () -> fileUtils.uploadPostMedia(svg, "image"));
    }

    private FileUtils configuredFileUtils() {
        AppConfig appConfig = new AppConfig();
        ReflectionTestUtils.setField(appConfig, "projectFolder", tempDirectory.toString());
        FileUtils fileUtils = new FileUtils();
        ReflectionTestUtils.setField(fileUtils, "appConfig", appConfig);
        return fileUtils;
    }
}
