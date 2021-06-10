// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.cem.service;

import aws.proserve.bcs.ce.CloudEndureBlueprint;
import aws.proserve.bcs.ce.CloudEndureDisk;
import aws.proserve.bcs.ce.service.InstanceTypeService;
import aws.proserve.bcs.ce.service.MachineService;
import aws.proserve.bcs.ce.service.RegionService;
import aws.proserve.bcs.cem.CemBlueprint;
import aws.proserve.bcs.cem.dto.ConfigureBlueprintRequest;
import aws.proserve.bcs.cem.dto.CreateCemProjectRequest;
import aws.proserve.bcs.cem.dto.SelectSecurityGroupRequest;
import aws.proserve.bcs.cem.dto.SetBlueprintRequest;
import aws.proserve.bcs.dr.aws.AwsSecurityGroup;
import aws.proserve.bcs.dr.ce.CloudEndureConstants;
import aws.proserve.bcs.dr.cem.CemItem;
import aws.proserve.bcs.dr.cem.CemProject;
import aws.proserve.bcs.dr.project.Component;
import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.project.ProjectFinder;
import aws.proserve.bcs.dr.project.ProjectService;
import aws.proserve.bcs.dr.project.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Named
public class CemService implements ProjectService {
    private static final int GB = 1024 * 1024 * 1024;
    private static final String ECONOMY = "economy";
    private static final String BUSINESS = "business";
    private static final String CUSTOMIZED = "customized";
    private static final String T2_LARGE = "t2.large";
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AWSLambda lambda;
    private final ObjectMapper mapper;
    private final DynamoDBMapper dbMapper;

    private final MachineService machineService;
    private final ProjectFinder projectFinder;
    private final RegionService regionService;

    private final CemNetworkService networkService;
    private final InstanceTypeService instanceTypeService;

    CemService(
            AWSLambda lambda,
            ObjectMapper mapper,
            DynamoDBMapper dbMapper,
            MachineService machineService,
            ProjectFinder projectFinder,
            RegionService regionService,
            CemNetworkService networkService,
            InstanceTypeService instanceTypeService) {
        this.lambda = lambda;
        this.mapper = mapper;
        this.dbMapper = dbMapper;
        this.machineService = machineService;
        this.projectFinder = projectFinder;
        this.regionService = regionService;
        this.networkService = networkService;
        this.instanceTypeService = instanceTypeService;
    }

    public void create(CreateCemProjectRequest request) {
        final var cemProject = new CemProject();
        final var item = new CemItem();
        final var ceProject = request.getProject();
        item.setVpcId(request.getTargetVpcId());
        item.setProject(ceProject);
        cemProject.setItems(List.of(item));

        final var project = new Project();
        project.setName(request.getName());
        project.setType(Component.CloudEndureManager);
        project.setTargetRegion(new Region(Regions.fromName(request.getTargetRegion())));
        project.setCemProject(cemProject);

        if (ceProject.getSourceRegion() != null) {
            final var region = regionService.find(ceProject.getCloudCredentialsIDs()[0], ceProject.getSourceRegion());
            region.ifPresent(value -> project.setSourceRegion(new Region(Regions.fromName(value.getName()))));
        }

        log.debug("Save CEM project [{}]", project.getName());
        projectFinder.save(project);
    }

    @Override
    public void delete(Project project) {
        projectFinder.delete(project);
    }

    public List<CemBlueprint> getBlueprints(Project project) {
        return dbMapper.query(CemBlueprint.class, new DynamoDBQueryExpression<CemBlueprint>()
                .withKeyConditionExpression("id = :id")
                .withExpressionAttributeValues(Map.of(":id", new AttributeValue().withS(project.getId()))));
    }

    public void loadBlueprints(Project project) {
        final var item = project.getCemProject().getProject();
        final var machines = machineService.findAll(item.getId());
        final var subnet = networkService.findSubnet(project, false);
        final var securityGroups = networkService.findSecurityGroups(project);
        final var addresses = networkService.findIpAddress(project, subnet, machines.length);
        final var blueprintMap = getBlueprints(project).stream().collect(Collectors.toMap(CemBlueprint::getMachineId, i -> i));
        final var newBlueprints = new ArrayList<CemBlueprint>();
        for (int i = 0; i < machines.length; i++) {
            CemBlueprint blueprint = blueprintMap.get(machines[i].getId());
            if (blueprint == null) {
                final var p = machines[i].getSourceProperties();
                final var cpus = p.getCpu().length == 0 ? 1 : p.getCpu()[0].getCores();

                blueprint = new CemBlueprint();
                blueprint.setId(project.getId());
                blueprint.setCpus(cpus);
                blueprint.setMemory(p.getMemory());
                blueprint.setPublicSubnet(false);
                blueprint.setMachineId(machines[i].getId());
                blueprint.setName(p.getName());
                blueprint.setOsName(p.getOs());
                blueprint.setInstanceType(InstanceType.find(true, cpus, p.getMemory() / GB).getName());
                blueprint.setSubnetId(subnet.getSubnetId());
                blueprint.setIpAddress(addresses.get(i));
                blueprint.setSecurityGroups(securityGroups.get(p.getName()));
                blueprint.setDisks(Arrays.stream(p.getDisks()).map(CloudEndureDisk::getName).toArray(String[]::new));
                blueprint.setDiskIops(3000);
                blueprint.setDiskType(DiskType.STANDARD);
            } else {
                blueprint.setSecurityGroups(securityGroups.get(blueprint.getName()));
            }

            newBlueprints.add(blueprint);
        }
        dbMapper.batchSave(newBlueprints);
    }

    public void setBlueprint(Project project, SetBlueprintRequest request) {
        final var size = request.getMachineIds().length;
        Subnet subnet = null;
        List<String> addresses = null;

        if (!request.getSubnetIntact()) {
            subnet = networkService.findSubnet(project, request.getPublicSubnet());
            addresses = networkService.findIpAddress(project, subnet, size);
        }

        for (int i = 0; i < size; i++) {
            final var blueprint = dbMapper.load(CemBlueprint.class, project.getId(), request.getMachineIds()[i]);

            if (!request.getSubnetIntact()) {
                blueprint.setPublicSubnet(request.getPublicSubnet());
                blueprint.setSubnetId(subnet.getSubnetId());
                blueprint.setIpAddress(addresses.get(i));
            }

            if (!request.getDiskIntact()) {
                switch (request.getDiskType()) {
                    case ECONOMY:
                        blueprint.setDiskType(DiskType.STANDARD);
                        break;

                    case BUSINESS:
                        blueprint.setDiskType(DiskType.SSD);
                        break;

                    case CUSTOMIZED:
                        blueprint.setDiskType(DiskType.PROVISIONED_SSD);
                        break;
                }
            }

            if (!request.getInstanceIntact()) {
                switch (request.getInstanceType()) {
                    case ECONOMY:
                    case BUSINESS:
                        var instanceType = InstanceType.find(request.getInstanceType().equals("economy"), blueprint.getCpus(), blueprint.getMemory() / GB);
                        blueprint.setInstanceType(instanceType.getName());
                        break;

                    case CUSTOMIZED:
                        blueprint.setInstanceType(request.getInstanceType());
                        break;
                }
            }

            dbMapper.save(blueprint);
        }
    }

    public void selectSecurityGroup(Project project, SelectSecurityGroupRequest request) {
        final var size = request.getMachineIds().length;
        for (int i = 0; i < size; i++) {
            final var blueprint = dbMapper.load(CemBlueprint.class, project.getId(), request.getMachineIds()[i]);
            blueprint.setSecurityGroups(Arrays.asList(request.getSecurityGroups()));
            dbMapper.save(blueprint);
        }
    }

    public void configureBlueprint(Project project, ConfigureBlueprintRequest request) {
        final var ceProject = project.getCemProject().getProject();
        log.info("Configure blueprint for project [{}]", ceProject.getName());

        final AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
                .withRegion(project.getTargetRegion().toAwsRegion())
                .build();

        final var tags = List.of(new Tag(CloudEndureConstants.TAG_BLUEPRINT,
                ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME)));

        for (var blueprint : request.getBlueprints()) {
            final var instanceType = instanceTypeService.mapType(ec2, project.getTargetRegion().getName(),
                    blueprint.getInstanceType(), T2_LARGE);
            final var itemId = ceProject.getId();

            final var payload = new HashMap<String, Object>();
            payload.put("projectId", itemId);
            payload.put("machineId", blueprint.getMachineId());
            payload.put("securityGroupIds", blueprint.getSecurityGroups().stream().map(AwsSecurityGroup::getId).collect(Collectors.toList()));
            payload.put("subnetId", blueprint.getSubnetId());
            payload.put("privateIp", blueprint.getIpAddress());
            payload.put("iamRole", "");
            payload.put("instanceType", instanceType);
            payload.put("disks", blueprint.getDisks());
            payload.put("diskIops", blueprint.getDiskIops());
            payload.put("diskType", blueprint.getDiskType());
            payload.put("tags", tags);

            try {
                final var invoke = lambda.invoke(new InvokeRequest()
                        .withFunctionName("DRPCloudEndureConfigureBlueprint")
                        .withPayload(mapper.writeValueAsString(payload)));
                final var output = StandardCharsets.UTF_8.decode(invoke.getPayload()).toString();
                log.debug("Configure blueprint output [{}]", output);
                mapper.readValue(output, CloudEndureBlueprint.class);
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to configure blueprint", e);
            }
        }
    }
}
