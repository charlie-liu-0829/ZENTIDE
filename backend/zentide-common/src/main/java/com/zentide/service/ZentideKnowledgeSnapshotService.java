package com.zentide.service;

import com.zentide.entity.po.ZentideInterestComment;
import com.zentide.entity.po.ZentideInterestHub;
import com.zentide.entity.po.ZentideInterestPost;
import com.zentide.mapper.ZentideInterestMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Writes a human-readable, derived Markdown snapshot for each published post.
 * The database remains the source of truth; failures here are logged and do not
 * make a successful post/comment transaction fail.
 */
@Service
public class ZentideKnowledgeSnapshotService {
    private static final Logger log = LoggerFactory.getLogger(ZentideKnowledgeSnapshotService.class);

    private final ZentideInterestMapper mapper;
    private final Path root;

    public ZentideKnowledgeSnapshotService(
            ZentideInterestMapper mapper,
            @Value("${zentide.knowledge.snapshot-dir:./data/knowledge-snapshots}") String snapshotDirectory) {
        this.mapper = mapper;
        String configured = snapshotDirectory == null || snapshotDirectory.isBlank()
                ? "./data/knowledge-snapshots" : snapshotDirectory.trim();
        this.root = Path.of(configured).toAbsolutePath().normalize();
    }

    public void refreshPost(Long postId) {
        if (postId == null) return;
        try {
            ZentideInterestPost post = mapper.findPost(postId, null);
            if (post == null || !"PUBLISHED".equalsIgnoreCase(post.getStatus())) {
                deletePost(postId);
                return;
            }
            ZentideInterestHub hub = post.getHubId() == null ? null : mapper.findHub(post.getHubId());
            if (hub == null || !"ACTIVE".equalsIgnoreCase(hub.getStatus())) {
                deletePost(post.getHubId(), postId);
                return;
            }
            List<ZentideInterestComment> comments = mapper.listCommentsForSnapshot(postId);
            List<String> topics = mapper.listPostTopicNames(postId);
            writeAtomically(fileFor(post.getHubId(), postId), render(post, hub, comments, topics));
            // Remove the pre-scene-layout file after the new snapshot is safely written.
            Files.deleteIfExists(legacyFileFor(postId));
        } catch (Exception e) {
            log.warn("Unable to refresh knowledge snapshot for post {}", postId, e);
        }
    }

    public void deletePost(Long postId) {
        if (postId == null) return;
        try {
            Files.deleteIfExists(legacyFileFor(postId));
            if (!Files.isDirectory(root)) return;
            try (Stream<Path> sceneDirectories = Files.list(root)) {
                sceneDirectories
                        .filter(Files::isDirectory)
                        .filter(path -> path.getFileName().toString().matches("scene-\\d+"))
                        .forEach(path -> deleteSnapshotAndEmptyDirectory(path, postId));
            }
        } catch (Exception e) {
            log.warn("Unable to delete knowledge snapshot for post {}", postId, e);
        }
    }

    public void deletePost(Long sceneId, Long postId) {
        if (sceneId == null || postId == null) {
            deletePost(postId);
            return;
        }
        try {
            Files.deleteIfExists(legacyFileFor(postId));
            deleteSnapshotAndEmptyDirectory(sceneDirectory(sceneId), postId);
        } catch (Exception e) {
            log.warn("Unable to delete knowledge snapshot for post {} in scene {}", postId, sceneId, e);
        }
    }

    /**
     * Periodic repair pass for writes that failed while the application was unavailable.
     * The database is authoritative: files for unpublished/deleted posts are removed,
     * and every currently published post is rebuilt from the latest database state.
     */
    @Scheduled(fixedDelayString = "${zentide.knowledge.rebuild-interval-ms:600000}")
    public void rebuildPublishedSnapshots() {
        try {
            List<Long> postIds = mapper.listPublishedPostIdsForSnapshot();
            Set<Long> expected = new HashSet<>(postIds == null ? List.of() : postIds);
            expected.remove(null);
            expected.forEach(this::refreshPost);
            removeOrphanSnapshots(expected);
        } catch (Exception e) {
            log.warn("Unable to rebuild knowledge snapshots", e);
        }
    }

    private void removeOrphanSnapshots(Set<Long> expectedPostIds) throws IOException {
        if (!Files.isDirectory(root)) return;
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches("post-\\d+\\.md"))
                    .forEach(path -> {
                        Long postId = postIdFromFile(path);
                        if (postId != null && !expectedPostIds.contains(postId)) {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                log.warn("Unable to delete orphan knowledge snapshot {}", path, e);
                            }
                        }
                    });
        }
        try (Stream<Path> directories = Files.walk(root)) {
            directories.filter(Files::isDirectory)
                    .filter(path -> !path.equals(root))
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try (Stream<Path> entries = Files.list(path)) {
                            if (entries.findAny().isEmpty()) Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("Unable to remove empty knowledge snapshot directory {}", path, e);
                        }
                    });
        }
    }

    private Long postIdFromFile(Path path) {
        String name = path.getFileName().toString();
        try {
            return Long.valueOf(name.substring("post-".length(), name.length() - ".md".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Path sceneDirectory(Long sceneId) {
        if (sceneId == null || sceneId <= 0) throw new IllegalArgumentException("Invalid scene id");
        Path directory = root.resolve("scene-" + sceneId).normalize();
        if (!directory.startsWith(root)) throw new IllegalArgumentException("Invalid knowledge snapshot path");
        return directory;
    }

    private Path fileFor(Long sceneId, Long postId) {
        if (postId == null || postId <= 0) throw new IllegalArgumentException("Invalid post id");
        Path file = sceneDirectory(sceneId).resolve("post-" + postId + ".md").normalize();
        if (!file.startsWith(root)) throw new IllegalArgumentException("Invalid knowledge snapshot path");
        return file;
    }

    private Path legacyFileFor(Long postId) {
        if (postId == null || postId <= 0) throw new IllegalArgumentException("Invalid post id");
        Path file = root.resolve("post-" + postId + ".md").normalize();
        if (!file.startsWith(root)) throw new IllegalArgumentException("Invalid knowledge snapshot path");
        return file;
    }

    private void deleteSnapshotAndEmptyDirectory(Path directory, Long postId) {
        try {
            Files.deleteIfExists(directory.resolve("post-" + postId + ".md"));
            if (Files.isDirectory(directory)) {
                try (Stream<Path> entries = Files.list(directory)) {
                    if (entries.findAny().isEmpty()) Files.deleteIfExists(directory);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to delete scene snapshot", e);
        }
    }

    private void writeAtomically(Path file, String content) throws IOException {
        Path directory = file.getParent();
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, ".post-", ".md.tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private String render(ZentideInterestPost post, ZentideInterestHub hub,
                          List<ZentideInterestComment> comments, List<String> topics) {
        StringBuilder out = new StringBuilder(2048);
        out.append("---\n");
        field(out, "post_id", post.getPostId());
        field(out, "scene_id", post.getHubId());
        field(out, "scene_name", hub == null ? post.getHubName() : hub.getName());
        field(out, "visibility", hub == null ? null : hub.getVisibility());
        field(out, "status", post.getStatus());
        field(out, "author_id", post.getAuthorId());
        field(out, "author_name", post.getAuthorLabel());
        field(out, "post_type", post.getPostTypeLabel() == null ? post.getPostType() : post.getPostTypeLabel());
        field(out, "created_at", post.getCreatedAt());
        field(out, "updated_at", post.getUpdatedAt());
        out.append("---\n\n");
        out.append("# ").append(text(post.getTitle(), "未命名帖子")).append("\n\n");
        out.append(text(post.getBody(), "")).append("\n\n");
        if (!topics.isEmpty()) out.append("**话题**：").append(String.join("、", topics)).append("\n\n");
        if (post.getEventTitle() != null && !post.getEventTitle().isBlank()) {
            out.append("**关联活动**：").append(post.getEventTitle()).append("\n\n");
        }
        out.append("## 评论区\n\n");
        if (comments == null || comments.isEmpty()) {
            out.append("暂无已发布评论。\n");
        } else {
            for (ZentideInterestComment comment : comments) {
                out.append("### 评论 ").append(comment.getCommentId()).append("\n\n");
                field(out, "comment_id", comment.getCommentId());
                field(out, "author_id", comment.getAuthorId());
                field(out, "author_name", comment.getAuthorLabel());
                field(out, "parent_comment_id", comment.getParentCommentId());
                field(out, "created_at", comment.getCreatedAt());
                out.append("\n").append(text(comment.getBody(), "")).append("\n\n");
            }
        }
        return out.toString();
    }

    private void field(StringBuilder out, String key, Object value) {
        if (value != null) out.append(key).append(": ").append(value).append("\n");
    }

    private String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
