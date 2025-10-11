package com.server.zoho.component;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

import com.server.framework.common.AppProperties;
import com.server.framework.entity.UserEntity;
import com.server.framework.service.AuthTokenService;
import com.server.framework.service.ConfigurationService;
import com.server.framework.service.UserService;
import com.server.framework.user.RoleEnum;

@Component
public class DatabaseMigrationComponent implements ApplicationRunner
{

	private static final Logger LOGGER = Logger.getLogger(DatabaseMigrationComponent.class.getName());

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private UserService userService;

	@Autowired
	private AuthTokenService authTokenService;

	@Autowired
	private ConfigurationService configurationService;

	@Override
	@Transactional
	public void run(ApplicationArguments args) throws Exception
	{
		if(userService.findAll().isEmpty())
		{
			LOGGER.info("Cold start detected - running database migrations...");

			if(AppProperties.getProperty("environment").equals("zoho"))
			{
				userService.createUser("admin", "4aca328a7942dc649ecfadff9c3dbfb5d95828b2a13ca0d45878c0f3d3e894d8", RoleEnum.ADMIN.getType());
				configurationService.setValue("zoho.critical.operation.allowed.users", AppProperties.getProperty("zoho.critical.operation.allowed.users"));
				return;
			}

			userService.createUser("admin", "7676aaafb027c825bd9abab78b234070e702752f625b752e55e55b48e607e358", RoleEnum.ADMIN.getType());
			UserEntity testUserEntity = userService.createUser("test", "8622f0f69c91819119a8acf60a248d7b36fdb7ccf857ba8f85cf7f2767ff8265", RoleEnum.NORMAL.getType());

			authTokenService.createToken(testUserEntity, "8622f0f69c91819119a8acf60a248d7b36fdb7ccf857ba8f85cf7f2767ff8265");
		}
	}

	private void runMigrationForBuildMonitorTable()
	{
		LOGGER.info("Starting database migration for BuildMonitor table...");

		try
		{
			String checkTableSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BuildMonitor'";
			Integer tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class);

			if(tableCount == null || tableCount == 0)
			{
				LOGGER.info("BuildMonitor table does not exist yet - skipping migration");
				return;
			}

			LOGGER.info("BuildMonitor table exists - checking for columns to migrate...");

			String checkRemainingProductsSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BuildMonitor' AND COLUMN_NAME = 'RemainingProducts'";
			Integer remainingProductsCount = jdbcTemplate.queryForObject(checkRemainingProductsSql, Integer.class);

			if(remainingProductsCount != null && remainingProductsCount > 0)
			{
				LOGGER.info("MIGRATION: Removing RemainingProducts column from BuildMonitor table");
				jdbcTemplate.execute("ALTER TABLE BuildMonitor DROP COLUMN RemainingProducts");
				LOGGER.info("MIGRATION: RemainingProducts column removed successfully");
			}
			else
			{
				LOGGER.info("MIGRATION: RemainingProducts column does not exist - already removed");
			}

			String checkCurrentBuildIdSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BuildMonitor' AND COLUMN_NAME = 'CurrentBuildId'";
			Integer currentBuildIdCount = jdbcTemplate.queryForObject(checkCurrentBuildIdSql, Integer.class);

			if(currentBuildIdCount != null && currentBuildIdCount > 0)
			{
				LOGGER.info("MIGRATION: Removing CurrentBuildId column from BuildMonitor table");
				jdbcTemplate.execute("ALTER TABLE BuildMonitor DROP COLUMN CurrentBuildId");
				LOGGER.info("MIGRATION: CurrentBuildId column removed successfully");
			}
			else
			{
				LOGGER.info("MIGRATION: CurrentBuildId column does not exist - already removed");
			}

			String checkCurrentProductSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BuildMonitor' AND COLUMN_NAME = 'CurrentProduct'";
			Integer currentProductCount = jdbcTemplate.queryForObject(checkCurrentProductSql, Integer.class);

			if(currentProductCount != null && currentProductCount > 0)
			{
				LOGGER.info("MIGRATION: Removing CurrentProduct column from BuildMonitor table");
				jdbcTemplate.execute("ALTER TABLE BuildMonitor DROP COLUMN CurrentProduct");
				LOGGER.info("MIGRATION: CurrentProduct column removed successfully");
			}
			else
			{
				LOGGER.info("MIGRATION: CurrentProduct column does not exist - already removed");
			}

			String checkUserEmailSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BuildMonitor' AND COLUMN_NAME = 'UserEmail'";
			Integer userEmailCount = jdbcTemplate.queryForObject(checkUserEmailSql, Integer.class);

			if(userEmailCount != null && userEmailCount > 0)
			{
				LOGGER.info("MIGRATION: Removing UserEmail column from BuildMonitor table");
				jdbcTemplate.execute("ALTER TABLE BuildMonitor DROP COLUMN UserEmail");
				LOGGER.info("MIGRATION: UserEmail column removed successfully");
			}
			else
			{
				LOGGER.info("MIGRATION: UserEmail column does not exist - already removed");
			}

			LOGGER.info("MIGRATION: BuildMonitor table migration completed successfully");

		}
		catch(Exception e)
		{
			LOGGER.severe("MIGRATION: Failed to migrate BuildMonitor table: " + e.getMessage());
			LOGGER.log(java.util.logging.Level.SEVERE, "Exception in DatabaseMigrationComponent", e);
		}
	}

	private void runMigrationForWorkFlowInstanceTable()
	{
		LOGGER.info("Starting database migration for WorkFlowInstance table...");

		try
		{
			String checkTableSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'WorkflowInstance'";
			Integer tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class);

			if(tableCount == null || tableCount == 0)
			{
				LOGGER.info("WorkFlowInstance table does not exist yet - skipping migration");
				return;
			}

			LOGGER.info("WorkFlowInstance table exists - checking for columns to migrate...");

			String checkInstanceIdColumnSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'WorkflowInstance' AND COLUMN_NAME = 'InstanceId'";
			Integer instanceIdColumnCount = jdbcTemplate.queryForObject(checkInstanceIdColumnSql, Integer.class);

			if(instanceIdColumnCount != null && instanceIdColumnCount > 0)
			{
				LOGGER.info("MIGRATION: Renaming InstanceId column to ReferenceID in WorkflowInstance table");
				jdbcTemplate.execute("ALTER TABLE WorkflowInstance CHANGE COLUMN InstanceId ReferenceID VARCHAR(255) NOT NULL");
				LOGGER.info("MIGRATION: InstanceId column renamed to ReferenceID successfully");
			}
			else
			{
				LOGGER.info("MIGRATION: InstanceId column does not exist - checking if ReferenceID already exists");
				String checkReferenceIdColumnSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'WorkflowInstance' AND COLUMN_NAME = 'ReferenceID'";
				Integer referenceIdColumnCount = jdbcTemplate.queryForObject(checkReferenceIdColumnSql, Integer.class);
				if(referenceIdColumnCount == null || referenceIdColumnCount == 0)
				{
					LOGGER.info("MIGRATION: ReferenceID column does not exist - table may need to be recreated");
				}
			}

			String checkBuildMonitorIdColumnSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'WorkflowInstance' AND COLUMN_NAME = 'BuildMonitorId'";
			Integer buildMonitorIdColumnCount = jdbcTemplate.queryForObject(checkBuildMonitorIdColumnSql, Integer.class);

			if(buildMonitorIdColumnCount != null && buildMonitorIdColumnCount > 0)
			{
				LOGGER.info("MIGRATION: Removing BuildMonitorId column from WorkflowInstance table");
				jdbcTemplate.execute("ALTER TABLE WorkflowInstance DROP FOREIGN KEY FK5mq6baqlmnc1lkdy7xyk6aurx");
				jdbcTemplate.execute("ALTER TABLE WorkflowInstance DROP COLUMN BuildMonitorId");
				LOGGER.info("MIGRATION: BuildMonitorId column removed successfully");
			}
			else
			{
				LOGGER.info("MIGRATION: BuildMonitorId column does not exist - already removed");
			}

			String checkIdColumnSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'WorkflowInstance' AND COLUMN_NAME = 'Id'";
			Integer idColumnCount = jdbcTemplate.queryForObject(checkIdColumnSql, Integer.class);

			if(idColumnCount != null && idColumnCount > 0)
			{
				LOGGER.info("MIGRATION: Removing Id column from WorkflowInstance table (ReferenceID is now primary key)");
				jdbcTemplate.execute("ALTER TABLE WorkflowInstance DROP COLUMN Id");
				LOGGER.info("MIGRATION: Id column removed successfully");
			}
			else
			{
				LOGGER.info("MIGRATION: Id column does not exist - already removed");
			}

			LOGGER.info("MIGRATION: WorkflowInstance table migration completed successfully");

			migrateBuildMonitorTable();

		}
		catch(Exception e)
		{
			LOGGER.severe("MIGRATION: Failed to migrate WorkflowInstance table: " + e.getMessage());
			LOGGER.log(java.util.logging.Level.SEVERE, "Exception in DatabaseMigrationComponent", e);
		}
	}

	private void migrateBuildMonitorTable()
	{
		try
		{
			LOGGER.info("MIGRATION: Starting BuildMonitor table migration...");

			String checkTableSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BuildMonitor'";
			Integer tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class);

			if(tableCount == null || tableCount == 0)
			{
				LOGGER.info("MIGRATION: BuildMonitor table does not exist yet - skipping migration");
				return;
			}

			String checkWorkflowInstanceIdColumnSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BuildMonitor' AND COLUMN_NAME = 'WorkflowInstanceId'";
			Integer workflowInstanceIdColumnCount = jdbcTemplate.queryForObject(checkWorkflowInstanceIdColumnSql, Integer.class);

			if(workflowInstanceIdColumnCount != null && workflowInstanceIdColumnCount > 0)
			{
				LOGGER.info("MIGRATION: Removing WorkflowInstanceId column from BuildMonitor table");

				jdbcTemplate.execute("ALTER TABLE BuildMonitor DROP FOREIGN KEY fk_buildmonitor_workflow_instance");

				jdbcTemplate.execute("DROP INDEX IF EXISTS idx_buildmonitor_workflow_instance_id ON BuildMonitor");

				jdbcTemplate.execute("ALTER TABLE BuildMonitor DROP COLUMN WorkflowInstanceId");

				LOGGER.info("MIGRATION: WorkflowInstanceId column removed successfully from BuildMonitor table");
			}
			else
			{
				LOGGER.info("MIGRATION: WorkflowInstanceId column does not exist in BuildMonitor table - no migration needed");
			}

			LOGGER.info("MIGRATION: BuildMonitor table migration completed successfully");
		}
		catch(Exception e)
		{
			LOGGER.severe("MIGRATION: Failed to migrate BuildMonitor table: " + e.getMessage());
			LOGGER.log(java.util.logging.Level.SEVERE, "Exception in migrateBuildMonitorTable", e);
		}
	}
}
