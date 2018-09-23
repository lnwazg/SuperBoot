# SuperBoot
集成了kit、dbkit、httpkit的欢乐包应用。以Boot模板的方式启动，遵循约定大于配置，开启极速应用开发体验。  
媲美SpringBoot，但更精致小巧，web开发的一站式解决方案！    
详情参考示例应用： myStation

### 从main函数极速启动：  
```  
public class BootMain
{
    public static void main(String[] args)
    {
        BootApplication.run(BootMain.class, args);
    }
}
```  


### 配置文件示例 (application.properties)
```
#服务器端口号
server.port=8080

#日志前缀是否带时间戳
server.timestamplog.switch=true
#日志是否输出到文件
server.filelog.switch=true
#数据库操作日志是否是详细模式
server.log.dbdebugmode=true
#数据库慢sql监控工具
server.monitor.sql=true

#数据库地址
db.url=jdbc:sqlite://c:/myStation.db
#数据库用户名
db.username=
#数据库密码
db.password=

#mvc Controller的访问后缀
mvc.controller.suffix=do
#实体类扫描包
mvc.packagesearch.entity=com.lnwazg.entity
#DAO扫描包
mvc.packagesearch.dao=com.lnwazg.dao
#Service扫描包
mvc.packagesearch.service=com.lnwazg.service
#controller扫描包
mvc.packagesearch.controller=com.lnwazg.controller
#job扫描包
mvc.packagesearch.job=com.lnwazg.job

#启动后任务列表驱动类
afterboot.tasklist.classpath=com.lnwazg.afterstart.TaskList
```

### 多数据源支持：  
##### 1.首先在配置文件中启用多数据源  
```
#以下为默认数据源配置（主数据源配置）
#数据库地址
db.url=jdbc:sqlite://c:/myStation.db
#数据库用户名
db.username=
#数据库密码
db.password=

#启用多数据源配置
db.more.ds=ds1,ds2,ds3

#额外数据源ds1
ds1.db.url=jdbc:sqlite://c:/ds1.db
ds1.db.username=
ds1.db.password=

#额外数据源ds2
ds2.db.url=jdbc:sqlite://c:/ds2.db
ds2.db.username=
ds2.db.password=

#额外数据源ds3
ds3.db.url=jdbc:sqlite://c:/ds3.db
ds3.db.username=
ds3.db.password=
```

##### 2.对于需要指定数据源的实体类，加入注解@DataSource;若无注解，则使用默认数据源    
```
@DataSource("ds1")
public class WorkInfo
{
	...
}
```

##### 3.对需要指定数据源的DAO，加入注解@DataSource;若无注解，则使用默认数据源  
```
@DataSource("ds1")
public interface WorkInfoDao extends MyJdbc
{
	...
}
```

##### 4.对于Service层，可根据成员变量DAO自动选择数据源；当然也可以采用注解的方式显式指定数据源  
当你的成员变量DAO有多个的时候，无法自适应选择数据源，此时就必须使用注解的方式指定数据源

自适应数据源的方式：
```
public class WorkInfoService {
	private WorkInfoDao workInfoDao = B.g(WorkInfoDao.class);

	@Transactional
	public void saveWorkInfo(WorkInfo workInfo) throws SQLException {
		//此处引用了workInfoDao，而workInfoDao使用了@DataSource("ds1")，那么切面事务管理也将自动使用ds1作为数据源
		workInfoDao.insert(workInfo);
	}

	public List<WorkInfo> queryBycontent(String content) {
		return workInfoDao.queryByContent(content);
	}
}
```

手动指定数据源的方式：

```
public class WorkInfoService {
	private WorkInfoDao workInfoDao = B.g(WorkInfoDao.class);

	@Transactional(dsName="ds1")
	public void saveWorkInfo(WorkInfo workInfo) throws SQLException {
		workInfoDao.insert(workInfo);
	}

	public List<WorkInfo> queryBycontent(String content) {
		return workInfoDao.queryByContent(content);
	}
}
```























