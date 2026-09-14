package com.zentide.source;

import com.zentide.entity.po.ZentideSource;

public interface SourceFetcher {
    SourceFetchResult fetch(ZentideSource source);
}
