import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class TableConstantGenerator
{

	public static void main(String[] args) throws Exception
	{
		try
		{

			Properties properties = new Properties();
			properties.load(Files.newInputStream(Path.of(System.getProperty("user.dir") + "/custom/application-custom.properties")));


			String jdbcUrl = properties.getProperty("spring.datasource.url");
			String dbUser = properties.getProperty("spring.datasource.username");
			String dbPassword = properties.getProperty("spring.datasource.password");

			Connection connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);

			DatabaseMetaData databaseMetaData = connection.getMetaData();

			List<TableMeta> tableMetaList = new ArrayList<>();

			List<String> tableList = new ArrayList<>();

			ResultSet rs = connection.createStatement().executeQuery("Show tables");
			while(rs.next())
			{
				tableList.add(rs.getString(1));
			}

			for(String tableName : tableList)
			{
				List<String> columnList = new ArrayList<>();
				ResultSet columnResultSet = databaseMetaData.getColumns(null, null, tableName, null);
				while(columnResultSet.next())
				{
					String columnName = columnResultSet.getString("COLUMN_NAME");
					columnList.add(columnName);
				}

				tableMetaList.add(new TableMeta(tableName, columnList));
			}

			if(tableMetaList.isEmpty())
			{
				System.out.println("No tables found in the database. Table constants generation skipped.");
				return;
			}

			VelocityEngine velocityEngine = new VelocityEngine();
			velocityEngine.setProperty("resource.loader", "class");
			velocityEngine.setProperty("class.resource.loader.class",
				"org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
			velocityEngine.init();

			String packageName = "com.server.table.constants";
			String outputDir = System.getProperty("user.dir") + "/table-constants/target/temp/";
			System.out.println("Output Directory: " + outputDir);

			for(TableMeta tableMeta : tableMetaList)
			{
				VelocityContext context = new VelocityContext();
				context.put("tableMeta", tableMeta);
				context.put("pkg", packageName);

				Template template = velocityEngine.getTemplate("ClassTemplate.vtl");

				File file = new File(outputDir + packageName.replaceAll("\\.", "/"));
				file.mkdirs();
				Writer writer = new FileWriter(file.getAbsolutePath() + "/" + tableMeta.getFormattedTableName() + ".java");
				template.merge(context, writer);
				writer.flush();
				writer.close();
			}

			System.out.println("Constants Generated");
			connection.close();
		}
		catch(Exception e)
		{
			System.err.println("Warning: Could not connect to database to generate table constants: " + e.getMessage());
			System.err.println("Table constants generation skipped. You can generate them later when database is available.");
			System.out.println("Table constants generation completed with warnings.");
		}
	}
}
