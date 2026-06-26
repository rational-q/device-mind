package com.devicemind.core.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * 多数据源 MyBatis-Plus 配置
 * <p>
 * MySQL（主数据源）：存储设备元数据，mapper 包 com.devicemind.core.persistence.mapper.mysql
 * TimescaleDB：存储设备时序数据，mapper 包 com.devicemind.core.persistence.mapper.timescale
 * <p>
 * 使用两个 {@link MybatisSqlSessionFactoryBean} 分别创建 SqlSessionFactory，
 * 通过内部类上的 @MapperScan 将不同包的 Mapper 绑定到对应的 SqlSessionFactory。
 */
@Configuration
public class DataSourceConfig {

    /**
     * 构建统一的 MyBatis-Plus 全局配置：自动填充 + 下划线转驼峰 + SQL 日志。
     * <p>
     * 手动创建的 SqlSessionFactory 不会读取 application.yml 中的 mybatis-plus.*，
     * 故在此显式装配，两个数据源共用同一份配置策略。
     */
    private static MybatisConfiguration buildMybatisConfiguration() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(Slf4jImpl.class);
        return configuration;
    }

    private static GlobalConfig buildGlobalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMetaObjectHandler(new MybatisPlusMetaObjectHandler());
        return globalConfig;
    }

    /**
     * 分页拦截器，按数据源方言区分（MySQL / PostgreSQL）。
     */
    private static MybatisPlusInterceptor paginationInterceptor(DbType dbType) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));
        return interceptor;
    }

    // ==================== 数据源 Bean ====================

    /**
     * MySQL 数据源（主数据源，@Primary）
     * <p>
     * 连接池参数绑定 spring.datasource.mysql.hikari.* 子节点。
     */
    @Primary
    @Bean(name = "mysqlDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.mysql.hikari")
    public DataSource mysqlDataSource() {
        return mysqlDataSourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    @Primary
    @Bean(name = "mysqlDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.mysql")
    public DataSourceProperties mysqlDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * TimescaleDB 数据源（时序数据存储）
     * <p>
     * 连接池参数绑定 spring.datasource.timescaledb.hikari.* 子节点。
     */
    @Bean(name = "timescaleDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.timescaledb.hikari")
    public DataSource timescaleDataSource() {
        return timescaleDataSourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    @Bean(name = "timescaleDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.timescaledb")
    public DataSourceProperties timescaleDataSourceProperties() {
        return new DataSourceProperties();
    }

    // ==================== MySQL MyBatis-Plus 配置 ====================

    @Configuration
    @MapperScan(
            basePackages = "com.devicemind.core.persistence.mapper.mysql",
            sqlSessionFactoryRef = "mysqlSqlSessionFactory",
            sqlSessionTemplateRef = "mysqlSqlSessionTemplate"
    )
    public static class MySqlMyBatisConfig {

        @Primary
        @Bean(name = "mysqlSqlSessionFactory")
        public SqlSessionFactory sqlSessionFactory(
                @Qualifier("mysqlDataSource") DataSource dataSource) throws Exception {
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(dataSource);
            factory.setConfiguration(buildMybatisConfiguration());
            factory.setGlobalConfig(buildGlobalConfig());
            factory.setPlugins(paginationInterceptor(DbType.MYSQL));
            factory.setMapperLocations(
                    new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/mysql/**/*.xml"));
            return factory.getObject();
        }

        @Primary
        @Bean(name = "mysqlSqlSessionTemplate")
        public SqlSessionTemplate sqlSessionTemplate(
                @Qualifier("mysqlSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
            return new SqlSessionTemplate(sqlSessionFactory);
        }

        @Primary
        @Bean(name = "mysqlTransactionManager")
        public DataSourceTransactionManager transactionManager(
                @Qualifier("mysqlDataSource") DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    // ==================== TimescaleDB MyBatis-Plus 配置 ====================

    @Configuration
    @MapperScan(
            basePackages = "com.devicemind.core.persistence.mapper.timescale",
            sqlSessionFactoryRef = "timescaleSqlSessionFactory",
            sqlSessionTemplateRef = "timescaleSqlSessionTemplate"
    )
    public static class TimescaleMyBatisConfig {

        @Bean(name = "timescaleSqlSessionFactory")
        public SqlSessionFactory sqlSessionFactory(
                @Qualifier("timescaleDataSource") DataSource dataSource) throws Exception {
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(dataSource);
            factory.setConfiguration(buildMybatisConfiguration());
            factory.setGlobalConfig(buildGlobalConfig());
            factory.setPlugins(paginationInterceptor(DbType.POSTGRE_SQL));
            factory.setMapperLocations(
                    new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/timescale/**/*.xml"));
            return factory.getObject();
        }

        @Bean(name = "timescaleSqlSessionTemplate")
        public SqlSessionTemplate sqlSessionTemplate(
                @Qualifier("timescaleSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
            return new SqlSessionTemplate(sqlSessionFactory);
        }

        @Bean(name = "timescaleTransactionManager")
        public DataSourceTransactionManager transactionManager(
                @Qualifier("timescaleDataSource") DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
