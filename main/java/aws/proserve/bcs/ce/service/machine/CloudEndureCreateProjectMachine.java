// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.service.machine;

import aws.proserve.bcs.ce.dto.CreateCloudEndureProjectRequest;
import aws.proserve.bcs.ce.dto.ImmutableCreateCloudEndureProjectRequest;
import aws.proserve.bcs.dr.machine.AbstractStateMachine;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Named;

@Named
public class CloudEndureCreateProjectMachine extends AbstractStateMachine {

    CloudEndureCreateProjectMachine(AWSStepFunctions machine, ObjectMapper mapper) {
        super(machine, mapper);
    }

    public void create(CreateCloudEndureProjectRequest request, String secretId, String subnetId) {
        execute(ImmutableCreateCloudEndureProjectRequest.builder()
                .from(request)
                .sourceCredential(null)
                .sourceCredentialId(secretId)
                .stagingSubnetId(subnetId)
                .build());
    }
}
