package com.bridge.service;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for monitoring database performance metrics including connection pool,
 * query performance, and Hibernate statistics
 */
@Service
public class DatabasePerformanceMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(DatabasePerformanceMonitoringService.class);

    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFactory;

    @Autowired
    public DatabasePerformanceMonitoringService(DataSource dataSource, EntityManagerFactory entityManagerFactory) {
        this.dataSource = dataSource;
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Get connection pool metrics from HikariCP
     */
    public ConnectionPoolMetrics getConnectionPoolMetrics() {
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();

                return new ConnectionPoolMetrics(
                    poolBean.getTotalConnections(),
                    poolBean.getActiveConnections(),
                    poolBean.getIdleConnections(),
                    poolBean.getThreadsAwaitingConnection(),
                    hikariDataSource.getMaximumPoolSize(),
                    hikariDataSource.getMinimumIdle(),
                    hikariDataSource.getConnectionTimeout(),
                    hikariDataSource.getIdleTimeout(),
                    hikariDataSource.getMaxLifetime()
                );
            }
        } catch (Exception e) {
            logger.error("Error getting connection pool metrics", e);
        }
        return new ConnectionPoolMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Get Hibernate statistics for query performance monitoring
     */
    public HibernateMetrics getHibernateMetrics() {
        try {
            SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
            Statistics stats = sessionFactory.getStatistics();

            return new HibernateMetrics(
                stats.getQueryExecutionCount(),
                stats.getQueryExecutionMaxTime(),
                stats.getQueryExecutionMaxTimeQueryString(),
                stats.getQueryCacheHitCount(),
                stats.getQueryCacheMissCount(),
                stats.getSecondLevelCacheHitCount(),
                stats.getSecondLevelCacheMissCount(),
                stats.getSessionOpenCount(),
                stats.getSessionCloseCount(),
                stats.getTransactionCount(),
                stats.getSuccessfulTransactionCount(),
                stats.getOptimisticFailureCount(),
                stats.getFlushCount(),
                stats.getConnectCount(),
                stats.getPrepareStatementCount(),
                stats.getCloseStatementCount()
            );
        } catch (Exception e) {
            logger.error("Error getting Hibernate metrics", e);
        }
        return new HibernateMetrics(0, 0, "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Get database performance summary
     */
    public DatabasePerformanceSummary getPerformanceSummary() {
        ConnectionPoolMetrics poolMetrics = getConnectionPoolMetrics();
        HibernateMetrics hibernateMetrics = getHibernateMetrics();

        // Calculate derived metrics
        double connectionUtilization = poolMetrics.getMaxPoolSize() > 0 ? 
            (double) poolMetrics.getActiveConnections() / poolMetrics.getMaxPoolSize() * 100 : 0;

        double queryCacheHitRatio = (hibernateMetrics.getQueryCacheHitCount() + hibernateMetrics.getQueryCacheMissCount()) > 0 ?
            (double) hibernateMetrics.getQueryCacheHitCount() / 
            (hibernateMetrics.getQueryCacheHitCount() + hibernateMetrics.getQueryCacheMissCount()) * 100 : 0;

        double secondLevelCacheHitRatio = (hibernateMetrics.getSecondLevelCacheHitCount() + hibernateMetrics.getSecondLevelCacheMissCount()) > 0 ?
            (double) hibernateMetrics.getSecondLevelCacheHitCount() / 
            (hibernateMetrics.getSecondLevelCacheHitCount() + hibernateMetrics.getSecondLevelCacheMissCount()) * 100 : 0;

        return new DatabasePerformanceSummary(
            poolMetrics,
            hibernateMetrics,
            connectionUtilization,
            queryCacheHitRatio,
            secondLevelCacheHitRatio,
            hibernateMetrics.getQueryExecutionCount() > 0 ? 
                hibernateMetrics.getQueryExecutionMaxTime() / hibernateMetrics.getQueryExecutionCount() : 0
        );
    }

    /**
     * Reset Hibernate statistics
     */
    public void resetHibernateStatistics() {
        try {
            SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
            Statistics stats = sessionFactory.getStatistics();
            stats.clear();
            logger.info("Hibernate statistics reset successfully");
        } catch (Exception e) {
            logger.error("Error resetting Hibernate statistics", e);
        }
    }

    /**
     * Check if database performance is healthy
     */
    public PerformanceHealthCheck checkPerformanceHealth() {
        DatabasePerformanceSummary summary = getPerformanceSummary();
        
        boolean isHealthy = true;
        StringBuilder issues = new StringBuilder();

        // Check connection pool utilization
        if (summary.getConnectionUtilization() > 80) {
            isHealthy = false;
            issues.append("High connection pool utilization (")
                   .append(String.format("%.1f", summary.getConnectionUtilization()))
                   .append("%); ");
        }

        // Check for threads waiting for connections
        if (summary.getConnectionPoolMetrics().getThreadsAwaitingConnection() > 0) {
            isHealthy = false;
            issues.append("Threads waiting for connections (")
                   .append(summary.getConnectionPoolMetrics().getThreadsAwaitingConnection())
                   .append("); ");
        }

        // Check query performance
        if (summary.getHibernateMetrics().getQueryExecutionMaxTime() > 5000) { // 5 seconds
            isHealthy = false;
            issues.append("Slow query detected (")
                   .append(summary.getHibernateMetrics().getQueryExecutionMaxTime())
                   .append("ms): ")
                   .append(summary.getHibernateMetrics().getSlowQueryString())
                   .append("; ");
        }

        // Check cache hit ratios
        if (summary.getQueryCacheHitRatio() < 70 && summary.getHibernateMetrics().getQueryCacheHitCount() > 0) {
            isHealthy = false;
            issues.append("Low query cache hit ratio (")
                   .append(String.format("%.1f", summary.getQueryCacheHitRatio()))
                   .append("%); ");
        }

        return new PerformanceHealthCheck(
            isHealthy,
            isHealthy ? "Database performance is healthy" : issues.toString(),
            summary
        );
    }

    /**
     * Get performance recommendations based on current metrics
     */
    public Map<String, String> getPerformanceRecommendations() {
        Map<String, String> recommendations = new HashMap<>();
        DatabasePerformanceSummary summary = getPerformanceSummary();

        if (summary.getConnectionUtilization() > 70) {
            recommendations.put("connection_pool", 
                "Consider increasing connection pool size or optimizing query performance");
        }

        if (summary.getQueryCacheHitRatio() < 80 && summary.getHibernateMetrics().getQueryCacheHitCount() > 0) {
            recommendations.put("query_cache", 
                "Enable query caching for frequently executed queries");
        }

        if (summary.getHibernateMetrics().getQueryExecutionMaxTime() > 1000) {
            recommendations.put("query_optimization", 
                "Review and optimize slow queries: " + summary.getHibernateMetrics().getSlowQueryString());
        }

        if (summary.getHibernateMetrics().getOptimisticFailureCount() > 0) {
            recommendations.put("concurrency", 
                "Review optimistic locking strategy due to concurrent update conflicts");
        }

        return recommendations;
    }

    // Data classes for metrics
    public static class ConnectionPoolMetrics {
        private final int totalConnections;
        private final int activeConnections;
        private final int idleConnections;
        private final int threadsAwaitingConnection;
        private final int maxPoolSize;
        private final int minIdle;
        private final long connectionTimeout;
        private final long idleTimeout;
        private final long maxLifetime;

        public ConnectionPoolMetrics(int totalConnections, int activeConnections, int idleConnections,
                                   int threadsAwaitingConnection, int maxPoolSize, int minIdle,
                                   long connectionTimeout, long idleTimeout, long maxLifetime) {
            this.totalConnections = totalConnections;
            this.activeConnections = activeConnections;
            this.idleConnections = idleConnections;
            this.threadsAwaitingConnection = threadsAwaitingConnection;
            this.maxPoolSize = maxPoolSize;
            this.minIdle = minIdle;
            this.connectionTimeout = connectionTimeout;
            this.idleTimeout = idleTimeout;
            this.maxLifetime = maxLifetime;
        }

        // Getters
        public int getTotalConnections() { return totalConnections; }
        public int getActiveConnections() { return activeConnections; }
        public int getIdleConnections() { return idleConnections; }
        public int getThreadsAwaitingConnection() { return threadsAwaitingConnection; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public int getMinIdle() { return minIdle; }
        public long getConnectionTimeout() { return connectionTimeout; }
        public long getIdleTimeout() { return idleTimeout; }
        public long getMaxLifetime() { return maxLifetime; }
    }

    public static class HibernateMetrics {
        private final long queryExecutionCount;
        private final long queryExecutionMaxTime;
        private final String slowQueryString;
        private final long queryCacheHitCount;
        private final long queryCacheMissCount;
        private final long secondLevelCacheHitCount;
        private final long secondLevelCacheMissCount;
        private final long sessionOpenCount;
        private final long sessionCloseCount;
        private final long transactionCount;
        private final long successfulTransactionCount;
        private final long optimisticFailureCount;
        private final long flushCount;
        private final long connectCount;
        private final long prepareStatementCount;
        private final long closeStatementCount;

        public HibernateMetrics(long queryExecutionCount, long queryExecutionMaxTime, String slowQueryString,
                              long queryCacheHitCount, long queryCacheMissCount,
                              long secondLevelCacheHitCount, long secondLevelCacheMissCount,
                              long sessionOpenCount, long sessionCloseCount,
                              long transactionCount, long successfulTransactionCount,
                              long optimisticFailureCount, long flushCount,
                              long connectCount, long prepareStatementCount, long closeStatementCount) {
            this.queryExecutionCount = queryExecutionCount;
            this.queryExecutionMaxTime = queryExecutionMaxTime;
            this.slowQueryString = slowQueryString;
            this.queryCacheHitCount = queryCacheHitCount;
            this.queryCacheMissCount = queryCacheMissCount;
            this.secondLevelCacheHitCount = secondLevelCacheHitCount;
            this.secondLevelCacheMissCount = secondLevelCacheMissCount;
            this.sessionOpenCount = sessionOpenCount;
            this.sessionCloseCount = sessionCloseCount;
            this.transactionCount = transactionCount;
            this.successfulTransactionCount = successfulTransactionCount;
            this.optimisticFailureCount = optimisticFailureCount;
            this.flushCount = flushCount;
            this.connectCount = connectCount;
            this.prepareStatementCount = prepareStatementCount;
            this.closeStatementCount = closeStatementCount;
        }

        // Getters
        public long getQueryExecutionCount() { return queryExecutionCount; }
        public long getQueryExecutionMaxTime() { return queryExecutionMaxTime; }
        public String getSlowQueryString() { return slowQueryString; }
        public long getQueryCacheHitCount() { return queryCacheHitCount; }
        public long getQueryCacheMissCount() { return queryCacheMissCount; }
        public long getSecondLevelCacheHitCount() { return secondLevelCacheHitCount; }
        public long getSecondLevelCacheMissCount() { return secondLevelCacheMissCount; }
        public long getSessionOpenCount() { return sessionOpenCount; }
        public long getSessionCloseCount() { return sessionCloseCount; }
        public long getTransactionCount() { return transactionCount; }
        public long getSuccessfulTransactionCount() { return successfulTransactionCount; }
        public long getOptimisticFailureCount() { return optimisticFailureCount; }
        public long getFlushCount() { return flushCount; }
        public long getConnectCount() { return connectCount; }
        public long getPrepareStatementCount() { return prepareStatementCount; }
        public long getCloseStatementCount() { return closeStatementCount; }
    }

    public static class DatabasePerformanceSummary {
        private final ConnectionPoolMetrics connectionPoolMetrics;
        private final HibernateMetrics hibernateMetrics;
        private final double connectionUtilization;
        private final double queryCacheHitRatio;
        private final double secondLevelCacheHitRatio;
        private final double averageQueryTime;

        public DatabasePerformanceSummary(ConnectionPoolMetrics connectionPoolMetrics,
                                        HibernateMetrics hibernateMetrics,
                                        double connectionUtilization,
                                        double queryCacheHitRatio,
                                        double secondLevelCacheHitRatio,
                                        double averageQueryTime) {
            this.connectionPoolMetrics = connectionPoolMetrics;
            this.hibernateMetrics = hibernateMetrics;
            this.connectionUtilization = connectionUtilization;
            this.queryCacheHitRatio = queryCacheHitRatio;
            this.secondLevelCacheHitRatio = secondLevelCacheHitRatio;
            this.averageQueryTime = averageQueryTime;
        }

        // Getters
        public ConnectionPoolMetrics getConnectionPoolMetrics() { return connectionPoolMetrics; }
        public HibernateMetrics getHibernateMetrics() { return hibernateMetrics; }
        public double getConnectionUtilization() { return connectionUtilization; }
        public double getQueryCacheHitRatio() { return queryCacheHitRatio; }
        public double getSecondLevelCacheHitRatio() { return secondLevelCacheHitRatio; }
        public double getAverageQueryTime() { return averageQueryTime; }
    }

    public static class PerformanceHealthCheck {
        private final boolean isHealthy;
        private final String message;
        private final DatabasePerformanceSummary summary;

        public PerformanceHealthCheck(boolean isHealthy, String message, DatabasePerformanceSummary summary) {
            this.isHealthy = isHealthy;
            this.message = message;
            this.summary = summary;
        }

        // Getters
        public boolean isHealthy() { return isHealthy; }
        public String getMessage() { return message; }
        public DatabasePerformanceSummary getSummary() { return summary; }
    }
}