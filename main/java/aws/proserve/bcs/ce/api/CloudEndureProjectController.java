// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.api;

import aws.proserve.bcs.ce.CloudEndureAuditLog;
import aws.proserve.bcs.ce.CloudEndureMachine;
import aws.proserve.bcs.ce.dto.ManageCloudEndureMachinesRequest;
import aws.proserve.bcs.ce.service.CloudEndureBlueprintService;
import aws.proserve.bcs.ce.service.CloudEndureProjectService;
import aws.proserve.bcs.ce.service.CloudEndureStateMachineService;
import aws.proserve.bcs.ce.service.ProjectService;
import aws.proserve.bcs.dr.dto.Response;
import aws.proserve.bcs.dr.project.ProjectFinder;
import aws.proserve.bcs.dr.project.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @apiNote {@code projectId} refers to the DRPortal project ID.
 */
@RestController
@RequestMapping("/cloudendure/projects")
class CloudEndureProjectController {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ProjectFinder projectFinder;
    private final ProjectService projectService;

    private final CloudEndureBlueprintService cloudEndureBlueprintService;
    private final CloudEndureProjectService cloudEndureProjectService;
    private final CloudEndureStateMachineService cloudEndureStateMachineService;

    CloudEndureProjectController(
            ProjectFinder projectFinder,
            ProjectService projectService,

            CloudEndureBlueprintService cloudEndureBlueprintService,
            CloudEndureProjectService cloudEndureProjectService,
            CloudEndureStateMachineService cloudEndureStateMachineService) {
        this.projectFinder = projectFinder;
        this.projectService = projectService;

        this.cloudEndureStateMachineService = cloudEndureStateMachineService;
        this.cloudEndureProjectService = cloudEndureProjectService;
        this.cloudEndureBlueprintService = cloudEndureBlueprintService;
    }

    @PutMapping("/{projectId}/{terminate}")
    ResponseEntity<Response> prepareCutback(
            @PathVariable String projectId,
            @PathVariable boolean terminate) {
        cloudEndureStateMachineService.cutback(projectFinder.findOne(projectId), terminate);
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }

    @GetMapping("/{projectId}/auditLogs/{side}")
    ResponseEntity<CloudEndureAuditLog[]> findAllAuditLogs(
            @PathVariable String projectId,
            @PathVariable Side side) {
        final var project = projectFinder.findOne(projectId);
        final var item = project.getCloudEndureProject().getItem(side);
        if (item == null) {
            return ResponseEntity.ok(new CloudEndureAuditLog[0]);
        }

        return ResponseEntity.ok(projectService.findAllAuditLogs(item.getId()));
    }

    @GetMapping("/{projectId}/machines/{side}")
    ResponseEntity<CloudEndureMachine[]> getMachines(
            @PathVariable String projectId,
            @PathVariable Side side) {
        final var project = projectFinder.findOne(projectId);
        final var item = project.getCloudEndureProject().getItem(side);
        if (item == null) {
            return ResponseEntity.ok(new CloudEndureMachine[0]);
        }

        return ResponseEntity.ok(cloudEndureProjectService.getMachines(project.getRegion(side).getName(), item));
    }

    @PutMapping("/{projectId}/machines/agent")
    ResponseEntity<Response> installAgent(
            @PathVariable String projectId,
            @RequestBody ManageCloudEndureMachinesRequest request) {
        cloudEndureProjectService.installAgent(projectFinder.findOne(projectId), request);
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }

    @PutMapping("/{projectId}/machines/blueprint")
    ResponseEntity<Response> configureBlueprint(
            @PathVariable String projectId,
            @RequestBody ManageCloudEndureMachinesRequest request) {
        final var project = projectFinder.findOne(projectId);
        for (var entry : request.getMachineIdMap().entrySet()) {
            cloudEndureBlueprintService.configure(project, request.getSide(), entry.getKey(), entry.getValue());
        }
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }

    @PostMapping("/{projectId}/machines")
    ResponseEntity<Response> launchMachines(
            @PathVariable String projectId,
            @RequestBody ManageCloudEndureMachinesRequest request) {
        cloudEndureProjectService.launchMachines(projectFinder.findOne(projectId), request);
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }
}
