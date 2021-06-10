// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.cem.dto;


import aws.proserve.bcs.cem.CemBlueprint;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * From GWT.
 */
@JsonSerialize(as = ImmutableConfigureBlueprintRequest.class)
@JsonDeserialize(as = ImmutableConfigureBlueprintRequest.class)
@Value.Immutable
public interface ConfigureBlueprintRequest {

    CemBlueprint[] getBlueprints();
}
