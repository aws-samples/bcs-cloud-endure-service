// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.service;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;

@Named
@Aspect
public class SessionAspect {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private SessionService sessionService;

    SessionAspect(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Around("execution(public * aws.proserve.bcs.ce.service.*Service.*(..))")
    public Object loginSession(ProceedingJoinPoint joinPoint) throws Throwable {
        if (joinPoint.getSignature().getName().equals("login")) {
            return joinPoint.proceed();
        }

        try {
            return joinPoint.proceed();
        } catch (Exception e) { // try to login in case the session expires.
            final var account = sessionService.login();
            log.info("Login for {}", account.getUsername());
            return joinPoint.proceed();
        }
    }
}
