// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.cem.dto;


import aws.proserve.bcs.dr.aws.AwsSecurityGroup;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * From GWT.
 */
@JsonSerialize(as = ImmutableSelectSecurityGroupRequest.class)
@JsonDeserialize(as = ImmutableSelectSecurityGroupRequest.class)
@Value.Immutable
public interface SelectSecurityGroupRequest {

    String[] getMachineIds();

    AwsSecurityGroup[] getSecurityGroups();
}
