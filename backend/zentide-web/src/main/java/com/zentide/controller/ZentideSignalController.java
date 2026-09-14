package com.zentide.controller;

import com.zentide.controller.ABaseController;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.service.ZentideSignalService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/zentide/v1/signals")
@Validated
public class ZentideSignalController extends ABaseController {
    private final ZentideSignalService service;

    public ZentideSignalController(ZentideSignalService service) {
        this.service = service;
    }

    @PostMapping("/list")
    public ResponseVO<?> list(@RequestParam(required = false) @Min(1) @Max(100) Integer limit) {
        return getSuccessResponseVO(service.listActive(limit));
    }
}
