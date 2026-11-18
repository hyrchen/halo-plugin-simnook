# SIMnook

在 Halo 里管理自己的手机号卡与消费记录，并生成一个公开的号卡墙页面。号码默认脱敏，哪些字段对访客公开由你逐张决定。

![SIMnook 号卡墙](https://raw.githubusercontent.com/sunnyhmz7010/halo-plugin-simnook/main/docs/images/home.webp)

## 功能

- **号卡管理**：在 Halo 控制台「工具 → SIMnook」中录入号卡、续费周期、余额和每一笔消费记录；支持快捷续费、按卡片类型 / 国家地区 / 续费紧急度 / 状态筛选。号码会自动整理为「+区号 号码」格式。
- **公开号卡墙**：站点的 `/simnook/` 页面展示号卡、续费倒计时、花费统计和地球分布图，支持名片、表盘、横条三种样式与中英文切换。每张卡可单独选择公开哪些字段，未公开的字段对访客显示为「登录后可见」，完整号码默认不公开。
- **续费提醒**：添加通知渠道，选择接收的 Halo 用户、提醒的号卡和事件（即将到期、续费窗口开放、已逾期），可设置多个每日发送时间和任意提前天数。提醒通过 Halo 通知中心发送，接收方式（站内信、邮件等）沿用 Halo 的通知设置。
- **备份恢复**：导出完整 JSON 备份或 CSV 清单；恢复前逐张预览，可选择新增、跳过、覆盖或仅追加缺失的消费记录。
- **内置运营商图标**：按运营商名称自动匹配常见运营商的图标，也可以上传自定义图标。

## 安装

要求 Halo 2.27.2 及以上。

1. 从 [Releases](https://github.com/sunnyhmz7010/halo-plugin-simnook/releases) 下载最新的 JAR 文件，或在 Halo 应用市场中搜索 SIMnook。
2. 在 Halo 控制台「插件」中上传并启用。
3. 打开「工具 → SIMnook」开始录入号卡，访问 `https://你的站点/simnook/` 查看号卡墙。

插件设置里可以修改号卡墙标题、默认语言与主题、公告、背景，以及是否显示底部的访客信息条（开启后访客浏览器会请求第三方 IP 定位服务）。

## 开发

环境：Java 21、Node.js 22.18 及以上。

```bash
./gradlew build
```

构建产物位于 `build/libs/`。目录结构：

- `src/main/java`：插件后端（号卡、提醒、备份接口与定时任务）
- `ui/`：Halo 控制台界面（Vue 3 + `@halo-dev/components`）
- `frontend/`：公开号卡墙页面

后台接口客户端由 `./gradlew generateApiClient` 从运行中的 Halo（`http://127.0.0.1:18090`，需已安装本插件）的 OpenAPI 文档生成，输出到 `ui/src/api/generated`。

## 致谢

公开号卡墙基于 [SIMerald](https://github.com/BurningSky0306/SIMerald) 与 [Komari Emerald 主题](https://github.com/Tokinx/komari-theme-emerald) 改写，相关代码沿用其 MIT 许可证，见 [frontend/LICENSE](frontend/LICENSE)。

## 许可证

[GPL-3.0](LICENSE)
