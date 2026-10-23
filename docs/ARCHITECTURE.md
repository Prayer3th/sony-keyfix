# 实现与边界

## 数据路径

Bluetooth HID → Linux input_event → 本机 ADB 原始流 → EventDecoder → 固定按键映射 → Android input keyevent。

`MainActivity` 只提供界面和显式启停操作。`RemapService` 持有本机 TCP 连接、前台通知、原始事件流和动作队列。退出界面不结束服务；停止服务关闭连接与输入流，没有脱离服务的后台 shell 守护进程。

`BootReceiver` 处理系统 BOOT_COMPLETED 和 MY_PACKAGE_REPLACED。开机恢复需要 `boot=true` 且 `enabled=true`；升级恢复只要求此前已启用。偏好名称与原安装相同。默认关闭开机恢复，用户可自行开启。

## 设备与事件

- 服务限制 Android API 28 与 `armeabi-v7a`，读取的原始结构为 16 字节、小端 Linux input_event。不能凭 64 位内核名称推断用户空间事件宽度。
- `RemoteDevice` 仅解析 `dumpsys input` 的 Event Hub 设备块，选择 bus `0005`、vendor `054c`、product `0eae`。路径必须完整匹配 `/dev/input/event` 加数字，不能包含 shell 字符。
- 使用 `exec:cat` 读取二进制输入；没有独占抓取。重连时重新定位节点，不保存 event2 等固定编号。
- 四个 HID usage 伴随 `MSC_SCAN` 与 `KEY_UNKNOWN`。只映射首次 DOWN，不映射重复值 2、UP、普通已知按键或其他 usage。
- 遇到 SYN_DROPPED 时清空状态并丢弃至下一 SYN_REPORT，只接受其后的新 DOWN；不会通过补发动作恢复按住的键。参见 [Linux 输入事件文档](https://www.kernel.org/doc/html/latest/input/event-codes.html)。

## 连接与失败处理

本机 ADB 协议由固定版本的 AdbLib 提供，修改记录见 NOTICE.txt。每个连接拥有独立的 socket，旧动作只能关闭自己的连接，不能影响新连接。首次连接有 4 秒连接超时、60 秒授权读取超时；短命令和流打开约 12 秒超时。断开后约 5 秒重试。

待执行动作队列最多 4 项，加上 1 个执行中的动作；溢出丢弃。每流缓冲最多 256 KiB，ADB 包最大 4 KiB，短命令输出最多 64 KiB。读取超限或损坏包时关闭连接，避免无限增长。传统 shell 流通过固定退出码标记判断命令是否完成，连接断开不会当作成功。

应用状态使用本地化资源，不直接把 shell 输出或底层异常堆栈展示给普通用户。没有持久化按键日志、遥测或任意命令输入。

## 为什么不用其他方式

该固件把四键交给上层时都表现为未知键，原始 HID usage 才能区分。参考 [Key Mapper 的相关讨论](https://github.com/keymapperorg/KeyMapper/issues/1276)。[tv-keyremap](https://github.com/Caldis/tv-keyremap) 提供了 Sony Android 9 shell 原始事件读取的参考方向。

实机观察中，输入源 keyevent 178 有效；快速设置使用 Sony 的 189。176 未触发所需设置界面，82 是应用菜单而不是主界面菜单。Sony 的 quicklauncher Activity 实际打开输入选择，不能替代快速设置。

Sony IRCC 能列出遥控代码，但该电视发送操作返回认证错误；本方案不依赖 IRCC，也不修改 IP 控制认证。参见 [Sony IRCC-IP 文档](https://pro-bravia.sony.net/remote-display-control/ircc-ip/)。仓库不含 Sony 系统 APK、反编译输出或专有代码。
