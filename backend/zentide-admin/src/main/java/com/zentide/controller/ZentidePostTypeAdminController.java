package com.zentide.controller;

import com.zentide.controller.ABaseController;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.service.ZentideInterestCommunityService;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/zentide/v1/community/post-types")
@Validated
public class ZentidePostTypeAdminController extends ABaseController {
    private final ZentideInterestCommunityService service;

    public ZentidePostTypeAdminController(ZentideInterestCommunityService service) {
        this.service = service;
    }

    @PostMapping("/list")
    public ResponseVO<?> list() {
        getAdminAccount();
        return getSuccessResponseVO(service.systemPostTypes());
    }

    @PostMapping("/create")
    public ResponseVO<?> create(@RequestParam String displayName, @RequestParam(required = false) String description) {
        return getSuccessResponseVO(service.createSystemPostType(getAdminAccount(), displayName, description));
    }

    @PostMapping("/{postTypeId}/update")
    public ResponseVO<?> update(@PathVariable @Positive Long postTypeId,
                                @RequestParam String displayName,
                                @RequestParam(required = false) String description,
                                @RequestParam(defaultValue = "true") boolean active,
                                @RequestParam(required = false) Integer sortOrder) {
        getAdminAccount();
        return getSuccessResponseVO(service.updateSystemPostType(postTypeId, displayName, description, active, sortOrder));
    }

    @PostMapping("/{postTypeId}/delete")
    public ResponseVO<?> delete(@PathVariable @Positive Long postTypeId) {
        getAdminAccount();
        return getSuccessResponseVO(service.deleteSystemPostType(postTypeId));
    }
}
