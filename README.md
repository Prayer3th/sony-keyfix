# Sony Keyfix

为 **Sony X9500H + RMF-TX910C 蓝牙遥控器**修复输入源、齿轮、选项与三横线按键。无需 Root、无需购买软件，也不需要电脑常开。

[下载安装包](https://github.com/Prayer3th/sony-keyfix/releases/latest) · [English](README.en.md) · [安装与使用](docs/USAGE.md) · [构建](docs/BUILDING.md) · [兼容与验证](docs/COMPATIBILITY.md) · [发布到 GitHub](docs/PUBLISHING.md)

这是独立的社区项目，与 Sony 无隶属关系。

## 功能

| 遥控器按键 | 修复后的功能 |
| --- | --- |
| 输入源 | 打开输入选择 |
| 齿轮 | 打开 Sony 快速设置 |
| 选项 | 当前应用的标准菜单 |
| 三横线 | 当前应用的标准菜单，与原装遥控器的菜单键一致 |

菜单需要当前应用支持，在电视主界面按下无反应属于正常行为。电视键不映射。正常方向键、音量与语音继续使用系统原有功能。

- 在电视本机运行，可选择重启后自动恢复；升级时保留启停状态。
- 遥控器重新连接后，自动查找变化的输入设备编号。
- 中英文界面，支持遥控器方向键与确认键操作。
- 无广告、统计、云服务或无障碍服务。

## 支持范围

目前实机验证的组合是 **X9500H、Android 9、32 位 ARM 用户空间、RMF-TX910C**。应用按此范围设计，服务拒绝其他 Android 版本与用户空间格式。不是通用 Android TV 映射器，其他电视型号或固件不保证兼容。

电视必须已启用 **TCP 5555 端口的 ADB 调试**，允许本机连接，并允许授权的 ADB shell 读取遥控器输入设备。应用不会自行开启调试、修改系统分区或绕过授权。网络调试应只用于可信家庭网络，不要将端口映射到公网。

## 开始使用

1. 安装已签名的 `Sony-Keyfix-1.0.0.apk`；也可以按[构建文档](docs/BUILDING.md)自行构建并签名。
2. 打开 **Sony Keyfix**，选择「启动修复」。
3. 电视弹出调试授权时，勾选「始终允许」，再选择允许。这是应用连接本机的授权。
4. 根据需要勾选「电视重启后自动恢复」。关闭应用界面后，修复继续运行。

选择「停止修复」会保存停止状态；卸载应用可移除修复。清除应用数据会丢失本机 ADB 身份，需要重新授权。详见[使用说明](docs/USAGE.md)。

## 工作原理

四个按键通过蓝牙产生不同的 HID 扫描码，但旧固件将它们报告为同一个未知键。应用使用用户授权的本机 ADB 读取原始事件，并为这四个扫描码发送固定系统按键。

| HID usage | 系统 keyevent |
| --- | --- |
| `000c0533` | `178`，输入源 |
| `000c03c3` | `189`，Sony 快速设置 |
| `000c051f` / `000c038b` | `82`，标准菜单 |

应用只连接 `127.0.0.1:5555`，私有保存每次安装生成的 RSA 密钥。读取输入不独占设备，也不修改系统 keylayout。详细实现、权限和边界见[架构说明](docs/ARCHITECTURE.md)与[安全说明](SECURITY.md)。

## 开发

需要 Python 3.9+、JDK 17、Android SDK Platform 28 和 Build Tools 35.0.1。

```sh
python3 build.py --test-only
python3 build.py
```

默认生成未签名 APK。签名必须显式提供自己的密钥，项目不附带私钥或固定密码。完整参数、JRE + ECJ 备选方案和发布流程见[构建文档](docs/BUILDING.md)。GitHub Actions 配置会运行测试、构建未签名 APK，并上传构建产物；不会自动创建公开 Release。

## 许可与来源

项目代码使用 [MIT License](LICENSE.txt)。内置 [Cameron Gutman / AdbLib](https://github.com/cgutman/AdbLib) 使用 [BSD-3-Clause](AdbLib-LICENSE.txt)，固定上游版本及本地修改见 [NOTICE](NOTICE.txt)。两份许可也随 APK 提供，可在「关于 → 开源许可」查看。

[Caldis/tv-keyremap](https://github.com/Caldis/tv-keyremap) 为 Sony Android 9 原始事件读取提供了参考方向，本项目没有分发或运行其安装脚本。项目不包含 Sony 固件、系统 APK 或系统应用反编译产物。

欢迎提交附带型号、固件与复现步骤的问题报告；请先移除日志中的设备地址和身份信息。参见 [CONTRIBUTING](CONTRIBUTING.md)。
