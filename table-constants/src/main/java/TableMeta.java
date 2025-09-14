import java.util.List;

public class TableMeta
{
	private String tableName;
	private List<String> columnList;
	private String formattedTableName;

	public TableMeta(String tableName, List<String> columnList)
	{
		super();
		this.tableName = tableName;
		this.formattedTableName = tableName.toUpperCase().replaceAll("_", "");
		this.columnList = columnList;
	}

	public String getTableName()
	{
		return tableName;
	}

	public List<String> getColumnList()
	{
		return columnList;
	}

	public String getFormattedTableName()
	{
		return formattedTableName;
	}
}
