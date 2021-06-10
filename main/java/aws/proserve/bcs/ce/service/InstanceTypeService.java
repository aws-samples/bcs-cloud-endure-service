// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.service;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstanceTypesRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceTypesResult;
import com.amazonaws.services.ec2.model.InstanceTypeInfo;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Named
public class InstanceTypeService {

    private final Map<String, Set<String>> typeMap = new HashMap<>();

    public String mapType(AmazonEC2 targetEc2, String region, String type, String defaultType) {
        final Set<String> typeSet;

        if (typeMap.containsKey(region)) {
            typeSet = typeMap.get(region);
        } else {
            final var instanceTypes = new ArrayList<InstanceTypeInfo>();
            final var describeRequest = new DescribeInstanceTypesRequest();
            DescribeInstanceTypesResult result;
            do {
                result = targetEc2.describeInstanceTypes(describeRequest);
                describeRequest.setNextToken(result.getNextToken());
                instanceTypes.addAll(result.getInstanceTypes());
            } while (result.getNextToken() != null);

            typeSet = instanceTypes.stream()
                    .map(InstanceTypeInfo::getInstanceType)
                    .collect(Collectors.toSet());
            typeMap.put(region, typeSet);
        }

        return typeSet.contains(type) ? type : defaultType;
    }
}
