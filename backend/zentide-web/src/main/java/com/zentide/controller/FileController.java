package com.zentide.controller;

import com.zentide.annotation.GlobalInterceptor;
import com.zentide.constants.Constants;
import com.zentide.entity.config.AppConfig;
import com.zentide.entity.enums.ResponseCodeEnum;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.exception.BusinessException;
import com.zentide.utils.FileUtils;
import com.zentide.utils.StringTools;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Locale;

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
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO uploadCover(@NotNull MultipartFile file, Boolean createThumbnail) throws IOException {
        String filePath = fileUtils.uploadImage(file, createThumbnail);
        return getSuccessResponseVO(filePath);
    }

    @RequestMapping("/uploadPostMedia")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO uploadPostMedia(@NotNull MultipartFile file, @NotEmpty String mediaType) throws IOException {
        return getSuccessResponseVO(fileUtils.uploadPostMedia(file, mediaType));
    }

    @RequestMapping("/getResource")
    public void getResource(HttpServletRequest request, HttpServletResponse response, @NotEmpty String sourceName) {
        if (!StringTools.pathIsOk(sourceName)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String suffix = StringTools.getFileSuffix(sourceName);
        response.setContentType(contentType(suffix));
        response.setHeader("Cache-Control", "max-age=2592000");
        readFile(request, response, sourceName);
    }

    protected void readFile(HttpServletRequest request, HttpServletResponse response, String filePath) {
        if (!StringTools.pathIsOk(filePath)) {
            return;
        }
        File file = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + filePath);
        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        long fileLength = file.length();
        long start = 0;
        long end = fileLength - 1;
        String range = request.getHeader("Range");
        response.setHeader("Accept-Ranges", "bytes");
        if (range != null && range.startsWith("bytes=") && !range.contains(",")) {
            try {
                String[] bounds = range.substring(6).split("-", 2);
                if (bounds[0].isBlank()) {
                    long suffixLength = Long.parseLong(bounds[1]);
                    if (suffixLength <= 0) throw new NumberFormatException();
                    start = Math.max(0, fileLength - suffixLength);
                } else {
                    start = Long.parseLong(bounds[0]);
                    if (bounds.length > 1 && !bounds[1].isBlank()) end = Long.parseLong(bounds[1]);
                }
                if (start < 0 || end < start || start >= fileLength) throw new NumberFormatException();
                end = Math.min(end, fileLength - 1);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            } catch (NumberFormatException exception) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + fileLength);
                return;
            }
        }
        long contentLength = end - start + 1;
        response.setHeader("Content-Length", String.valueOf(contentLength));
        try (OutputStream out = response.getOutputStream(); RandomAccessFile in = new RandomAccessFile(file, "r")) {
            in.seek(start);
            byte[] byteData = new byte[1024];
            long remaining = contentLength;
            while (remaining > 0) {
                int len = in.read(byteData, 0, (int) Math.min(byteData.length, remaining));
                if (len == -1) break;
                out.write(byteData, 0, len);
                remaining -= len;
            }
            out.flush();
        } catch (Exception e) {
            log.error("读取文件异常", e);
        }
    }

    private String contentType(String requestedSuffix) {
        String suffix = requestedSuffix == null ? "" : requestedSuffix.toLowerCase(Locale.ROOT);
        return switch (suffix) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".avif" -> "image/avif";
            case ".mp4" -> "video/mp4";
            case ".webm" -> "video/webm";
            case ".mov" -> "video/quicktime";
            default -> "application/octet-stream";
        };
    }
}
