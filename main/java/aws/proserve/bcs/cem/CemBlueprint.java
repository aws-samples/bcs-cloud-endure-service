// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.cem;

import aws.proserve.bcs.cem.service.DiskType;
import aws.proserve.bcs.dr.aws.AwsSecurityGroup;
import aws.proserve.bcs.dr.cem.CemConstants;
import aws.proserve.bcs.dr.dto.HasName;
import aws.proserve.bcs.dr.project.Item;
import aws.proserve.bcs.dr.util.StringArrayListConverter;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAutoGenerateStrategy;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAutoGeneratedTimestamp;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel.DynamoDBAttributeType;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTyped;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBVersionAttribute;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * The blueprint class for CloudEndure machine. One blueprint for one machine, identified by machine ID.
 */
@DynamoDBTable(tableName = CemConstants.TABLE_BLUEPRINT)
public class CemBlueprint extends Item implements HasName {

    private int cpus;
    private long memory;
    private boolean publicSubnet;
    private String machineId;
    private String name;
    private String osName;
    private String iamRole;
    private String instanceType;
    private String subnetId;
    private String ipAddress;
    private String[] disks;
    private int diskIops;
    private DiskType diskType;
    private List<AwsSecurityGroup> securityGroups;
    private Long version;
    private Date createdDate;
    private Date lastUpdatedDate;

    /**
     * @return the DRP CEM project ID.
     */
    @Override
    @DynamoDBHashKey
    public String getId() {
        return super.getId();
    }

    public int getCpus() {
        return cpus;
    }

    public void setCpus(int cpus) {
        this.cpus = cpus;
    }

    public long getMemory() {
        return memory;
    }

    public void setMemory(long memory) {
        this.memory = memory;
    }

    @DynamoDBTyped(DynamoDBAttributeType.BOOL)
    public boolean isPublicSubnet() {
        return publicSubnet;
    }

    public void setPublicSubnet(boolean publicSubnet) {
        this.publicSubnet = publicSubnet;
    }

    /**
     * @return the CE machine ID.
     */
    @DynamoDBRangeKey
    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public String getIamRole() {
        return iamRole;
    }

    public void setIamRole(String iamRole) {
        this.iamRole = iamRole;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @DynamoDBTypeConverted(converter = StringArrayListConverter.class)
    public String[] getDisks() {
        return disks;
    }

    public void setDisks(String[] disks) {
        this.disks = disks;
    }

    public int getDiskIops() {
        return diskIops;
    }

    public void setDiskIops(int diskIops) {
        this.diskIops = diskIops;
    }

    @DynamoDBTypeConverted(converter = DiskTypeConverter.class)
    public DiskType getDiskType() {
        return diskType;
    }

    public void setDiskType(DiskType diskType) {
        this.diskType = diskType;
    }

    public List<AwsSecurityGroup> getSecurityGroups() {
        return securityGroups;
    }

    public void setSecurityGroups(List<AwsSecurityGroup> securityGroups) {
        this.securityGroups = securityGroups == null ? Collections.emptyList() : securityGroups;
    }

    @DynamoDBVersionAttribute
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @DynamoDBAutoGeneratedTimestamp(strategy = DynamoDBAutoGenerateStrategy.CREATE)
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @DynamoDBAutoGeneratedTimestamp
    public Date getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(Date lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    /**
     * @apiNote DynamoDB requires it to be public.
     */
    public static class DiskTypeConverter implements DynamoDBTypeConverter<String, DiskType> {
        @Override
        public String convert(DiskType type) {
            return type.name();
        }

        @Override
        public DiskType unconvert(String value) {
            return DiskType.valueOf(value);
        }
    }
}
