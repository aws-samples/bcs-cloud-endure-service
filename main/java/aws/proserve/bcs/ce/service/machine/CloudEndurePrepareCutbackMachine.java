// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.service.machine;

import aws.proserve.bcs.ce.dto.ImmutableCutbackRequest;
import aws.proserve.bcs.dr.machine.AbstractStateMachine;
import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.project.Side;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Named;

@Named
public class CloudEndurePrepareCutbackMachine extends AbstractStateMachine {

    CloudEndurePrepareCutbackMachine(AWSStepFunctions machine, ObjectMapper mapper) {
        super(machine, mapper);
    }

    public void cutback(Project project, boolean terminate) {
        execute(ImmutableCutbackRequest.builder()
                .terminate(terminate)
                .side(Side.source) // instances of side to terminate
                .projectId(project.getId())
                .build());
    }
}
