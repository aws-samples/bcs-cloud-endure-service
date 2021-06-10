// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.api;

import aws.proserve.bcs.ce.CloudEndureCredential;
import aws.proserve.bcs.ce.CloudEndureLicense;
import aws.proserve.bcs.ce.CloudEndureRegion;
import aws.proserve.bcs.ce.dto.CloudEndureCredentialInput;
import aws.proserve.bcs.ce.dto.GetAwsInstanceRequest;
import aws.proserve.bcs.ce.dto.GetAwsVpcRequest;
import aws.proserve.bcs.ce.dto.RunCloudEndureWizardRequest;
import aws.proserve.bcs.ce.service.CloudEndureInstanceService;
import aws.proserve.bcs.ce.service.CloudEndureNetworkService;
import aws.proserve.bcs.ce.service.CloudEndureStateMachineService;
import aws.proserve.bcs.ce.service.CredentialService;
import aws.proserve.bcs.ce.service.LicenseService;
import aws.proserve.bcs.ce.service.RegionService;
import aws.proserve.bcs.dr.aws.AwsInstance;
import aws.proserve.bcs.dr.aws.AwsVpc;
import aws.proserve.bcs.dr.dto.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/cloudendure")
class CloudEndureController {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CredentialService credentialService;
    private final LicenseService licenseService;
    private final RegionService regionService;

    private final CloudEndureInstanceService cloudEndureInstanceService;
    private final CloudEndureNetworkService cloudEndureNetworkService;
    private final CloudEndureStateMachineService cloudEndureStateMachineService;

    CloudEndureController(
            CredentialService credentialService,
            LicenseService licenseService,
            RegionService regionService,

            CloudEndureInstanceService cloudEndureInstanceService,
            CloudEndureNetworkService cloudEndureNetworkService,
            CloudEndureStateMachineService cloudEndureStateMachineService) {
        this.credentialService = credentialService;
        this.licenseService = licenseService;
        this.regionService = regionService;

        this.cloudEndureInstanceService = cloudEndureInstanceService;
        this.cloudEndureNetworkService = cloudEndureNetworkService;
        this.cloudEndureStateMachineService = cloudEndureStateMachineService;
    }

    @GetMapping("/credentials")
    ResponseEntity<CloudEndureCredential[]> findAllCredentials() {
        return ResponseEntity.ok(credentialService.findAllCredentials());
    }

    @PostMapping("/credentials")
    ResponseEntity<CloudEndureCredential> save(@RequestBody CloudEndureCredentialInput input) {
        final var credential = credentialService.save(input);
        final var uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/credentials/{id}")
                .buildAndExpand(credential.getId()).toUri();
        return ResponseEntity.created(uri).body(credential);
    }

    @GetMapping("/licenses/migration")
    ResponseEntity<CloudEndureLicense> findAllLicenses() {
        final var license = licenseService.findMigrationLicense();
        if (license == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(license);
        }
    }

    @GetMapping("/credentials/{credId}/regions")
    ResponseEntity<CloudEndureRegion[]> findAllRegions(@PathVariable String credId) {
        return ResponseEntity.ok(regionService.findAllRegions(credId));
    }

    /**
     * @apiNote Get mapping does not accept request body.
     */
    @PutMapping("/awsVpcs")
    ResponseEntity<AwsVpc[]> findAllAwsVpcs(@RequestBody GetAwsVpcRequest request) {
        return ResponseEntity.ok(cloudEndureNetworkService.findAllAwsVpcs(request));
    }

    /**
     * @apiNote Get mapping does not accept request body.
     */
    @PutMapping("/awsInstances")
    ResponseEntity<AwsInstance[]> findAwsInstances(@RequestBody GetAwsInstanceRequest request) {
        return ResponseEntity.ok(cloudEndureInstanceService.findAllQualifiedInstances(
                request.getSourceRegion(), request.getSourceCredential(), request.getVpcId()));
    }

    @PostMapping("/wizard")
    ResponseEntity<Response> runWizard(@RequestBody RunCloudEndureWizardRequest request) {
        cloudEndureStateMachineService.runWizard(request);
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }
}
