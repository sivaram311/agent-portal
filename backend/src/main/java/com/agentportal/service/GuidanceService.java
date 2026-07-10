package com.agentportal.service;

import com.agentportal.domain.*;
import com.agentportal.dto.*;
import com.agentportal.repo.GuidancePackRepository;
import com.agentportal.repo.SessionGuidanceRepository;
import com.agentportal.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class GuidanceService {

    private final GuidancePackRepository packRepository;
    private final SessionGuidanceRepository sessionGuidanceRepository;
    private final GuidanceMaterializer materializer;

    public GuidanceService(
            GuidancePackRepository packRepository,
            SessionGuidanceRepository sessionGuidanceRepository,
            GuidanceMaterializer materializer
    ) {
        this.packRepository = packRepository;
        this.sessionGuidanceRepository = sessionGuidanceRepository;
        this.materializer = materializer;
    }

    public List<GuidancePackDto> listPacks(GuidanceKind kind) {
        String user = CurrentUser.usernameOrAnonymous();
        List<GuidancePack> packs = kind == null
                ? packRepository.findByOwnerUsernameOrderByKindAscTitleAsc(user)
                : packRepository.findByOwnerUsernameAndKindOrderByTitleAsc(user, kind);
        return packs.stream().map(GuidancePackDto::from).toList();
    }

    @Transactional
    public GuidancePackDto createPack(CreateGuidancePackRequest request) {
        String user = CurrentUser.usernameOrAnonymous();
        String slug = resolveSlug(user, request.slug(), request.title());
        GuidancePack pack = new GuidancePack();
        pack.setOwnerUsername(user);
        pack.setKind(request.kind());
        pack.setSlug(slug);
        pack.setTitle(request.title().trim());
        pack.setDescription(blankToNull(request.description()));
        pack.setBodyMarkdown(request.bodyMarkdown());
        pack.setGlobs(blankToNull(request.globs()));
        pack.setAlwaysApply(request.alwaysApply() == null || request.alwaysApply());
        pack.setEnabledByDefault(request.enabledByDefault() == null || request.enabledByDefault());
        return GuidancePackDto.from(packRepository.save(pack));
    }

    @Transactional
    public GuidancePackDto updatePack(UUID id, UpdateGuidancePackRequest request) {
        GuidancePack pack = requireOwnedPack(id);
        if (request.title() != null && !request.title().isBlank()) {
            pack.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            pack.setDescription(blankToNull(request.description()));
        }
        if (request.bodyMarkdown() != null && !request.bodyMarkdown().isBlank()) {
            pack.setBodyMarkdown(request.bodyMarkdown());
        }
        if (request.globs() != null) {
            pack.setGlobs(blankToNull(request.globs()));
        }
        if (request.alwaysApply() != null) {
            pack.setAlwaysApply(request.alwaysApply());
        }
        if (request.enabledByDefault() != null) {
            pack.setEnabledByDefault(request.enabledByDefault());
        }
        if (request.slug() != null && !request.slug().isBlank()) {
            String slug = slugify(request.slug());
            packRepository.findByOwnerUsernameAndSlug(pack.getOwnerUsername(), slug)
                    .filter(other -> !other.getId().equals(pack.getId()))
                    .ifPresent(other -> {
                        throw new IllegalArgumentException("Slug already in use: " + slug);
                    });
            pack.setSlug(slug);
        }
        return GuidancePackDto.from(packRepository.save(pack));
    }

    @Transactional
    public void deletePack(UUID id) {
        GuidancePack pack = requireOwnedPack(id);
        packRepository.delete(pack);
    }

    public List<GuidancePackDto> getDefaults() {
        String user = CurrentUser.usernameOrAnonymous();
        return packRepository.findByOwnerUsernameAndEnabledByDefaultTrueOrderByKindAscTitleAsc(user)
                .stream()
                .map(GuidancePackDto::from)
                .toList();
    }

    @Transactional
    public List<GuidancePackDto> putDefaults(GuidanceDefaultsRequest request) {
        String user = CurrentUser.usernameOrAnonymous();
        Set<UUID> enabled = new HashSet<>(request.enabledPackIds() == null ? List.of() : request.enabledPackIds());
        List<GuidancePack> packs = packRepository.findByOwnerUsernameOrderByKindAscTitleAsc(user);
        for (GuidancePack pack : packs) {
            pack.setEnabledByDefault(enabled.contains(pack.getId()));
        }
        packRepository.saveAll(packs);
        return getDefaults();
    }

    @Transactional
    public void seedDefaultsForNewSession(UUID sessionId, boolean useDefaults) {
        if (!useDefaults) {
            return;
        }
        if (sessionGuidanceRepository.countBySessionId(sessionId) > 0) {
            return;
        }
        String user = CurrentUser.usernameOrAnonymous();
        List<GuidancePack> defaults = packRepository
                .findByOwnerUsernameAndEnabledByDefaultTrueOrderByKindAscTitleAsc(user);
        int order = 0;
        for (GuidancePack pack : defaults) {
            SessionGuidance row = new SessionGuidance();
            row.setSessionId(sessionId);
            row.setPackId(pack.getId());
            row.setKind(pack.getKind());
            row.setTitle(pack.getTitle());
            row.setEnabled(true);
            row.setSortOrder(order++);
            sessionGuidanceRepository.save(row);
        }
    }

    public SessionGuidanceDto getSessionGuidance(UUID sessionId) {
        List<SessionGuidanceItemDto> items = buildItems(sessionId);
        List<SessionGuidanceItemDto> effective = items.stream().filter(SessionGuidanceItemDto::enabled).toList();
        return new SessionGuidanceDto(items, effective);
    }

    @Transactional
    public SessionGuidanceDto putSessionGuidance(UUID sessionId, UpdateSessionGuidanceRequest request, PathAwareSession session) {
        sessionGuidanceRepository.deleteBySessionId(sessionId);
        List<SessionGuidanceEntryRequest> entries = request.items() == null ? List.of() : request.items();
        int order = 0;
        String user = CurrentUser.usernameOrAnonymous();
        for (SessionGuidanceEntryRequest entry : entries) {
            SessionGuidance row = new SessionGuidance();
            row.setSessionId(sessionId);
            row.setEnabled(entry.enabled() == null || entry.enabled());
            row.setSortOrder(entry.sortOrder() != null ? entry.sortOrder() : order);
            if (entry.packId() != null) {
                GuidancePack pack = packRepository.findByIdAndOwnerUsername(entry.packId(), user)
                        .or(() -> packRepository.findById(entry.packId()))
                        .orElseThrow(() -> new NoSuchElementException("Pack not found: " + entry.packId()));
                row.setPackId(pack.getId());
                row.setKind(pack.getKind());
                row.setTitle(pack.getTitle());
            } else {
                String body = entry.sessionBody();
                if (body == null || body.isBlank()) {
                    throw new IllegalArgumentException("sessionBody is required for session-only guidance");
                }
                row.setPackId(null);
                row.setKind(entry.kind() != null ? entry.kind() : GuidanceKind.RULE);
                row.setTitle(entry.title() != null && !entry.title().isBlank() ? entry.title().trim() : "Session note");
                row.setSessionBody(body);
            }
            sessionGuidanceRepository.save(row);
            order++;
        }
        materializer.materialize(session.workspacePath(), resolveEffectivePacks(sessionId));
        return getSessionGuidance(sessionId);
    }

    public List<ResolvedGuidance> resolveEffectivePacks(UUID sessionId) {
        List<SessionGuidance> rows = sessionGuidanceRepository.findBySessionIdOrderBySortOrderAscCreatedAtAsc(sessionId);
        List<ResolvedGuidance> out = new ArrayList<>();
        for (SessionGuidance row : rows) {
            if (!row.isEnabled()) {
                continue;
            }
            if (row.getPackId() != null) {
                Optional<GuidancePack> pack = packRepository.findById(row.getPackId());
                if (pack.isEmpty()) {
                    continue;
                }
                GuidancePack p = pack.get();
                out.add(new ResolvedGuidance(
                        p.getId(),
                        p.getKind(),
                        p.getSlug(),
                        p.getTitle(),
                        p.getDescription(),
                        p.getBodyMarkdown(),
                        p.getGlobs(),
                        p.isAlwaysApply()
                ));
            } else if (row.getSessionBody() != null && !row.getSessionBody().isBlank()) {
                String slug = "session-" + row.getId().toString().substring(0, 8);
                out.add(new ResolvedGuidance(
                        null,
                        row.getKind(),
                        slug,
                        row.getTitle() != null ? row.getTitle() : "Session note",
                        null,
                        row.getSessionBody(),
                        null,
                        true
                ));
            }
        }
        return out;
    }

    public void materializeForSession(String workspacePath, UUID sessionId) {
        materializer.materialize(workspacePath, resolveEffectivePacks(sessionId));
    }

    public String buildPromptPrefix(UUID sessionId) {
        return materializer.buildPromptPrefix(resolveEffectivePacks(sessionId));
    }

    public List<Map<String, Object>> starterTemplates() {
        return List.of(
                template("RULE", "safe-shell", "Safe shell",
                        "Prefer non-destructive commands; ask before rm/format/push --force.",
                        """
                                - Prefer read-only inspection before edits.
                                - Never run destructive commands (rm -rf, format, drop database, git push --force) unless the user explicitly asks.
                                - Quote paths with spaces; avoid piping secrets to logs.
                                """, true),
                template("RULE", "concise-replies", "Concise replies",
                        "Keep answers short and actionable.",
                        """
                                - Lead with the answer or change.
                                - Use short bullets; avoid long preambles.
                                - Do not restate the task unless clarifying a blocker.
                                """, true),
                template("SKILL", "angular-style", "Angular style",
                        "Follow Angular 19 standalone patterns used in this portal.",
                        """
                                When editing Angular code:
                                - Prefer standalone components and existing design tokens.
                                - Match nearby SCSS patterns; keep mobile (360px) usable.
                                - Do not add NgModules for new UI.
                                """, false)
        );
    }

    @Transactional
    public List<GuidancePackDto> installStarterTemplates() {
        List<GuidancePackDto> created = new ArrayList<>();
        for (Map<String, Object> t : starterTemplates()) {
            String slug = String.valueOf(t.get("slug"));
            String user = CurrentUser.usernameOrAnonymous();
            if (packRepository.findByOwnerUsernameAndSlug(user, slug).isPresent()) {
                continue;
            }
            CreateGuidancePackRequest req = new CreateGuidancePackRequest(
                    GuidanceKind.valueOf(String.valueOf(t.get("kind"))),
                    String.valueOf(t.get("title")),
                    String.valueOf(t.get("description")),
                    String.valueOf(t.get("bodyMarkdown")),
                    null,
                    Boolean.TRUE.equals(t.get("alwaysApply")),
                    true,
                    slug
            );
            created.add(createPack(req));
        }
        return created;
    }

    private static Map<String, Object> template(
            String kind, String slug, String title, String description, String body, boolean alwaysApply
    ) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kind", kind);
        m.put("slug", slug);
        m.put("title", title);
        m.put("description", description);
        m.put("bodyMarkdown", body.stripIndent().trim());
        m.put("alwaysApply", alwaysApply);
        return m;
    }

    private List<SessionGuidanceItemDto> buildItems(UUID sessionId) {
        List<SessionGuidance> rows = sessionGuidanceRepository.findBySessionIdOrderBySortOrderAscCreatedAtAsc(sessionId);
        List<SessionGuidanceItemDto> items = new ArrayList<>();
        for (SessionGuidance row : rows) {
            String body;
            String title = row.getTitle();
            GuidanceKind kind = row.getKind();
            boolean sessionOnly = row.getPackId() == null;
            if (row.getPackId() != null) {
                Optional<GuidancePack> pack = packRepository.findById(row.getPackId());
                if (pack.isPresent()) {
                    body = pack.get().getBodyMarkdown();
                    title = pack.get().getTitle();
                    kind = pack.get().getKind();
                } else {
                    body = "";
                }
            } else {
                body = row.getSessionBody() != null ? row.getSessionBody() : "";
            }
            items.add(new SessionGuidanceItemDto(
                    row.getId(),
                    row.getPackId(),
                    kind,
                    title,
                    body,
                    row.isEnabled(),
                    row.getSortOrder(),
                    sessionOnly
            ));
        }
        return items;
    }

    private GuidancePack requireOwnedPack(UUID id) {
        String user = CurrentUser.usernameOrAnonymous();
        return packRepository.findByIdAndOwnerUsername(id, user)
                .orElseThrow(() -> new NoSuchElementException("Guidance pack not found: " + id));
    }

    private String resolveSlug(String user, String requested, String title) {
        String base = requested != null && !requested.isBlank() ? slugify(requested) : slugify(title);
        if (base.isBlank()) {
            base = "pack";
        }
        String candidate = base;
        int i = 2;
        while (packRepository.findByOwnerUsernameAndSlug(user, candidate).isPresent()) {
            candidate = base + "-" + i++;
        }
        return candidate;
    }

    static String slugify(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (s.length() > 48) {
            s = s.substring(0, 48).replaceAll("-+$", "");
        }
        return s;
    }

    private static String blankToNull(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        return v.trim();
    }

    /** Minimal session path holder to avoid circular deps with SessionService. */
    public record PathAwareSession(String workspacePath) {
    }

    public record ResolvedGuidance(
            UUID packId,
            GuidanceKind kind,
            String slug,
            String title,
            String description,
            String bodyMarkdown,
            String globs,
            boolean alwaysApply
    ) {
    }
}
