// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.cem.dto;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * From GWT.
 */
@JsonSerialize(as = ImmutableSetBlueprintRequest.class)
@JsonDeserialize(as = ImmutableSetBlueprintRequest.class)
@Value.Immutable
public interface SetBlueprintRequest {
    /**
     * @apiNote must provide this default method for boolean value because GWT omits the boolean value if it is false,
     * therefore, JSON will complain that the value is missing.
     * <p>
     * Must use prefix of <code>get</code> instead of <code>is</code> as JSON generates <code>setIs</code> for
     * <code>is</code> prefix methods.
     */
    @Value.Default
    default boolean getPublicSubnet() {
        return false;
    }

    @Value.Default
    default boolean getDiskIntact() {
        return false;
    }

    @Value.Default
    default boolean getInstanceIntact() {
        return false;
    }

    @Value.Default
    default boolean getSubnetIntact() {
        return false;
    }

    String[] getMachineIds();

    String getDiskType();

    String getInstanceType();
}
