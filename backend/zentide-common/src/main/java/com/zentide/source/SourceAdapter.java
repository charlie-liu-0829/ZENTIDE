package com.zentide.source;

import com.zentide.entity.po.ZentideSource;

import java.util.List;

public interface SourceAdapter {
    String sourceType();

    List<NormalizedDocument> parse(ZentideSource source, SourceFetchResult result);
}
