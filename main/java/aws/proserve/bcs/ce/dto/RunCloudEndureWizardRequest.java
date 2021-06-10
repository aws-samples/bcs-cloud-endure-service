// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableRunCloudEndureWizardRequest.class)
@JsonDeserialize(as = ImmutableRunCloudEndureWizardRequest.class)
@Value.Immutable
public interface RunCloudEndureWizardRequest extends CreateCloudEndureProjectRequest {

    String getCidr();

    /**
     * @apiNote must provide this default method for boolean value because GWT omits the boolean value if it is false,
     * therefore, JSON will complain that the value is missing.
     * <p>
     * Must use prefix of <code>get</code> instead of <code>is</code> as JSON generates <code>setIs</code> for
     * <code>is</code> prefix methods.
     */
    @Value.Default
    default boolean getContinuous() {
        return false;
    }

    /**
     * @return EC2 instance ID. (Do NOT use machineId)
     */
    String[] getInstanceIds();
}
