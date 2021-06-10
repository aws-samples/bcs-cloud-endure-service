// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.service;

import aws.proserve.bcs.dr.aws.AwsInstance;
import aws.proserve.bcs.dr.aws.ImmutableAwsInstance;
import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.project.Side;
import aws.proserve.bcs.dr.secret.Credential;
import aws.proserve.bcs.dr.secret.SecretManager;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.AttachedPolicy;
import com.amazonaws.services.identitymanagement.model.GetInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.ListAttachedRolePoliciesRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Named;
import java.util.ArrayList;

@Named
public class CloudEndureInstanceService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SecretManager secretManager;

    CloudEndureInstanceService(SecretManager secretManager) {
        this.secretManager = secretManager;
    }

    /**
     * @return machines:
     * <ol>
     * <li>with instance profile which contains <code>AmazonSSMManagedInstanceCore</code>.</li>
     * <li>resides in the designated VPC.</li>
     * </ol>
     */
    public AwsInstance[] findAllQualifiedInstances(Project project, Side side) {
        return findAllQualifiedInstances(project.getRegion(side).getName(),
                secretManager.getCredential(project),
                project.getCloudEndureProject().getVpcId(side));
    }

    public AwsInstance[] findAllQualifiedInstances(String region, Credential credential, String vpcId) {
        final var ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(credential.toProvider())
                .withRegion(region)
                .build();

        final var iam = AmazonIdentityManagementClientBuilder.standard()
                .withCredentials(credential.toProvider())
                .withRegion(region)
                .build();

        final var instances = new ArrayList<AwsInstance>();
        final var request = new DescribeInstancesRequest();
        DescribeInstancesResult result;
        do {
            result = ec2.describeInstances(request);
            request.setNextToken(result.getNextToken());

            for (var reservation : result.getReservations()) {
                for (var instance : reservation.getInstances()) {
                    if (!isQualified(instance, vpcId, iam)) {
                        continue;
                    }

                    instances.add(ImmutableAwsInstance.builder()
                            .from(AwsInstance.convert(instance))
                            .region(region)
                            .build());
                }
            }
        } while (result.getNextToken() != null);

        return instances.toArray(new AwsInstance[0]);
    }

    public boolean isQualified(
            Instance instance,
            @Nullable String vpcId,
            AmazonIdentityManagement iam) {
        final var profile = instance.getIamInstanceProfile();
        if (profile == null) {
            log.info("Instance [{}] has no instance profile.", instance.getInstanceId());
            return false;
        }

        final var arn = profile.getArn();
        if (arn.lastIndexOf('/') == -1) {
            log.info("Instance [{}]: invalid instance profile arn {}.", instance.getInstanceId(), arn);
            return false;
        }

        final var role = iam.getInstanceProfile(new GetInstanceProfileRequest()
                .withInstanceProfileName(arn.substring(arn.lastIndexOf('/') + 1)))
                .getInstanceProfile().getRoles().get(0); // has exactly one role

        if (iam.listAttachedRolePolicies(new ListAttachedRolePoliciesRequest()
                .withRoleName(role.getRoleName()))
                .getAttachedPolicies()
                .stream()
                .map(AttachedPolicy::getPolicyName)
                .noneMatch("AmazonSSMManagedInstanceCore"::equals)) {
            log.info("Instance [{}] profile has no AmazonSSMManagedInstanceCore policy attached",
                    instance.getInstanceId());
            return false;
        }

        if (vpcId != null && !instance.getVpcId().equals(vpcId)) {
            log.info("Instance [{}] is not running inside the source VPC [{}]", instance.getInstanceId(), vpcId);
            return false;
        }

        return true;
    }
}
