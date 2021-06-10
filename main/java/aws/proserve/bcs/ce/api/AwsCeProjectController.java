// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.api;

import aws.proserve.bcs.ce.service.ProjectService;
import aws.proserve.bcs.ce.service.RegionService;
import aws.proserve.bcs.dr.ce.CloudEndureItem;
import aws.proserve.bcs.dr.project.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/awsce/projects")
public class AwsCeProjectController {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ProjectService projectService;
    private final RegionService regionService;

    AwsCeProjectController(ProjectService projectService, RegionService regionService) {
        this.projectService = projectService;
        this.regionService = regionService;
    }

    @GetMapping
    ResponseEntity<CloudEndureItem[]> findAll() {
        return ResponseEntity.ok(projectService.findAll());
    }

    /**
     * Find the target region throw this path: CE project -> replication config -> region.
     */
    @GetMapping("/{projectId}/configs/{configId}/targetRegion")
    ResponseEntity<Region> findTargetRegion(
            @PathVariable String projectId,
            @PathVariable String configId) {
        final var project = projectService.findOne(projectId);
        if (project == null || project.getReplicationConfiguration() == null) {
            log.warn("Unable to find CE project [{}]", projectId);
            return ResponseEntity.notFound().build();
        }
        final var configs = projectService.findAllReplicationConfigurations(project.getId());
        final var config = Arrays.stream(configs).filter(c -> c.getId().equals(configId)).findFirst();
        if (config.isEmpty()) {
            log.warn("Unable to find replication configuration [{}]", configId);
            return ResponseEntity.notFound().build();
        }
        if (project.getCloudCredentialsIDs().length == 0) {
            log.warn("CE project [{}] does not have cloud credentials", projectId);
            return ResponseEntity.notFound().build();
        }
        final var region = regionService.find(project.getCloudCredentialsIDs()[0], config.get().getRegion());
        return region.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(region.get());
    }
}
