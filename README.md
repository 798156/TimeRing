# 时环 — 专注学习助手

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_logo.png" width="120" alt="时环 Logo" />
</p>

<p align="center">
  <strong>时环</strong> — 一款轻量、优雅的 Android 学习专注 App。<br />
  计时 · 待办 · 统计 · 签到，陪你度过每一个专注的时刻。
</p>

<p align="center">
  <img src="https://img.shields.io/badge/platform-Android-green" />
  <img src="https://img.shields.io/badge/language-Kotlin-purple" />
  <img src="https://img.shields.io/badge/minSdk-24-blue" />
  <img src="https://img.shields.io/badge/UI-Material%20Components-orange" />
</p>

---

## ✨ 功能概览

### ⏱ 专注计时
- 预设科目：**数学、英语、背单词、专业课一、专业课二**
- 支持**自定义科目**添加与删除，自定义科目**持久化存储**
- 开始 / 暂停 / 继续 / 结束 完整计时流程
- 结束时展示**玻璃质感弹窗**，四舍五入取整分钟，< 30 秒不保存脏数据
- 背景散布**樱花简笔画花纹**（✿❀✾❁），若隐若现

### 📋 待办事项
- 新增 / 勾选完成 / 删除，**完成率进度条**实时更新
- **长按设置截止时间**，支持年月日时分精确选择
- 截止时间**提前 30 分钟**推送通知栏提醒
- 首次启动自动引导授权「闹钟和提醒」权限
- 逾期未完成 → 截止时间显示红色
- 完成自动取消闹钟

### 📊 统计数据
- **累计专注**：专注次数 / 总时长 / 日均时长
- **每日签到**：连续签到天数 / 累计签到次数 / 本周专注
- **当日专注**：今日次数及时长
- **专注时长分布**：科目饼图，支持**日 / 周 / 月**切换，**◀ ▶ 翻页查看历史数据**
- **学习时段热力图**：12 格 × 2 小时，GitHub 贡献图风格，支持**按天翻页**
- **本月每日专注**：柱状图，支持**翻月查看**，顶部摘要（总计 / 最高 / 日均）

### 👤 我的
- **每周摘要**：本周专注 vs 上周，🔥 增长 / 💪 不足对比
- **签到日历**：当月完整日历，今日粉色高亮，**已签到日期樱花印记**，◀ ▶ 切月
- **主题配色**：5 套主题一键切换（🌸 粉色 / 🍃 薄荷 / 🌊 天蓝 / 🍊 暖橘 / 🌙 暗夜）
- 清空数据 / 版本信息

### 📝 专注记录
- 所有历史记录列表，支持单条删除

### 🌸 全局设计
- **四页统一樱花线描花纹** 背景装饰
- **亚克力玻璃质感 Dock** 底部导航栏
- **玻璃质感弹窗** 截止时间 / 结束专注
- 跨主题的全属性动态配色（`?attr/colorPrimary` 等）

---

## 🎨 主题配色

| 主题 | 预览 | 主色 |
|------|------|------|
| 🌸 樱花粉 | 默认 | `#F48FB1` → `#EC407A` |
| 🍃 薄荷绿 | 清新 | `#81C784` → `#43A047` |
| 🌊 天空蓝 | 宁静 | `#64B5F6` → `#1E88E5` |
| 🍊 暖橘 | 活力 | `#FFB74D` → `#F57C00` |
| 🌙 暗夜 | 护眼深色 | `#78909C` → `#B0BEC5` |

> 切换主题时通过 `activity?.recreate()` 全局刷新，所有页面即时生效。

---

## 📱 截图预览

| 计时 | 待办 | 统计 | 我的 |
|------|------|------|------|
| 科目选择+计时+樱花背景 | 待办列表+截止时间+进度条 | 饼图+热力图+柱状图+签到 | 每周摘要+签到日历+主题切换 |

---

## 🛠 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | **Kotlin** |
| UI 框架 | Material Components + View Binding |
| 页面导航 | **ViewPager2** + BottomNavigationView |
| 图表 | **MPAndroidChart** (PieChart / BarChart) |
| 架构 | Fragment + **AndroidViewModel** + LiveData |
| 数据持久化 | Gson + 本地 JSON 文件 / SharedPreferences |
| 通知 | AlarmManager + NotificationCompat |
| 构建 | Gradle (Kotlin DSL) |

---

## 📁 项目结构

```
app/src/main/java/com/studyfocus/assistant/
├── StudyApp.kt                  # Application 初始化
├── MainActivity.kt              # 主 Activity + 主题/权限引导
├── data/
│   ├── DataRepository.kt        # 专注记录存储
│   ├── TodoRepository.kt        # 待办事项存储
│   ├── CheckInRepository.kt     # 签到存储
│   └── entity/                  # 数据实体 (FocusRecord / TodoItem / SubjectStat / DailyStat)
├── viewmodel/
│   ├── MainViewModel.kt         # 统计+签到+时段 ViewModel
│   └── TodoViewModel.kt         # 待办 ViewModel
├── notification/
│   ├── NotificationHelper.kt    # 提醒通知工具
│   └── AlarmReceiver.kt        # 闹钟广播接收器
├── ui/
│   ├── timer/TimerFragment.kt   # 计时页
│   ├── todo/TodoFragment.kt     # 待办页
│   ├── statistics/StatisticsFragment.kt  # 统计页
│   ├── records/RecordsFragment.kt        # 记录页
│   └── settings/SettingsFragment.kt      # 我的页
└── res/
    ├── layout/                  # 布局 XML
    ├── drawable/                # 背景 / 圆角 / 渐变资源
    ├── values/                  # 颜色 / 字符串 / 5套主题
    └── mipmap/                  # App 图标
```

---

## 🚀 构建运行

```bash
# 克隆仓库
git clone https://github.com/你的用户名/TimeRing.git
cd TimeRing

# 使用 Gradle 构建
./gradlew assembleDebug

# 安装到设备
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> 最低要求：Android 7.0 (API 24)，推荐 Android 12+

---

## 📄 权限说明

| 权限 | 用途 |
|------|------|
| `POST_NOTIFICATIONS` | 发送待办截止提醒通知 |
| `SCHEDULE_EXACT_ALARM` | 精确闹钟定时提醒 |

> 首次启动会弹窗引导用户授权。仅 Android 12+ 需要手动开启。

---

## 💕 关于「时环」

> 时间的圆环，记录每一次专注。
>
> 时环是一款面向学生群体的学习专注工具。
> 它不仅帮你计时，更帮你看见时间去了哪里——
> 什么科目花了最多时间，什么时段最高效，一周一月又进步了多少。
>
> 希望「时环」能陪你度过每一个专注的时刻。🌸

---

## 📃 License

MIT License

---

<p align="center">
  <sub>Made with ❤️ and a lot of ☕</sub>
</p>
