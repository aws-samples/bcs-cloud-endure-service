// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.cem.service;

import aws.proserve.bcs.dr.aws.AwsSecurityGroup;
import aws.proserve.bcs.dr.ce.CloudEndureConstants;
import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.vpc.Cidr;
import aws.proserve.bcs.dr.vpc.Filters;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeNetworkInterfacesRequest;
import com.amazonaws.services.ec2.model.DescribeNetworkInterfacesResult;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.Subnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Named
class CemNetworkService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Random rnd = new Random(System.currentTimeMillis());

    /**
     * @return A map from machine name to security groups.
     */
    Map<String, List<AwsSecurityGroup>> findSecurityGroups(Project project) {
        final var ec2 = AmazonEC2ClientBuilder.standard()
                .withRegion(project.getTargetRegion().toAwsRegion())
                .build();

        final var pairs = new ArrayList<Object[]>();
        final var vpcId = project.getCemProject().getFirst().getVpcId();
        final var describeRequest = new DescribeSecurityGroupsRequest()
                .withFilters(Filters.vpcId(vpcId));
        DescribeSecurityGroupsResult result;
        do {
            result = ec2.describeSecurityGroups(describeRequest);
            describeRequest.setNextToken(result.getNextToken());

            for (var group : result.getSecurityGroups()) {
                pairs.addAll(group.getTags().stream()
                        .filter(t -> t.getKey().equals(CloudEndureConstants.TAG_MACHINE))
                        .map(t -> t.getValue().split(","))
                        .flatMap(Arrays::stream)
                        .map(s -> new Object[]{s, new AwsSecurityGroup(group.getGroupId(), group.getGroupName())})
                        .collect(Collectors.toList()));
            }
        } while (result.getNextToken() != null);
        return pairs.stream().collect(Collectors.groupingBy(s -> (String) s[0],
                Collectors.mapping(s -> (AwsSecurityGroup) s[1], Collectors.toList())));
    }

    Subnet findSubnet(Project project, boolean publicSubnet) {
        final var ec2 = AmazonEC2ClientBuilder.standard()
                .withRegion(project.getTargetRegion().toAwsRegion())
                .build();

        final var vpcId = project.getCemProject().getFirst().getVpcId();
        final var describeRequest = new DescribeSubnetsRequest().withFilters(Filters.vpcId(vpcId));
        DescribeSubnetsResult result;
        do {
            result = ec2.describeSubnets(describeRequest);
            describeRequest.setNextToken(result.getNextToken());

            for (var subnet : result.getSubnets()) {
                final var tables = ec2.describeRouteTables(new DescribeRouteTablesRequest()
                        .withFilters(Filters.associatedSubnetId(subnet.getSubnetId()))).getRouteTables();
                if (tables.isEmpty()) {
                    if (!publicSubnet) { // no route table association, private subnet
                        log.info("Found private subnet {} with empty route table", subnet.getSubnetId());
                        return subnet;
                    }
                } else {
                    boolean routeIgw = false;
                    for (var table : tables) {
                        for (var route : table.getRoutes()) {
                            if (route.getGatewayId() != null && route.getGatewayId().startsWith("igw-")) { // route to IGW, public subnet
                                routeIgw = true;
                                if (publicSubnet) {
                                    log.info("Found public subnet {} with IGW", subnet.getSubnetId());
                                    return subnet;
                                }
                            }
                        }
                    }

                    if (!routeIgw && !publicSubnet) { // no route to IGW, private subnet
                        log.info("Found private subnet {} with no route to IGW", subnet.getSubnetId());
                        return subnet;
                    }
                }
            }
        } while (result.getNextToken() == null);
        throw new IllegalStateException("Unable to find a subnet [publicSubnet = " + publicSubnet + "]");
    }

    private List<String> findUnusedAddress(Cidr cidr, List<String> addresses, int count) {
        final var unused = new ArrayList<String>();
        final var size = cidr.getSize();
        for (int i = 0; i < size; i++) {
            final var address = cidr.findAddress(rnd.nextInt((int) size));
            if (addresses.contains(address)) {
                continue;
            }

            unused.add(address);

            if (unused.size() > count) {
                return unused;
            }
        }
        return unused;
    }

    List<String> findIpAddress(Project project, Subnet subnet, int count) {
        final var ec2 = AmazonEC2ClientBuilder.standard()
                .withRegion(project.getTargetRegion().toAwsRegion())
                .build();

        final var vpcId = project.getCemProject().getFirst().getVpcId();
        final var addresses = new ArrayList<String>();
        final var request = new DescribeNetworkInterfacesRequest()
                .withFilters(Filters.vpcId(vpcId));
        DescribeNetworkInterfacesResult describeResult;
        do {
            describeResult = ec2.describeNetworkInterfaces(request);
            request.setNextToken(describeResult.getNextToken());

            for (var i : describeResult.getNetworkInterfaces()) {
                addresses.add(i.getPrivateIpAddress());
            }
        } while (describeResult.getNextToken() != null);
        return findUnusedAddress(new Cidr(subnet.getCidrBlock()), addresses, count);
    }
}
