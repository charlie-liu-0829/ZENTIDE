package com.zentide.entity.po;

import lombok.Data;
import java.util.Date;

@Data
public class ZentideCommunityUserProfile {
    private String userId;
    private String nickName;
    private String avatar;
    private String handle;
    private String bio;
    private String coverUrl;
    private String location;
    private String websiteUrl;
    private Date joinTime;
    private Integer postsCount;
    private Integer followersCount;
    private Integer followingCount;
    private Boolean followedByMe;
}
