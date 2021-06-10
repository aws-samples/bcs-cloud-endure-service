// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.cem.service;

enum InstanceType {
    t2_micro(1, 1),
    t2_small(1, 2),
    t2_medium(2, 4),
    t2_large(2, 8),

    m5_large(2, 8),
    m5_xlarge(4, 16),
    m5_2xlarge(8, 32),
    m5_4xlarge(16, 64),
    m5_8xlarge(32, 128),
    m5_12xlarge(48, 192),
    m5_16xlarge(64, 256),
    m5_24xlarge(96, 384),
    ;

    static InstanceType find(boolean economy, int cpus, long memory) {
        for (var type : InstanceType.values()) {
            if (economy && type.compareTo(t2_large) > 0) {
                return t2_large;
            }

            if (type.cpus >= cpus && type.memory >= memory) {
                return type;
            }
        }

        return InstanceType.values()[InstanceType.values().length - 1];
    }

    private final int cpus;
    private final long memory;

    InstanceType(int cpus, long memory) {
        this.cpus = cpus;
        this.memory = memory;
    }

    String getName() {
        return name().replace('_', '.');
    }
}
