// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.service;

import aws.proserve.bcs.ce.dto.CreateCloudEndureProjectRequest;
import aws.proserve.bcs.ce.dto.GetAwsVpcRequest;
import aws.proserve.bcs.ce.dto.ImmutableCreateCloudEndureProjectRequest;
import aws.proserve.bcs.dr.aws.AwsVpc;
import aws.proserve.bcs.dr.aws.ImmutableAwsVpc;
import aws.proserve.bcs.dr.dynamo.DynamoConstants;
import aws.proserve.bcs.dr.exception.PortalException;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Named
public class CloudEndureNetworkService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ObjectMapper mapper;
    private final DynamoDB dynamoDB;
    private final AWSLambda lambda;

    CloudEndureNetworkService(
            ObjectMapper mapper,
            DynamoDB dynamoDB,
            AWSLambda lambda) {
        this.mapper = mapper;
        this.dynamoDB = dynamoDB;
        this.lambda = lambda;
    }

    public AwsVpc[] findAllAwsVpcs(GetAwsVpcRequest request) {
        final var sourceEc2 = AmazonEC2ClientBuilder.standard()
                .withRegion(request.getSourceRegion())
                .withCredentials(request.getSourceCredential().toProvider())
                .build();
        final var targetEc2 = AmazonEC2ClientBuilder.standard()
                .withRegion(request.getTargetRegion())
                .withCredentials(request.getSourceCredential().toProvider())
                .build();

        final var table = dynamoDB.getTable(DynamoConstants.TABLE_VPC);
        final var candidates = new ArrayList<Object[]>();
        final var sourceVpcs = AwsVpc.getVpcs(sourceEc2);
        final var targetVpcs = AwsVpc.getVpcs(targetEc2).stream().map(Vpc::getVpcId).collect(Collectors.toSet());
        for (var sourceVpc : sourceVpcs) {
            for (var item : table.query(DynamoConstants.KEY_ID, sourceVpc.getVpcId())) {
                final var targetVpcId = item.getString(DynamoConstants.KEY_TARGET_ID);
                if (targetVpcs.contains(targetVpcId)
                        && request.getSourceRegion().equals(item.getString(DynamoConstants.KEY_SOURCE_REGION))
                        && request.getTargetRegion().equals(item.getString(DynamoConstants.KEY_TARGET_REGION))) {
                    candidates.add(new Object[]{sourceVpc, targetVpcId});
                }
            }
        }
        return candidates.stream().map(this::map).toArray(AwsVpc[]::new);
    }

    void peerVpc(CreateCloudEndureProjectRequest request, String secretId) {
        try {
            final var result = lambda.invoke(new InvokeRequest()
                    .withFunctionName("DRPCommonPeerVpc")
                    .withPayload(mapper.writeValueAsString(
                            ImmutableCreateCloudEndureProjectRequest.builder()
                                    .from(request)
                                    .sourceCredential(null)
                                    .sourceCredentialId(secretId)
                                    .build())));
            final var output = StandardCharsets.UTF_8.decode(result.getPayload()).toString();
            log.debug("DRPCommonPeerVpc {}: {}", result.getStatusCode(), output);

            final Map<String, Object> resultMap = mapper.readValue(output, Map.class);
            if (resultMap != null && resultMap.get("errorMessage") != null) {
                throw new PortalException(resultMap.get("errorMessage").toString());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to set VPC peering", e);
        }
    }

    String findStagingSubnetId(CreateCloudEndureProjectRequest request, String secretId) {
        try {
            final var result = lambda.invoke(new InvokeRequest()
                    .withFunctionName("DRPCommonFindCommonSubnet")
                    .withPayload(mapper.writeValueAsString(
                            ImmutableCreateCloudEndureProjectRequest.builder()
                                    .from(request)
                                    .sourceCredential(null)
                                    .sourceCredentialId(secretId)
                                    .build())));
            final var output = StandardCharsets.UTF_8.decode(result.getPayload()).toString();
            return mapper.readValue(output, String.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to find common subnet", e);
        }
    }

    public String findTargetVpcId(String sourceVpcId, String sourceRegion, String targetRegion) {
        final var table = dynamoDB.getTable(DynamoConstants.TABLE_VPC);
        for (var item : table.query(DynamoConstants.KEY_ID, sourceVpcId)) {
            if (sourceRegion.equals(item.getString(DynamoConstants.KEY_SOURCE_REGION))
                    && targetRegion.equals(item.getString(DynamoConstants.KEY_TARGET_REGION))) {
                return item.getString(DynamoConstants.KEY_TARGET_ID);
            }
        }

        return null;
    }

    private AwsVpc map(Object[] candidate) {
        final var vpc = (Vpc) candidate[0];
        final var peerVpcId = (String) candidate[1];
        return ImmutableAwsVpc.builder()
                .from(AwsVpc.convert(vpc))
                .peerVpcId(peerVpcId)
                .build();
    }
}
