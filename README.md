# SuperBoot
集成了kit、dbkit、httpkit的欢乐包应用。以Boot模板的方式约定大于配置，快速开发应用。  
详情参考示例应用： myStation

### 从main函数极速启动  
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
