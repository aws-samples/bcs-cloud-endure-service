# Disaster Recovery Factory - CloudEndure Service

## Introduction
This project is published as a solution in the Amazon Web Services Solutions Library.
For more information, including how to deploy it into your AWS account, please visit:
- https://www.amazonaws.cn/en/solutions/disaster-recovery-factory

### This Package
This package contains the backend service to manage CloudEndure.

To enable the auto-configuration of the cloud endure service, 
configure your `application.yaml` file with the following properties:
```yaml
cloudendure:
  enabled: true
  api:
    url: https://console.cloudendure.com/api/latest
```

Add the following property to deserialize datetime values as ISO strings:
```yaml
spring:
  jackson:
    serialization:
      WRITE_DATES_AS_TIMESTAMPS: false
```

## AWS Blogs
The following blog articles introduce in depth how this solution works and how to make the most of it.

- [Use Disaster Recovery Factory to efficiently manage instance disaster recovery configurations](https://aws.amazon.com/cn/blogs/china/use-cloud-disaster-recovery-management-tools-to-efficiently-manage-instance-disaster-recovery-configuration/) (March 2021)
- [Migrate and protect EC2 instances by Disaster Recovery Factory](https://aws.amazon.com/cn/blogs/china/gcr-blog-migrate-and-protect-ec2-instances-using-cloud-disaster-management-tools/) (July 2020)
