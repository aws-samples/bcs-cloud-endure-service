// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.dto;

import aws.proserve.bcs.dr.project.Side;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableCutbackRequest.class)
@JsonDeserialize(as = ImmutableCutbackRequest.class)
@Value.Immutable
public interface CutbackRequest {

    boolean getTerminate();

    Side getSide();

    String getProjectId();
}
