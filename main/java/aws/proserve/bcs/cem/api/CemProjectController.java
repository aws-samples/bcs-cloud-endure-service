// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.cem.api;

import aws.proserve.bcs.ce.CloudEndureAuditLog;
import aws.proserve.bcs.ce.CloudEndureMachine;
import aws.proserve.bcs.ce.service.CloudEndureBlueprintService;
import aws.proserve.bcs.ce.service.CloudEndureProjectService;
import aws.proserve.bcs.ce.service.ProjectService;
import aws.proserve.bcs.cem.dto.ConfigureBlueprintRequest;
import aws.proserve.bcs.cem.dto.SelectSecurityGroupRequest;
import aws.proserve.bcs.cem.dto.SetBlueprintRequest;
import aws.proserve.bcs.cem.service.CemService;
import aws.proserve.bcs.cem.CemBlueprint;
import aws.proserve.bcs.dr.dto.Response;
import aws.proserve.bcs.dr.project.ProjectFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @apiNote {@code projectId} refers to the DRPortal project ID.
 */
@RestController
@RequestMapping("/cem/projects")
class CemProjectController {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ProjectFinder projectFinder;
    private final ProjectService projectService;
    private final CemService cemService;

    private final CloudEndureBlueprintService cloudEndureBlueprintService;
    private final CloudEndureProjectService cloudEndureProjectService;

    CemProjectController(
            ProjectFinder projectFinder,
            ProjectService projectService,
            CemService cemService,
            CloudEndureBlueprintService cloudEndureBlueprintService,
            CloudEndureProjectService cloudEndureProjectService) {
        this.projectFinder = projectFinder;
        this.projectService = projectService;
        this.cemService = cemService;
        this.cloudEndureProjectService = cloudEndureProjectService;
        this.cloudEndureBlueprintService = cloudEndureBlueprintService;
    }

    @GetMapping("/{projectId}/blueprints")
    ResponseEntity<CemBlueprint[]> getBlueprints(@PathVariable String projectId) {
        return ResponseEntity.ok(cemService.getBlueprints(projectFinder.findOne(projectId)).toArray(new CemBlueprint[0]));
    }

    /**
     * Read machine information from CloudEndure and populate the CEM blueprint table. New machines will be added.
     *
     * @param projectId DRP CEM project ID.
     */
    @PutMapping("/{projectId}/blueprints")
    ResponseEntity<Response> loadBlueprints(@PathVariable String projectId) {
        final var project = projectFinder.findOne(projectId);
        cemService.loadBlueprints(project);
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }

    @PutMapping("/{projectId}/setBlueprint")
    ResponseEntity<Response> setBlueprint(
            @PathVariable String projectId,
            @RequestBody SetBlueprintRequest request) {
        final var project = projectFinder.findOne(projectId);
        cemService.setBlueprint(project, request);
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }

    @PutMapping("/{projectId}/selectSecurityGroup")
    ResponseEntity<Response> selectSecurityGroup(
            @PathVariable String projectId,
            @RequestBody SelectSecurityGroupRequest request) {
        final var project = projectFinder.findOne(projectId);
        cemService.selectSecurityGroup(project, request);
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }

    @PutMapping("/{projectId}/configureBlueprint")
    ResponseEntity<Response> configureBlueprint(
            @PathVariable String projectId,
            @RequestBody ConfigureBlueprintRequest request) {
        final var project = projectFinder.findOne(projectId);
        cemService.configureBlueprint(project, request);
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }

    @GetMapping("/{projectId}/auditLogs")
    ResponseEntity<CloudEndureAuditLog[]> getAuditLogs(@PathVariable String projectId) {
        final var project = projectFinder.findOne(projectId);
        final var ceProject = project.getCemProject().getProject();
        return ResponseEntity.ok(projectService.findAllAuditLogs(ceProject.getId()));
    }

    @GetMapping("/{projectId}/machines")
    ResponseEntity<CloudEndureMachine[]> getMachines(@PathVariable String projectId) {
        final var project = projectFinder.findOne(projectId);
        final var ceProject = project.getCemProject().getProject();
        return ResponseEntity.ok(cloudEndureProjectService.getMachines(project.getTargetRegion().getName(), ceProject));
    }
}
