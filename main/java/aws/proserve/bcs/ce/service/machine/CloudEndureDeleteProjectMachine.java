// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.service.machine;

import aws.proserve.bcs.dr.dto.request.ImmutableDeleteItemRequest;
import aws.proserve.bcs.dr.machine.AbstractStateMachine;
import aws.proserve.bcs.dr.project.Project;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Named;

@Named
public class CloudEndureDeleteProjectMachine extends AbstractStateMachine {

    CloudEndureDeleteProjectMachine(AWSStepFunctions machine, ObjectMapper mapper) {
        super(machine, mapper);
    }

    public void delete(Project project) {
        execute(ImmutableDeleteItemRequest.builder()
                .id(project.getId())
                .build());
    }
}
