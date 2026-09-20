package com.group9.eventgaze.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    @Value("${AWS.ACCESS.KEY.ID:${aws.access-key-id:${ACCESS_KEY:}}}")
    private String accessKey;

    @Value("${AWS.SECRET.ACCESS.KEY:${aws.secret-access-key:${SECRET_ACCESS_KEY:}}}")
    private String secretKey;

    @Value("${AWS.REGION:${aws.region:us-east-1}}")
    private String region;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region != null && !region.isBlank() ? region : "us-east-1"));

        if (accessKey != null && !accessKey.isBlank() &&
                secretKey != null && !secretKey.isBlank() &&
                !"default_access_key".equalsIgnoreCase(accessKey) &&
                !"mock_key".equalsIgnoreCase(accessKey) &&
                !"none".equalsIgnoreCase(accessKey)) {
            log.info("Configuring S3Client with static credentials for region {}", region);
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(awsCredentials));
        } else {
            log.info("Configuring S3Client with DefaultCredentialsProvider");
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}
