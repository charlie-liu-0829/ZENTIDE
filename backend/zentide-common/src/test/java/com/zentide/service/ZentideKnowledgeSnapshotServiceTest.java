package com.zentide.service;

import com.zentide.entity.po.ZentideInterestHub;
import com.zentide.entity.po.ZentideInterestPost;
import com.zentide.mapper.ZentideInterestMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZentideKnowledgeSnapshotServiceTest {
    @TempDir
    Path snapshotRoot;

    @Test
    void storesAndDeletesPostInsideItsSceneDirectory() throws Exception {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        ZentideInterestPost post = new ZentideInterestPost();
        post.setPostId(12L);
        post.setHubId(5L);
        post.setStatus("PUBLISHED");
        post.setTitle("现场帖子");
        post.setBody("<p>正文</p>");
        ZentideInterestHub hub = new ZentideInterestHub();
        hub.setHubId(5L);
        hub.setName("现场音乐");
        hub.setVisibility("PUBLIC");
        hub.setStatus("ACTIVE");

        when(mapper.findPost(12L, null)).thenReturn(post);
        when(mapper.findHub(5L)).thenReturn(hub);
        when(mapper.listCommentsForSnapshot(12L)).thenReturn(List.of());
        when(mapper.listPostTopicNames(12L)).thenReturn(List.of());

        Path legacyFile = snapshotRoot.resolve("post-12.md");
        Files.writeString(legacyFile, "legacy");
        ZentideKnowledgeSnapshotService service =
                new ZentideKnowledgeSnapshotService(mapper, snapshotRoot.toString());

        service.refreshPost(12L);

        Path sceneFile = snapshotRoot.resolve("scene-5/post-12.md");
        assertTrue(Files.isRegularFile(sceneFile));
        assertTrue(Files.readString(sceneFile).contains("scene_id: 5"));
        assertFalse(Files.exists(legacyFile));

        service.deletePost(5L, 12L);

        assertFalse(Files.exists(sceneFile));
        assertFalse(Files.exists(snapshotRoot.resolve("scene-5")));
    }

    @Test
    void rebuildRemovesSnapshotsMissingFromPublishedDatabaseSet() throws Exception {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        Path orphan = snapshotRoot.resolve("scene-9/post-99.md");
        Files.createDirectories(orphan.getParent());
        Files.writeString(orphan, "orphan");
        when(mapper.listPublishedPostIdsForSnapshot()).thenReturn(List.of());

        new ZentideKnowledgeSnapshotService(mapper, snapshotRoot.toString()).rebuildPublishedSnapshots();

        assertFalse(Files.exists(orphan));
        assertFalse(Files.exists(orphan.getParent()));
    }

    @Test
    void inactiveHubDoesNotKeepPostSnapshot() throws Exception {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        ZentideInterestPost post = new ZentideInterestPost();
        post.setPostId(12L);
        post.setHubId(5L);
        post.setStatus("PUBLISHED");
        ZentideInterestHub hub = new ZentideInterestHub();
        hub.setHubId(5L);
        hub.setStatus("HIDDEN");
        Path sceneFile = snapshotRoot.resolve("scene-5/post-12.md");
        Files.createDirectories(sceneFile.getParent());
        Files.writeString(sceneFile, "stale");
        when(mapper.findPost(12L, null)).thenReturn(post);
        when(mapper.findHub(5L)).thenReturn(hub);

        new ZentideKnowledgeSnapshotService(mapper, snapshotRoot.toString()).refreshPost(12L);

        assertFalse(Files.exists(sceneFile));
    }
}
