# Agent Portal REST Actions

Each UI action below maps to one OpenAPI `operationId` and backend route. All backend paths include the `/api` prefix.

| UI action | operationId | Method | Path |
|---|---|---:|---|
| Health check | `health` | GET | `/api/health` |
| Auth configuration | `authConfig` | GET | `/api/auth/config` |
| List session presets | `listPresets` | GET | `/api/presets` |
| List sessions | `listSessions` | GET | `/api/sessions` |
| Create session | `createSession` | POST | `/api/sessions` |
| Get session | `getSession` | GET | `/api/sessions/{id}` |
| List messages | `listMessages` | GET | `/api/sessions/{id}/messages` |
| List tools | `listTools` | GET | `/api/sessions/{id}/tools` |
| List permissions | `listPermissions` | GET | `/api/sessions/{id}/permissions` |
| Send prompt | `sendPrompt` | POST | `/api/sessions/{id}/prompt` |
| Cancel run | `cancelRun` | POST | `/api/sessions/{id}/cancel` |
| Resolve permission | `resolvePermission` | POST | `/api/sessions/{id}/permissions/{permissionId}` |
| Abandon sub-agent | `abandonSubagent` | POST | `/api/sessions/{id}/subagents/{subId}/abandon` |
| Archive session | `archiveSession` | POST | `/api/sessions/{id}/archive` |
| Unarchive session | `unarchiveSession` | POST | `/api/sessions/{id}/unarchive` |
| List files | `listFiles` | GET | `/api/sessions/{id}/files` |
| Read file | `readFile` | GET | `/api/sessions/{id}/files/content` |
| List changes | `listChanges` | GET | `/api/sessions/{id}/changes` |
| Diff change | `diffChange` | GET | `/api/sessions/{id}/changes/diff` |
| Accept change | `acceptChange` | POST | `/api/sessions/{id}/changes/accept` |
| Reject change | `rejectChange` | POST | `/api/sessions/{id}/changes/reject` |
| List events | `listEvents` | GET | `/api/sessions/{id}/events` |
| List collaborators | `listCollaborators` | GET | `/api/sessions/{id}/collaborators` |
| Add collaborator | `addCollaborator` | POST | `/api/sessions/{id}/collaborators` |
| Remove collaborator | `removeCollaborator` | DELETE | `/api/sessions/{id}/collaborators/{username}` |
| Get session guidance | `getSessionGuidance` | GET | `/api/sessions/{id}/guidance` |
| Put session guidance | `putSessionGuidance` | PUT | `/api/sessions/{id}/guidance` |
| List guidance packs | `listGuidancePacks` | GET | `/api/guidance/packs` |
| Create guidance pack | `createGuidancePack` | POST | `/api/guidance/packs` |
| Update guidance pack | `updateGuidancePack` | PATCH | `/api/guidance/packs/{id}` |
| Delete guidance pack | `deleteGuidancePack` | DELETE | `/api/guidance/packs/{id}` |
| Get guidance defaults | `getGuidanceDefaults` | GET | `/api/guidance/defaults` |
| Put guidance defaults | `putGuidanceDefaults` | PUT | `/api/guidance/defaults` |
| List guidance templates | `listGuidanceTemplates` | GET | `/api/guidance/templates` |
| Install guidance templates | `installGuidanceTemplates` | POST | `/api/guidance/templates/install` |
| List audit events | `listAudit` | GET | `/api/audit` |
| List port leases | `listPorts` | GET | `/api/platform/ports` |
| Claim port | `claimPort` | POST | `/api/platform/ports/claim` |
| Release port | `releasePort` | POST | `/api/platform/ports/{port}/release` |
| List platform apps | `listApps` | GET | `/api/platform/apps` |
| Platform app home | `platformHome` | GET | `/api/platform/home` |
| List EM tasks | `listTasks` | GET | `/api/platform/tasks` |
| Create EM task | `createTask` | POST | `/api/platform/tasks` |
| Link task session | `linkTaskSession` | POST | `/api/platform/tasks/{id}/session` |
| List roles | `listRoles` | GET | `/api/platform/roles` |
| List shared memory | `listMemory` | GET | `/api/platform/memory` |
| Upsert shared memory | `upsertMemory` | POST | `/api/platform/memory` |
| List agent messages | `listMessages` | GET | `/api/platform/messages` |
| Send agent message | `createMessage` | POST | `/api/platform/messages` |
| List pipelines | `listPipelines` | GET | `/api/platform/pipelines` |
| Run pipeline | `runPipeline` | POST | `/api/platform/pipelines/{id}/run` |
| Org dashboard | `orgDashboard` | GET | `/api/platform/org` |
| Role detail / ACL | `getRole` | GET | `/api/platform/roles/{id}` |
| Swarm tick | `swarmTick` | POST | `/api/platform/swarm/tick` |
