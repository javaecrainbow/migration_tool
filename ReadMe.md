## PaaS_migration
### 介绍
    migration_tool 是基于flyway社区版开源项目进行二次改造支持undo功能及批量执行功能的一个数据迁移工具
---
### 技术栈
* flyway:7.15.0 **(7.15.0是社区版目前支持mysql5.7的最高版本)**
* springboot:2.3.2.RELEASE
* spring-shell:2.0.0.RELEASE
---
### 快速使用
* info
   <br>查看当前的迁移版本情况及状态
* migration
   <br>执行迁移操作
* undo
  <br>回滚
    * SQL模式
   ```
    undo {模块名}/{undoSqlFileName}.sql
    ```
    * JDBC模式
  ```
  undo {模块名}/{undoJavaFileName}
  ```
* stop
  <br>退出工具
* repair
  <br>错误修复
    * 对执行失败的记录进行移除,继续执行后续版本的执行
    * 对变更过的已经执行的脚本做checksum的修改
* env
 <br>查看当前的运行环境
 ---
## 功能支持
* SQL模式
 <br>SQL 声明式执行模式
    * 支持placeholder替换
    * 支持#{entId}动态数据集获取批量执行
* JDBC模式
   <br>编程式迁移执行模式
    * 支持spring bean的获取
    * configuration配置的获取
* SQL_UNDO 模式
  <br>SQL 声明式的回滚模式
* JDBC_UNDO 模式
   <br>编程式的JDBC的回滚模式
* sys 模式
  <br>sys模式即需要在一个指定环境上执行，另外的环境不做重复执行
* repeat 执行
  <br>每次做迁移的时候都执行
   ---
## 版本说明
* 版本需要递增 : 采用左对齐原则, 缺位用 0 代替
>  * 1.0.1.1 比 1.0.1 版本高。
> * 1.0.10 比 1.0.9.4 版本高。
> * 1.0.10 和 1.0.010 版本号一样高, 每个版本号部分的前导 0 会被忽略。

## 可用参数说明
<br>待补充
 ---
 ## todo
- [ ] stop的支持
- [X] 日志详细优化
- [ ] 回滚支持批量
 ---


