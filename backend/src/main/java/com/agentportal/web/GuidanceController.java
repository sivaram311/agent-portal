package com.agentportal.web;

import com.agentportal.domain.GuidanceKind;
import com.agentportal.dto.*;
import com.agentportal.service.GuidanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/guidance")
public class GuidanceController {

    private final GuidanceService guidanceService;

    public GuidanceController(GuidanceService guidanceService) {
        this.guidanceService = guidanceService;
    }

    @GetMapping("/packs")
    public List<GuidancePackDto> listPacks(@RequestParam(value = "kind", required = false) GuidanceKind kind) {
        return guidanceService.listPacks(kind);
    }

    @PostMapping("/packs")
    @ResponseStatus(HttpStatus.CREATED)
    public GuidancePackDto createPack(@Valid @RequestBody CreateGuidancePackRequest request) {
        return guidanceService.createPack(request);
    }

    @PatchMapping("/packs/{id}")
    public GuidancePackDto updatePack(@PathVariable UUID id, @RequestBody UpdateGuidancePackRequest request) {
        return guidanceService.updatePack(id, request);
    }

    @DeleteMapping("/packs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePack(@PathVariable UUID id) {
        guidanceService.deletePack(id);
    }

    @GetMapping("/defaults")
    public List<GuidancePackDto> getDefaults() {
        return guidanceService.getDefaults();
    }

    @PutMapping("/defaults")
    public List<GuidancePackDto> putDefaults(@Valid @RequestBody GuidanceDefaultsRequest request) {
        return guidanceService.putDefaults(request);
    }

    @GetMapping("/templates")
    public List<Map<String, Object>> templates() {
        return guidanceService.starterTemplates();
    }

    @PostMapping("/templates/install")
    @ResponseStatus(HttpStatus.CREATED)
    public List<GuidancePackDto> installTemplates() {
        return guidanceService.installStarterTemplates();
    }
}
