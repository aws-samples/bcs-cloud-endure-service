// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.service;

import aws.proserve.bcs.ce.dto.CreateCloudEndureProjectRequest;
import aws.proserve.bcs.ce.dto.ImmutableCreateCloudEndureProjectRequest;
import aws.proserve.bcs.ce.dto.RunCloudEndureWizardRequest;
import aws.proserve.bcs.ce.service.machine.CloudEndureCreateProjectMachine;
import aws.proserve.bcs.ce.service.machine.CloudEndureDeleteProjectMachine;
import aws.proserve.bcs.ce.service.machine.CloudEndurePrepareCutbackMachine;
import aws.proserve.bcs.ce.service.machine.CloudEndureRunWizardMachine;
import aws.proserve.bcs.dr.exception.PortalException;
import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.project.ProjectService;
import aws.proserve.bcs.dr.project.Side;
import aws.proserve.bcs.dr.secret.SecretManager;

import javax.inject.Named;

@Named
public class CloudEndureStateMachineService implements ProjectService {

    private final SecretManager securityManager;

    private final CloudEndureNetworkService cloudEndureNetworkService;
    private final CloudEndureProjectService cloudEndureProjectService;

    private final CloudEndureCreateProjectMachine createProjectMachine;
    private final CloudEndureDeleteProjectMachine deleteProjectMachine;
    private final CloudEndurePrepareCutbackMachine prepareCutbackMachine;
    private final CloudEndureRunWizardMachine runWizardMachine;

    CloudEndureStateMachineService(
            SecretManager securityManager,

            CloudEndureNetworkService cloudEndureNetworkService,
            CloudEndureProjectService cloudEndureProjectService,

            CloudEndureCreateProjectMachine createProjectMachine,
            CloudEndureDeleteProjectMachine deleteProjectMachine,
            CloudEndurePrepareCutbackMachine prepareCutbackMachine,
            CloudEndureRunWizardMachine runWizardMachine) {
        this.securityManager = securityManager;

        this.cloudEndureProjectService = cloudEndureProjectService;
        this.cloudEndureNetworkService = cloudEndureNetworkService;

        this.createProjectMachine = createProjectMachine;
        this.deleteProjectMachine = deleteProjectMachine;
        this.prepareCutbackMachine = prepareCutbackMachine;
        this.runWizardMachine = runWizardMachine;
    }

    public void create(CreateCloudEndureProjectRequest request) {
        final var secretId = securityManager.saveSecret(request.getSourceCredential());
        if (!request.getPublicNetwork()) {
            cloudEndureNetworkService.peerVpc(request, secretId);
        }

        createProjectMachine.create(request, secretId,
                cloudEndureNetworkService.findStagingSubnetId(request, secretId));
    }

    @Override
    public void delete(Project project) {
        deleteProjectMachine.delete(project);
        securityManager.deleteSecret(project.generateSecretId(Side.source));
        securityManager.deleteTempSecrets();
    }

    public void cutback(Project project, boolean terminate) {
        final var cause = cloudEndureProjectService.checkCutbackPrecondition(project);
        if (cause == null) {
            prepareCutbackMachine.cutback(project, terminate);
        } else {
            throw new PortalException(cause);
        }
    }

    public void runWizard(RunCloudEndureWizardRequest request) {
        runWizardMachine.run(request,
                securityManager.saveSecret(request.getSourceCredential()));
    }
}
