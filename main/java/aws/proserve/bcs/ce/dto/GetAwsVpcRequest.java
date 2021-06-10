// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.dto;

import aws.proserve.bcs.dr.secret.Credential;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * From GWT.
 */
@JsonSerialize(as = ImmutableGetAwsVpcRequest.class)
@JsonDeserialize(as = ImmutableGetAwsVpcRequest.class)
@Value.Immutable
public interface GetAwsVpcRequest {

    Credential getSourceCredential();

    String getSourceRegion();

    String getTargetRegion();
}
