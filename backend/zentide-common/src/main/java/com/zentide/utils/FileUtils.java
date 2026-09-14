package com.zentide.utils;

import com.zentide.constants.Constants;
import com.zentide.entity.config.AppConfig;
import com.zentide.entity.enums.DateTimePatternEnum;
import com.zentide.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

@Component
@Slf4j
public class FileUtils {

    private static final Set<String> IMAGE_SUFFIXES = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".avif");
    private static final Set<String> VIDEO_SUFFIXES = Set.of(".mp4", ".webm", ".mov");
    private static final Set<String> IMAGE_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp", "image/avif");
    private static final Set<String> VIDEO_MIME_TYPES = Set.of("video/mp4", "video/webm", "video/quicktime");
    private static final long MAX_INLINE_IMAGE_BYTES = 15L * 1024 * 1024;
    private static final long MAX_INLINE_VIDEO_BYTES = 100L * 1024 * 1024;

    @Resource
    private AppConfig appConfig;

    public String uploadImage(MultipartFile file, Boolean createThumbnail) throws IOException {
        String folderName = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMM.getPattern()) + "/";
        Path folder = Path.of(appConfig.getProjectFolder(), Constants.FILE_FOLDER_FILE, folderName);
        Files.createDirectories(folder);
        String fileName = StringTools.getRandomString(Constants.LENGTH_30);
        String suffix = StringTools.getFileSuffix(file.getOriginalFilename());
        String resultFileName = fileName + suffix;
        Path resultPath = folder.resolve(resultFileName);
        // 直接复制输入流，绕过 Servlet 容器对相对目标路径的特殊解析。
        try (var input = file.getInputStream()) {
            Files.copy(input, resultPath);
        }
        if (createThumbnail != null && createThumbnail) {
            String thumbnail = fileName + Constants.IMAGE_THUMBNAIL_SUFFIX + suffix;
            createImageThumbnail(resultPath.toString(), folder.resolve(thumbnail).toString());
            return folderName + thumbnail;
        }
        return folderName + resultFileName;
    }

    public String uploadPostMedia(MultipartFile file, String requestedType) throws IOException {
        if (file == null || file.isEmpty()) throw new BusinessException("请选择需要上传的媒体文件");
        String mediaType = requestedType == null ? "" : requestedType.trim().toLowerCase(Locale.ROOT);
        boolean image = "image".equals(mediaType);
        boolean video = "video".equals(mediaType);
        if (!image && !video) throw new BusinessException("不支持的媒体类型");

        String originalName = file.getOriginalFilename();
        int suffixIndex = originalName == null ? -1 : originalName.lastIndexOf('.');
        if (suffixIndex < 0) throw new BusinessException("媒体文件缺少有效扩展名");
        String suffix = originalName.substring(suffixIndex).toLowerCase(Locale.ROOT);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        Set<String> allowedSuffixes = image ? IMAGE_SUFFIXES : VIDEO_SUFFIXES;
        Set<String> allowedMimeTypes = image ? IMAGE_MIME_TYPES : VIDEO_MIME_TYPES;
        long maxBytes = image ? MAX_INLINE_IMAGE_BYTES : MAX_INLINE_VIDEO_BYTES;
        if (!allowedSuffixes.contains(suffix) || !allowedMimeTypes.contains(contentType)) {
            throw new BusinessException(image ? "仅支持 JPG、PNG、GIF、WebP 或 AVIF 图片" : "仅支持 MP4、WebM 或 MOV 视频");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException(image ? "正文图片不能超过 15MB" : "正文视频不能超过 100MB");
        }

        String folderName = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMM.getPattern()) + "/";
        Path folder = Path.of(appConfig.getProjectFolder(), Constants.FILE_FOLDER_FILE, folderName);
        Files.createDirectories(folder);
        String resultFileName = StringTools.getRandomString(Constants.LENGTH_30) + suffix;
        Path resultPath = folder.resolve(resultFileName);
        try (var input = file.getInputStream()) {
            Files.copy(input, resultPath);
        }
        return folderName + resultFileName;
    }

    private void createImageThumbnail(String filePath, String thumbnailPath) {
        final String CMD_CREATE_IMAGE_THUMBNAIL = "ffmpeg -i \"%s\" -vf scale=200:-1 \"%s\"";
        String cmd = String.format(CMD_CREATE_IMAGE_THUMBNAIL, filePath, thumbnailPath);
        ProcessUtils.executeCommand(cmd, true);
    }
}
