package com.lnwazg.boot;

import java.io.IOException;
import java.util.Map;

import com.lnwazg.dbkit.jdbc.MyJdbc;
import com.lnwazg.dbkit.utils.DbKit;
import com.lnwazg.httpkit.filter.CtrlFilterChain;
import com.lnwazg.httpkit.server.HttpServer;
import com.lnwazg.kit.converter.VC;
import com.lnwazg.kit.job.JobLoader;
import com.lnwazg.kit.log.Logs;
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
        //加载应用配置信息
        Map<String, String> appConfigs = PropertyUtils.load(BootApplication.class.getClassLoader().getResourceAsStream(DEFAULT_APP_CONFIG_NAME));
        
        // 日志中是否打开时间戳记录
        Logs.TIMESTAMP_LOG_SWITCH = VC.of(appConfigs.get("server.timestamplog.switch")).booleanValue();
        // 文件日志开关
        Logs.FILE_LOG_SWITCH = VC.of(appConfigs.get("server.filelog.switch")).booleanValue();
        // 打印的数据库操作日志是否是详细模式
        DbKit.DEBUG_MODE = VC.of(appConfigs.get("server.log.dbdebugmode")).booleanValue();
        // 慢sql监控工具打开
        DbKit.SQL_MONITOR = VC.of(appConfigs.get("server.monitor.sql")).booleanValue();
        
        // 获取jdbc对象
        MyJdbc jdbc = DbKit.getJdbc(appConfigs.get("db.url"), appConfigs.get("db.username"), appConfigs.get("db.password"));
        B.s(MyJdbc.class, jdbc);
        
        // 自动初始化表结构
        DbKit.packageSearchAndInitTables(appConfigs.get("mvc.packagesearch.entity"), jdbc);
        
        // 初始化DAO层
        DbKit.packageSearchAndInitDao(appConfigs.get("mvc.packagesearch.dao"), jdbc);
        
        // 初始化service层
        DbKit.packageSearchAndInitService(appConfigs.get("mvc.packagesearch.service"), jdbc);
        
        // 起一个server实例，并初始化controller层
        int port = VC.of(appConfigs.get("server.port")).intValue();
        HttpServer server;
        try
        {
            server = HttpServer.bind(port);
            // server之所以没做成单例模式的，是因为我要支持多实例的server对象在一个项目中共存
            server.setControllerSuffix(appConfigs.get("mvc.controller.suffix"));
            // 读配置文件，初始化过滤器链条
            CtrlFilterChain ctrlFilterChain = server.initFilterConfigs();
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
        //启动定时任务调度器
        JobLoader.loadPackageJob(appConfigs.get("mvc.packagesearch.job"));
    }
}
