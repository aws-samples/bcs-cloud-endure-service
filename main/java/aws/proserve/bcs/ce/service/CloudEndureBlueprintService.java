// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.service;

import aws.proserve.bcs.ce.CloudEndureBlueprint;
import aws.proserve.bcs.ce.exception.CloudEndureException;
import aws.proserve.bcs.dr.ce.CloudEndureConstants;
import aws.proserve.bcs.dr.dynamo.DynamoConstants;
import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.project.Side;
import aws.proserve.bcs.dr.secret.SecretManager;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
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
import java.util.HashMap;
import java.util.Map;

@Named
public class CloudEndureBlueprintService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ObjectMapper mapper;
    private final DynamoDB dynamoDB;
    private final AWSLambda lambda;
    private final SecretManager secretManager;

    private final InstanceTypeService instanceTypeService;

    CloudEndureBlueprintService(
            ObjectMapper mapper,
            DynamoDB dynamoDB,
            AWSLambda lambda,
            SecretManager secretManager,

            InstanceTypeService instanceTypeService) {
        this.mapper = mapper;
        this.dynamoDB = dynamoDB;
        this.lambda = lambda;
        this.secretManager = secretManager;
        this.instanceTypeService = instanceTypeService;
    }

    public CloudEndureBlueprint configure(Project project, Side side, String machineId, String instanceId) {
        log.debug("Configure blueprint for [{}] machine [{}, {}]", side, machineId, instanceId);
        final AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
                .withRegion(project.getRegion(side).toAwsRegion())
                .withCredentials(secretManager.getCredential(project).toProvider())
                .build();

        final Instance instance;
        try {
            instance = ec2.describeInstances(new DescribeInstancesRequest()
                    .withInstanceIds(instanceId)).getReservations().get(0).getInstances().get(0);
        } catch (RuntimeException e) {
            throw new CloudEndureException("找不到虚拟机 " + instanceId);
        }

        final var securityGroups = new String[instance.getSecurityGroups().size()];
        final var table = dynamoDB.getTable(DynamoConstants.TABLE_VPC);
        final var subnetId = side == Side.source
                ? findTargetId(table, instance.getSubnetId())
                : findSourceId(table, instance.getSubnetId());

        for (var i = 0; i < securityGroups.length; i++) {
            final var group = instance.getSecurityGroups().get(i);
            securityGroups[i] = side == Side.source
                    ? findTargetId(table, group.getGroupId())
                    : findSourceId(table, group.getGroupId());
        }

        final var tags = instance.getTags();
        tags.add(new Tag(CloudEndureConstants.TAG_BLUEPRINT,
                ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME)));

        final var profile = instance.getIamInstanceProfile() == null ? null : instance.getIamInstanceProfile().getArn();
        final var instanceType = instanceTypeService.mapType(ec2, project.getRegion(side).getName(),
                instance.getInstanceType(), project.getCloudEndureProject().getTargetInstanceType());
        final var itemId = project.getCloudEndureProject().getItem(side).getId();

        final var payload = new HashMap<String, Object>();
        payload.put("projectId", itemId);
        payload.put("machineId", machineId);
        payload.put("subnetId", subnetId);
        payload.put("securityGroupIds", securityGroups);
        payload.put("privateIp", instance.getPrivateIpAddress());
        payload.put("instanceType", instanceType);
        payload.put("tags", tags);
        payload.put("disks", instance.getBlockDeviceMappings().stream()
                .map(InstanceBlockDeviceMapping::getDeviceName)
                .map(name -> name.replaceAll("/dev/sd", "/dev/xvd"))
                .toArray(String[]::new));

        if (profile != null && profile.lastIndexOf('/') != -1) {
            payload.put("iamRole", profile.substring(profile.lastIndexOf('/') + 1));
        } else {
            payload.put("iamRole", "");
        }

        try {
            final var invoke = lambda.invoke(new InvokeRequest()
                    .withFunctionName("DRPCloudEndureConfigureBlueprint")
                    .withPayload(mapper.writeValueAsString(payload)));
            final var output = StandardCharsets.UTF_8.decode(invoke.getPayload()).toString();
            log.debug("Configure blueprint output [{}]", output);
            return mapper.readValue(output, CloudEndureBlueprint.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to configure blueprint", e);
        }
    }

    private String findTargetId(Table table, String id) {
        Item result = null;
        for (var item : table.query(DynamoConstants.KEY_ID, id)) {
            result = item;
        }

        if (result == null) {
            throw new CloudEndureException("Unable to find target ID " + id);
        }
        return result.getString(DynamoConstants.KEY_TARGET_ID);
    }

    private String findSourceId(Table table, String id) {
        Item result = null;
        for (var item : table.scan(DynamoConstants.KEY_TARGET_ID + " = :id", null,
                Map.of(":id", id))) {
            result = item;
        }

        if (result == null) {
            throw new CloudEndureException("Unable to find source ID " + id);
        }
        return result.getString(DynamoConstants.KEY_ID);
    }
}
