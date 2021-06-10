// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.service;

import aws.proserve.bcs.ce.CloudEndureBlueprint;
import aws.proserve.bcs.ce.CloudEndureLaunchResult;
import aws.proserve.bcs.ce.CloudEndureMachine;
import aws.proserve.bcs.ce.CloudEndureTag;
import aws.proserve.bcs.ce.ImmutableCloudEndureMachine;
import aws.proserve.bcs.ce.dto.ManageCloudEndureMachinesRequest;
import aws.proserve.bcs.ce.exception.CloudEndureException;
import aws.proserve.bcs.dr.ce.CloudEndureConstants;
import aws.proserve.bcs.dr.ce.CloudEndureItem;
import aws.proserve.bcs.dr.exception.PortalException;
import aws.proserve.bcs.dr.project.Project;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named
public class CloudEndureProjectService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ObjectMapper mapper;
    private final AWSLambda lambda;

    private final BlueprintService blueprintService;
    private final MachineService machineService;

    CloudEndureProjectService(
            ObjectMapper mapper,
            AWSLambda lambda,
            BlueprintService blueprintService,
            MachineService machineService) {
        this.mapper = mapper;
        this.lambda = lambda;
        this.blueprintService = blueprintService;
        this.machineService = machineService;
    }

    String checkCutbackPrecondition(Project project) {
        final var item = Objects.requireNonNull(project.getCloudEndureProject().getCutover());
        final var machines = machineService.findAll(item.getId());
        final var blueprintMap = Stream.of(blueprintService
                .findAll(item.getId()))
                .collect(Collectors.toMap(CloudEndureBlueprint::getMachineId, i -> i));

        for (var machine : machines) {
            if (Stream.of(blueprintMap.get(machine.getId()).getTags())
                    .map(CloudEndureTag::getKey)
                    .noneMatch(CloudEndureConstants.TAG_BLUEPRINT::equals)) {
                return machine.getId() + " 启动蓝图没有配置";
            }

            final var info = machine.getReplicationInfo();
            if (1. * info.getReplicatedStorageBytes() / info.getTotalStorageBytes() < .9) {
                return machine.getId() + " 数据复制尚未完成";
            }

            if (info.getLastConsistencyDateTime() == null) {
                return machine.getId() + " 还没有数据一致时间";
            }
        }
        return null;
    }

    public CloudEndureMachine[] getMachines(String region, CloudEndureItem project) {
        final var itemId = project.getId();
        final var machines = machineService.findAll(itemId);
        final var blueprintMap = Stream.of(blueprintService
                .findAll(itemId))
                .collect(Collectors.toMap(CloudEndureBlueprint::getMachineId, i -> i));
        for (var i = 0; i < machines.length; i++) {
            final var machine = machines[i];
            machines[i] = ImmutableCloudEndureMachine.builder()
                    .from(machine)
                    .region(region)
                    .blueprintConfigured(
                            Stream.of(blueprintMap.get(machine.getId()).getTags())
                                    .map(CloudEndureTag::getKey)
                                    .anyMatch(CloudEndureConstants.TAG_BLUEPRINT::equals))
                    .build();
        }

        return machines;
    }

    private void addPeerRoute(Project project, Collection<String> instanceIds) {
        try {
            final var invoke = lambda.invoke(new InvokeRequest()
                    .withFunctionName("DRPCommonAddPeerRoute")
                    .withPayload(mapper.writeValueAsString(Map.of(
                            "sourceVpcId", project.getCloudEndureProject().getSourceVpcId(),
                            "sourceRegion", project.getSourceRegion().getName(),
                            "targetRegion", project.getTargetRegion().getName(),
                            "instanceIds", instanceIds,
                            "projectId", project.getId()))));
            final var output = StandardCharsets.UTF_8.decode(invoke.getPayload()).toString();
            final Map<String, Object> resultMap = mapper.readValue(output, Map.class);
            if (resultMap != null && resultMap.get("errorMessage") != null) {
                throw new PortalException(resultMap.get("errorMessage").toString());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to add peer route", e);
        }
    }

    public void installAgent(Project project, ManageCloudEndureMachinesRequest request) {
        if (!project.getCloudEndureProject().isPublicNetwork()) {
            log.info("Using private network, need to add routes to VPC peering.");
            addPeerRoute(project, request.getInstanceIds());
        }

        try {
            final var invoke = lambda.invoke(new InvokeRequest()
                    .withFunctionName("DRPCloudEndureInstallAgent")
                    .withPayload(mapper.writeValueAsString(Map.of(
                            "side", request.getSide(),
                            "projectId", project.getId(),
                            "instanceIds", request.getInstanceIds()))));
            final var result = Boolean.parseBoolean(StandardCharsets.UTF_8.decode(invoke.getPayload()).toString());
            if (!result) {
                throw new CloudEndureException("安装代理软件失败");
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("安装代理软件失败", e);
        }
    }

    public void launchMachines(Project project, ManageCloudEndureMachinesRequest request) {
        try {
            final var ceProjectId = project.getCloudEndureProject().getItem(request.getSide()).getId();
            final var invoke = lambda.invoke(new InvokeRequest()
                    .withFunctionName("DRPCloudEndureLaunchMachines")
                    .withPayload(mapper.writeValueAsString(Map.of(
                            "projectId", ceProjectId,
                            "launchType", request.getLaunchType(),
                            "machineIds", request.getMachineIds()))));
            final var output = StandardCharsets.UTF_8.decode(invoke.getPayload()).toString();
            log.debug("Launch machines output [{}]", output);
            mapper.readValue(output, CloudEndureLaunchResult.class);
        } catch (IOException e) {
            throw new IllegalStateException("启动虚拟机失败 " + e.getLocalizedMessage(), e);
        }
    }
}
