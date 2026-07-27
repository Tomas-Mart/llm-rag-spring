package com.example.rag.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class NPlusOneDetector {

    @PersistenceContext
    private EntityManager entityManager;

    private static final int MAX_QUERIES_THRESHOLD = 3;

    @After("execution(* com.example.rag.service.*.*(..))")
    public void detectNPlusOne(JoinPoint joinPoint) {
        Statistics statistics = entityManager.unwrap(org.hibernate.Session.class)
                .getSessionFactory()
                .getStatistics();

        long queryCount = statistics.getQueryExecutionCount();

        if (queryCount > MAX_QUERIES_THRESHOLD) {
            log.warn("========================================");
            log.warn("⚠️  POTENTIAL N+1 PROBLEM DETECTED!");
            log.warn("   Method: {}.{}",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName());
            log.warn("   Queries executed: {}", queryCount);
            log.warn("   Threshold: {}", MAX_QUERIES_THRESHOLD);
            log.warn("========================================");
        }
    }
}