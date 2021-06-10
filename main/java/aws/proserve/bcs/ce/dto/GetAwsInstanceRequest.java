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
@JsonSerialize(as = ImmutableGetAwsInstanceRequest.class)
@JsonDeserialize(as = ImmutableGetAwsInstanceRequest.class)
@Value.Immutable
public interface GetAwsInstanceRequest {

    Credential getSourceCredential();

    String getSourceRegion();

    String getVpcId();
}
