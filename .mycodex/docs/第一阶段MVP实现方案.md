# 第一阶段 MVP 实现方案

## 1. 开发目标

第一阶段只实现本地记账核心闭环：

新增账单 -> 保存到 SQLite -> 列表展示 -> 首页汇总 -> 预算提醒。

不实现凭证上传、导出、复杂图表、自定义分类、自定义账户、账单编辑删除、登录注册和云同步。

## 2. 技术边界

- 语言：Java。
- UI：Android 原生 View + XML。
- 页面：`MainActivity` + 3 个 Fragment。
- 数据库：`SQLiteOpenHelper`。
- 轻量配置：`SharedPreferences`。
- 列表：`ListView + BaseAdapter`。
- 不新增第三方依赖。
- 不引入 Room、Compose、Kotlin 或大型架构框架。

## 3. 目录设计

Java 代码目录：

```text
app/src/main/java/com/example/accountbook/
├─ MainActivity.java
├─ adapter/
│  ├─ AccountAdapter.java
│  └─ BillRecordAdapter.java
├─ db/
│  ├─ AccountBookDbHelper.java
│  ├─ AccountDao.java
│  ├─ BillRecordDao.java
│  └─ CategoryDao.java
├─ fragment/
│  ├─ AddBillFragment.java
│  ├─ HomeFragment.java
│  └─ MineFragment.java
├─ model/
│  ├─ Account.java
│  ├─ BillRecord.java
│  └─ Category.java
└─ util/
   ├─ DateUtils.java
   ├─ MoneyUtils.java
   └─ PreferenceUtils.java
```

资源目录：

```text
app/src/main/res/layout/
├─ activity_main.xml
├─ fragment_home.xml
├─ fragment_add_bill.xml
├─ fragment_mine.xml
├─ item_bill_record.xml
└─ item_account.xml
```

必要时再增加 `drawable` 形状背景、`values/dimens.xml` 和字符串资源。

## 4. 页面方案

### 4.1 MainActivity

职责：

- 承载 3 个 Fragment。
- 提供底部导航。
- 管理首页、记账、我的页面切换。
- 记账保存成功后触发数据刷新。

### 4.2 HomeFragment

展示：

- 本月收入。
- 本月支出。
- 本月结余。
- 月度预算进度。
- 预算提醒文案。
- 最近 10 条账单。

数据来源：

- `BillRecordDao` 查询本月收入、支出、最近账单。
- `PreferenceUtils` 读取月度预算和提醒开关。

### 4.3 AddBillFragment

功能：

- 切换收入 / 支出。
- 输入金额。
- 选择分类。
- 选择账户。
- 选择日期。
- 输入备注。
- 保存账单。

保存流程：

1. 校验金额、分类、账户、日期。
2. 创建 `BillRecord`。
3. 调用 `BillRecordDao.insertBillRecord()`。
4. 在同一数据库事务中更新账户余额。
5. 通知首页和我的页刷新。

### 4.4 MineFragment

功能：

- 设置月度预算。
- 设置是否开启预算提醒。
- 展示默认账户余额。

数据来源：

- `PreferenceUtils` 读取和保存预算配置。
- `AccountDao` 查询账户列表。

## 5. 数据库方案

数据库名：

```text
account_book.db
```

版本：

```text
1
```

### 5.1 bill_record

```sql
CREATE TABLE bill_record (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  type TEXT NOT NULL,
  amount REAL NOT NULL,
  category_id INTEGER NOT NULL,
  account_id INTEGER NOT NULL,
  record_date TEXT NOT NULL,
  remark TEXT,
  create_time INTEGER NOT NULL
);
```

### 5.2 category

```sql
CREATE TABLE category (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  type TEXT NOT NULL,
  sort_order INTEGER NOT NULL
);
```

默认数据：

- 支出：餐饮、交通、购物、娱乐、学习、其他。
- 收入：生活费、工资、兼职、红包、其他。

### 5.3 account

```sql
CREATE TABLE account (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  account_type TEXT NOT NULL,
  balance REAL NOT NULL DEFAULT 0
);
```

默认数据：

- 现金：`cash`。
- 银行卡：`bank_card`。
- 信用卡：`credit_card`。

## 6. DAO 设计

### 6.1 BillRecordDao

职责：

- 新增账单。
- 查询最近账单。
- 查询本月收入合计。
- 查询本月支出合计。
- 查询账单列表展示需要的分类名、账户名。
- 保存账单时同步更新账户余额。

关键方法：

```java
long insertBillRecord(BillRecord record);
List<BillRecord> getRecentBillRecords(int limit);
double getMonthlyTotal(String type, String monthStart, String monthEnd);
```

### 6.2 CategoryDao

职责：

- 按收入 / 支出类型查询分类。
- 初始化默认分类。

关键方法：

```java
List<Category> getCategoriesByType(String type);
```

### 6.3 AccountDao

职责：

- 查询账户列表。
- 更新账户余额。
- 初始化默认账户。

关键方法：

```java
List<Account> getAllAccounts();
void updateBalance(long accountId, double delta);
```

## 7. SharedPreferences 方案

文件名：

```text
budget_config
```

字段：

```text
monthly_budget: float
budget_warn_enabled: boolean
```

默认值：

- `monthly_budget = 0`
- `budget_warn_enabled = true`

当预算金额小于等于 0 时，首页显示“未设置预算”，不计算百分比。

## 8. 业务规则

### 8.1 金额校验

- 不能为空。
- 必须是合法数字。
- 必须大于 0。

### 8.2 分类规则

- 支出只能展示支出分类。
- 收入只能展示收入分类。
- 切换收支类型后重新加载分类。

### 8.3 账户余额

- 新增收入：账户余额增加。
- 新增支出：账户余额减少。
- 账单写入和余额更新放在同一个事务中，避免数据不一致。

### 8.4 首页统计

- 本月收入 = 当前自然月收入总和。
- 本月支出 = 当前自然月支出总和。
- 本月结余 = 本月收入 - 本月支出。
- 最近账单默认最多 10 条。

### 8.5 预算预警

- 预算只统计支出。
- 支出低于预算 80%：正常。
- 支出达到 80% 且低于 100%：显示“本月预算已使用较多”。
- 支出达到或超过 100%：显示“本月预算已超出”。
- 关闭提醒开关时，只显示进度，不显示提醒或预警文案。

## 9. 开发顺序

### Step 1：搭建页面骨架

- 修改 `MainActivity`。
- 新增 3 个 Fragment。
- 新增 3 个页面布局。
- 完成页面切换。

验收：

- App 启动进入首页。
- 可以切换首页、记账、我的三个页面。

### Step 2：实现数据库和默认数据

- 新增 Model。
- 新增 `AccountBookDbHelper`。
- 创建三张表。
- 初始化默认分类和默认账户。
- 新增 DAO 查询方法。

验收：

- 启动 App 后数据库创建成功。
- 默认分类和默认账户可查询。

### Step 3：实现记账保存

- 记账页加载分类和账户。
- 实现输入校验。
- 实现保存账单。
- 实现账户余额联动。

验收：

- 可以新增收入。
- 可以新增支出。
- 账户余额正确变化。
- 非法金额无法保存。

### Step 4：实现首页汇总和最近账单

- 首页查询本月收入、支出、结余。
- 首页展示最近 10 条账单。
- 保存账单后刷新首页。

验收：

- 新增账单后首页统计变化。
- 最近账单列表正确展示。

### Step 5：实现预算设置和提醒

- 我的页保存预算金额和提醒开关。
- 首页读取预算配置。
- 首页计算预算进度。
- 首页显示 80% 和 100% 状态。

验收：

- 预算设置可保存。
- 重启 App 后预算仍存在。
- 预算提醒状态正确。

### Step 6：构建和手工验证

- 运行 Gradle 构建。
- 在模拟器或真机验证核心流程。
- 根据验证结果修复问题。

## 10. 风险和处理

- 当前 `阶段执行TODO.md` 曾记录错误完成状态，后续以当前源码和本方案为准。
- 第一阶段不新增依赖，减少 Gradle 同步风险。
- 数据库结构在第一阶段尽量一次确定，后续变更需要升级版本并写迁移逻辑。
- 金额使用 `double` 满足课程 MVP 演示，后续若做严谨财务计算可改为分单位整数存储。
