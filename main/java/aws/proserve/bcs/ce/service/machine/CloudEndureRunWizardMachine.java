// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.service.machine;

import aws.proserve.bcs.ce.dto.ImmutableRunCloudEndureWizardRequest;
import aws.proserve.bcs.ce.dto.RunCloudEndureWizardRequest;
import aws.proserve.bcs.ce.service.CloudEndureNetworkService;
import aws.proserve.bcs.dr.machine.AbstractStateMachine;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Named;

@Named
public class CloudEndureRunWizardMachine extends AbstractStateMachine {

    private final CloudEndureNetworkService cloudEndureNetworkService;

    CloudEndureRunWizardMachine(
            AWSStepFunctions machine,
            ObjectMapper mapper,
            CloudEndureNetworkService cloudEndureNetworkService) {
        super(machine, mapper);
        this.cloudEndureNetworkService = cloudEndureNetworkService;
    }

    /**
     * Prepare target VPC ID if it is properly replicated.
     */
    public void run(RunCloudEndureWizardRequest request, String secretId) {
        execute(ImmutableRunCloudEndureWizardRequest.builder()
                .from(request)
                .sourceCredential(null)
                .sourceCredentialId(secretId)
                .targetVpcId(
                        cloudEndureNetworkService.findTargetVpcId(
                                request.getSourceVpcId(),
                                request.getSourceRegion(),
                                request.getTargetRegion()))
                .build());
    }
}
