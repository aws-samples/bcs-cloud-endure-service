// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.cem.dto;

import aws.proserve.bcs.dr.ce.CloudEndureItem;
import aws.proserve.bcs.dr.dto.request.CreateProjectRequest;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * From GWT.
 */
@JsonSerialize(as = ImmutableCreateCemProjectRequest.class)
@JsonDeserialize(as = ImmutableCreateCemProjectRequest.class)
@Value.Immutable
public interface CreateCemProjectRequest extends CreateProjectRequest {

    /**
     * @apiNote For on-perm to AWS migration, the source region is {@code null}.
     */
    @Override
    @Nullable
    default String getSourceRegion() {
        return null;
    }

    /**
     * @return the CE project.
     */
    CloudEndureItem getProject();

    String getTargetVpcId();
}
