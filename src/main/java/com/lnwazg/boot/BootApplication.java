package com.lnwazg.boot;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;

import com.lnwazg.dbkit.jdbc.MyJdbc;
import com.lnwazg.dbkit.utils.DbKit;
import com.lnwazg.httpkit.filter.CtrlFilterChain;
import com.lnwazg.httpkit.server.HttpServer;
import com.lnwazg.kit.cache.redis.RedisClient;
import com.lnwazg.kit.converter.VC;
import com.lnwazg.kit.handlerseq.HandlerSequence;
import com.lnwazg.kit.job.JobLoader;
import com.lnwazg.kit.log.Logs;
import com.lnwazg.kit.platform.Platforms;
import com.lnwazg.kit.property.MultiPropFile;
import com.lnwazg.kit.property.PropertyUtils;
import com.lnwazg.kit.singleton.B;

/**
 * 借鉴SpringBoot，新增以Boot的方式启动<br>
 * 约定优于配置原则的终极应用
 * @author nan.li
 * @version 2018年9月20日
 */
public class BootApplication
{
    /**
     * 默认的应用配置名称
     */
    private static final String DEFAULT_APP_CONFIG_NAME = "application.properties";
    
    public static void run(Class<?> mainClazz, String[] args)
    {
        //版权字符画信息输出
        try
        {
            
            List<String> list = IOUtils.readLines(BootApplication.class.getClassLoader().getResourceAsStream("legal/powerfile.txt"), CharEncoding.UTF_8);
            for (String line : list)
            {
                System.out.println(line);
            }
            System.out.println();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        // 加载应用配置信息
        Map<String, String> appConfigs = PropertyUtils
            .load(BootApplication.class.getClassLoader().getResourceAsStream(DEFAULT_APP_CONFIG_NAME));
        
        //配置文件加密的处理策略
        //根据不同的平台，去指定的本地路径去加载相应的配置文件，避免敏感信息公开后泄露
        if (StringUtils.isNotEmpty(appConfigs.get("ds.config.encrypt.filename")))
        {
            //加密文件名称
            String encryptFileName = appConfigs.get("ds.config.encrypt.filename");
            Logs.i("安全化配置模块名称:" + encryptFileName);
            
            String secureMultiFilePath = null;
            if (Platforms.IS_MACOSX)
            {
                secureMultiFilePath = Platforms.USER_HOME + "/Desktop/secure.properties";
            }
            else if (Platforms.IS_WINDOWS)
            {
                secureMultiFilePath = "D:/secure.properties";
            }
            else if (Platforms.IS_LINUX)
            {
                secureMultiFilePath = Platforms.USER_HOME + "/secure.properties";
            }
            
            Logs.i("开始加载安全化配置总文件，路径为" + secureMultiFilePath);
            //文件名-内容 映射表
            Map<String, List<String>> fileNameContentMap = MultiPropFile.loadMultiPropFile(secureMultiFilePath, CharEncoding.UTF_8);
            if (fileNameContentMap == null)
            {
                Logs.e("本地安全化配置总文件" + secureMultiFilePath + "不存在，无法加载配置信息，请检查！");
                return;
            }
            Logs.i("安全化配置总文件加载完毕！");
            
            Logs.i("加载安全配置文件信息...");
            List<String> fileStrList = fileNameContentMap.get(encryptFileName);
            Map<String, String> subFileConfigs = PropertyUtils.load(fileStrList);
            Logs.i("安全配置文件信息加载完毕");
            appConfigs.putAll(subFileConfigs);
        }
        
        //加载完配置之后要做的事情
        if (StringUtils.isNotEmpty(appConfigs.get("app.tasklist.classpath.afterReadConfigs")))
        {
            HandlerSequence.executeTasksByTaskClazzPathSync(appConfigs.get("app.tasklist.classpath.afterReadConfigs"),
                appConfigs);
        }
        
        // 日志中是否打开时间戳记录
        Logs.TIMESTAMP_LOG_SWITCH = VC.of(appConfigs.get("server.timestamplog.switch")).booleanValue();
        // 文件日志开关
        Logs.FILE_LOG_SWITCH = VC.of(appConfigs.get("server.filelog.switch")).booleanValue();
        // 打印的数据库操作日志是否是详细模式
        DbKit.DEBUG_MODE = VC.of(appConfigs.get("server.log.dbdebugmode")).booleanValue();
        // 慢sql监控工具打开
        DbKit.SQL_MONITOR = VC.of(appConfigs.get("server.monitor.sql")).booleanValue();
        
        // 获取jdbc对象
        MyJdbc jdbc = DbKit.getJdbc(appConfigs.get("db.url"),
            appConfigs.get("db.username"),
            appConfigs.get("db.password"));
        B.s(MyJdbc.class, jdbc);
        
        //多数据源支持
        if (StringUtils.isNotEmpty(appConfigs.get("db.more.ds")))
        {
            String dsNames = appConfigs.get("db.more.ds");
            Logs.i("--------------------------***************************--------------------------");
            Logs.i("即将启动多数据源支持...");
            String[] dsNameArray = StringUtils.split(dsNames, ",");
            for (String dsName : dsNameArray)
            {
                Logs.i("开始初始化数据源:" + dsName + "...");
                B.s(MyJdbc.class,
                    dsName,
                    DbKit.getJdbc(appConfigs.get(dsName + ".db.url"),
                        appConfigs.get(dsName + ".db.username"),
                        appConfigs.get(dsName + ".db.password")));
            }
        }
        
        // 自动初始化表结构
        DbKit.packageSearchAndInitTables(appConfigs.get("mvc.packagesearch.entity"));
        
        // 初始化DAO层
        DbKit.packageSearchAndInitDao(appConfigs.get("mvc.packagesearch.dao"));
        
        // 初始化service层
        DbKit.packageSearchAndInitService(appConfigs.get("mvc.packagesearch.service"));
        
        // 起一个server实例，并初始化controller层
        int port = VC.of(appConfigs.get("server.port")).intValue();
        HttpServer server;
        try
        {
            server = HttpServer.bind(port);
            // server之所以没做成单例模式的，是因为我要支持多实例的server对象在一个项目中共存
            server.setControllerSuffix(appConfigs.get("mvc.controller.suffix"));
            if (StringUtils.isNotEmpty(appConfigs.get("server.contextPath")))
            {
                server.setBasePath(appConfigs.get("server.contextPath"));
            }
            // 读配置文件，初始化过滤器链条
            List<String> valueList = PropertyUtils.readConfigList(appConfigs, "mvc.filters.cfg");
            CtrlFilterChain ctrlFilterChain = server.initFilterConfigs(valueList);
            // 为controller设置动态代理
            // 最终初始化controller
            server.packageSearchAndInit(appConfigs.get("mvc.packagesearch.controller"), ctrlFilterChain);
            // 启动服务器，并监听在这个端口处
            server.listen();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        // 启动定时任务调度器
        boolean jobClusterSwitch = VC.of(appConfigs.get("app.job.clustersupport.switch")).booleanValue();
        if (jobClusterSwitch)
        {
            //多节点job loader
            if (StringUtils.isNotEmpty(appConfigs.get("app.job.packagesearch")))
            {
                JobLoader.multiNodeLoadPackageJob(appConfigs.get("app.job.packagesearch"),
                    new RedisClient(
                        appConfigs.get("app.job.clustersupport.redis.ip"),
                        VC.of(appConfigs.get("app.job.clustersupport.redis.port")).intValue()),
                    appConfigs.get("app.job.clustersupport.appId"));
            }
        }
        else
        {
            //单节点job loader
            if (StringUtils.isNotEmpty(appConfigs.get("app.job.packagesearch")))
            {
                JobLoader.loadPackageJob(appConfigs.get("app.job.packagesearch"));
            }
        }
        
        // 启动后立即要做的一系列事情
        if (StringUtils.isNoneEmpty(appConfigs.get("app.tasklist.classpath.afterboot")))
        {
            HandlerSequence.executeTasksByTaskClazzPathAsync(appConfigs.get("app.tasklist.classpath.afterboot"));
        }
    }
}
