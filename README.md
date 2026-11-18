<div align="center">
  <img src="https://raw.githubusercontent.com/sunnyhmz7010/halo-plugin-simnook/main/src/main/resources/logo.svg" alt="SIMnook Logo" width="96" />
  <h1>SIMnook</h1>
  <p>在 Halo 中管理号卡与消费记录，并提供公开的 SIM/eSIM 收藏展示墙</p>
  <p>公开号卡墙派生自 <a href="https://github.com/BurningSky0306/SIMerald">SIMerald</a> 与 <a href="https://github.com/Tokinx/komari-theme-emerald">Komari Emerald 主题</a>，相关代码沿用其 MIT 许可证，见 <a href="https://github.com/sunnyhmz7010/halo-plugin-simnook/blob/main/frontend/LICENSE">frontend/LICENSE</a>。在此谨向原作者致以诚挚谢意，感谢其作出的原创性贡献。</p>
</div>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ed8b00?style=flat-square&amp;logo=openjdk&amp;logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Vue-3-4fc08d?style=flat-square&amp;logo=vuedotjs&amp;logoColor=white" alt="Vue 3" />
  <img src="https://img.shields.io/badge/Halo-2.27-1f2937?style=flat-square" alt="Halo 2.27" />
  <img src="https://img.shields.io/badge/Gradle-9-02303a?style=flat-square&amp;logo=gradle&amp;logoColor=white" alt="Gradle 9" />
</p>

<p align="center">
  <a href="https://github.com/sunnyhmz7010/halo-plugin-simnook/releases"><img src="https://img.shields.io/github/v/release/sunnyhmz7010/halo-plugin-simnook?label=Release&amp;color=3b82f6" alt="Release" /></a>
  <a href="https://github.com/sunnyhmz7010/halo-plugin-simnook/blob/main/LICENSE"><img src="https://img.shields.io/github/license/sunnyhmz7010/halo-plugin-simnook?color=10b981" alt="License" /></a>
  <a href="https://github.com/sunnyhmz7010/halo-plugin-simnook/actions/workflows/ci.yaml"><img src="https://img.shields.io/github/actions/workflow/status/sunnyhmz7010/halo-plugin-simnook/ci.yaml?branch=main&amp;label=CI" alt="CI" /></a>
</p>

---

## ✨ 为什么做这个插件

手里的 eSIM 和实体卡一多，每张卡的续费周期都不一样，靠备忘录记到期日很容易漏，续费后又算不清每张卡到底花了多少。记在表格里在手机上翻着也不方便，直接公开又担心把完整号码暴露出去。

- 一年下来总会漏掉一两张不常用的卡，等到停机或者号码被回收了才发现
- 续费、保号、充值混在一起，说不清每个月在这些卡上到底固定花多少
- 想分享自己在用哪些号卡时只能整张截图，号码和金额全在上面

## 📸 截图预览

![SIMnook 号卡墙](https://raw.githubusercontent.com/sunnyhmz7010/halo-plugin-simnook/main/home.webp)

## 🚀 核心能力

- 号卡管理：在控制台录入号卡、续费周期、余额和每一笔消费记录，支持快捷续费与多条件筛选
- 公开号卡墙：站点的 `/simnook/` 页面展示号卡、续费倒计时、花费统计和地球分布图，支持名片、表盘、横条三种样式与中英文界面
- 字段公开：每张卡单独选择对访客公开的字段，完整号码默认不公开，隐藏字段显示为登录后可见
- 续费提醒：通过 Halo 通知渠道发送每日汇总，可选择接收用户、号卡与提醒事件，支持多个发送时间和自定义提前天数
- 备份恢复：导出完整 JSON 备份或 CSV 清单，恢复前逐张预览并可选择新增、跳过、覆盖或仅追加缺失记录
- 运营商图标：内置 94 个运营商图标按名称自动匹配，也可上传自定义图标

## ⚡ 快速开始

### 📋 前置要求

- 已部署的 Halo 站点，版本 2.27.2 及以上

### 📦 安装与运行

从 [Releases](https://github.com/sunnyhmz7010/halo-plugin-simnook/releases) 下载最新的 JAR，在 Halo 控制台「插件 → 安装」上传 JAR，启用后即可使用；也可以在 Halo 应用市场中搜索 SIMnook 安装。

启用后从控制台「工具 → SIMnook」进入管理页，访问 `https://你的站点/simnook/` 查看公开号卡墙。

## 📖 使用说明

在「工具 → SIMnook」的号卡页新增第一张卡，填写运营商、号码、续费周期与续费价格。号码会自动整理为「+区号 号码」格式，续费日期和余额可在编辑弹窗中继续补充。

号卡编辑弹窗的「公开设置」决定访客能看到哪些字段，默认公开字段名、运营商、末四位、国家地区、卡片类型、保号方式、续费周期、价格、日期等，完整号码、设备、余额、账号、绑定服务默认不公开。点「恢复默认」可以一键勾回默认字段。

「续费提醒」页添加通知渠道，选择接收的 Halo 用户与提醒事件。即将到期只在勾选的提前天数当天提醒，续费窗口开放和已逾期按勾选的事件每天提醒。提醒通过 Halo 通知中心发送，站内信、邮件等接收方式沿用用户在 Halo 中的通知设置。

「备份恢复」页可导出完整 JSON 备份或两类 CSV 清单，导出文件名附带时间戳。恢复时先选择 JSON 文件预览，逐张选择恢复方式后再执行；覆盖会替换业务字段，仅追加消费记录只合并缺失的记录。

## 🧠 功能细节

- 时区固定为 Asia/Shanghai，提醒的发送时间与「今天」都按该时区计算
- 有效期取交易日期最新、且填写了延长日期的消费记录；首次购买日期取最早的消费日期
- 停用日期填写后号卡自动标记为已注销，不再参与提醒，也不再出现在续费紧急度排序中
- 每个发送时间各发一次汇总，同一时间点每名接收人只提交一次，失败的接收人在下一轮重试
- 备份 JSON 上限 32 MiB，同 ID 数据默认跳过，恢复中途失败时已完成的写入会保留
- 公开接口不返回未公开字段，且公开视图中的金额、类型等字段按脱敏值输出
- 开启底部的访客信息条后，页面会为收起状态预留高度，展开时浮在内容上方

## 🧱 技术栈

- Java 21：插件后端语言
- Spring Boot 与 Halo 插件框架：接口注册、扩展模型与定时任务
- Vue 3 与 TypeScript：控制台界面和公开号卡墙
- Vite：两个前端工程的构建与调试
- Gradle：插件构建、测试与本地联调

## 🗂️ 项目结构

```text
halo-plugin-simnook/
├── src/main/java/        # 插件后端：号卡、提醒、备份接口与定时任务
├── src/main/resources/   # plugin.yaml、设置表单、通知模板与公开页模板
├── ui/                   # Halo 控制台界面
├── frontend/             # 公开号卡墙页面
├── home.webp             # README 截图
└── build.gradle          # 构建配置
```

## 👨‍💻 本地开发

### 🧰 环境

- 开发平台：Windows 11、macOS 或 Linux，64 位
- Java：21
- Node.js：22.18 及以上
- pnpm：10.x，构建控制台界面时使用

### 🔨 构建插件

```bash
./gradlew build
```

产物位于 `build/libs/`。

### 🖥️ 本地联调

```bash
./gradlew haloServer
```

该任务通过 Docker 启动一个已安装本插件的 Halo，改动后执行 `./gradlew reloadPlugin` 重载插件。

### 🔧 生成后台接口客户端

```bash
./gradlew generateApiClient
```

需要 `127.0.0.1:18090` 上有已安装本插件的 Halo，生成结果输出到 `ui/src/api/generated`。

## 🔐 安全报告

如果发现安全问题，请不要公开披露细节。请优先参考仓库中的 [SECURITY.md](./SECURITY.md) 提交安全报告。

## 📄 许可证

本项目基于 [GPL-3.0](./LICENSE) 开源。

<div align="center">
  <sub>Built with ❤️ by Sunny</sub>
</div>
