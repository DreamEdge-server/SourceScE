# SourceScE

SourceScE 是全屏屏幕效果插件：使用 CraftEngine 图像作为全屏覆盖层，支持颜色、标题、副标题、原生淡入/停留/淡出和可选移动冻结。效果结束后会恢复玩家原本的 HUD 和状态。

## 运行要求

- Paper/ASP 后端 26.1.2-26.2，Java 25。
- CraftEngine：提供 `sourcesce:fullscreen` 和 `sourcesce:fullscreen_transparent` 图像，实际显示效果需要 CE。
- PacketEvents：隐藏原版热栏、生命、饥饿、经验条；缺失时这些 HUD 不隐藏，但插件仍能工作。
- BetterHud：可选，只有 `hide-betterhud: true` 时参与隐藏 BetterHud HUD。
- 产物：`../Source-dist/SourceScE.jar`。

```powershell
$env:JAVA_HOME = 'C:\Users\32394\.jdks\ms-21.0.11'
.\gradlew.bat shadowJar
```

默认以 26.1.2 API 构建；要检查 26.2 API，可执行：

```powershell
$env:JAVA_HOME = 'C:\Users\32394\.jdks\ms-21.0.11'
.\gradlew.bat '-PpaperApiVersion=26.2.build.87-stable' clean compileJava --offline --no-daemon
```

推送到 `main` 或 `master` 后，GitHub Actions 会构建并发布插件 JAR、CraftEngine 资源包 ZIP 和 SHA-256 清单到 GitHub Release；Pull Request 只构建不发布。

## 安装和资源

插件首次启动会将内置资源增量释放到：

```text
plugins/CraftEngine/resources/sourcesce/
```

如果目标文件新增或版本变化，执行：

```text
/ce reload all
```

不会覆盖管理员已有的同名 CE 文件。要调整图片实际渲染宽度，可修改 `overlay-image-width`，它只影响标题文字与覆盖图的居中计算。

## 配置

```yaml
hide-betterhud: false
hide-vanilla-hud: false
overlay-image-width: 256
execute_commands_on_start:
  enabled: false
  commands: []
execute_commands_on_finish:
  enabled: false
  commands: []
```

- 两个 HUD 开关默认关闭，避免与 SourceTasks 的对话 HUD/伪旁观者状态互相恢复。
- `execute_commands_on_start` 和 `execute_commands_on_finish` 支持 `%player%`，默认关闭，开启前确认命令不会重复触发业务逻辑。
- `lang.yml` 只负责命令提示；效果名来自 CraftEngine 图像 ID。

## 命令与权限

主命令 `/sourcesce`，别名 `/sce`、`/screeneffect`：

```text
/sourcesce <效果> <颜色> <淡入tick> <停留tick> <淡出tick> <freeze|nofreeze> [目标] [标题] [副标题]
/sourcesce stop [目标]
```

示例：

```text
/sce fullscreen RED 10 60 10 freeze me Welcome/_home
/sce fullscreen_transparent #770000 0 40 10 nofreeze Steve Boss/_warning
/sce stop Steve
```

- 效果目前是 `fullscreen` 或 `fullscreen_transparent`。
- 颜色支持 `RED` 等名称或 `#770000` 十六进制值。
- 时间单位是 tick，20 tick 约等于 1 秒。
- 目标支持 `me`、`all` 或在线玩家名；控制台必须明确指定目标。
- 标题和副标题中的 `/_` 会转换为空格。

权限：`sourcesce.use` 允许使用命令，`sourcesce.others` 允许指定其他玩家，均默认 OP。

## 故障排查

- 提示没有 CraftEngine：检查 CE 插件是否启用，再执行 `/ce reload all`。
- 提示没有效果：确认使用的是 `fullscreen` 或 `fullscreen_transparent`，并检查 CE 资源 ID。
- HUD 没有隐藏：检查 PacketEvents 是否启用，以及两个 `hide-*` 配置是否打开。
- 效果结束后 HUD 异常：不要让 SourceScE 与 SourceTasks 同时负责同一类 HUD 隐藏；当前默认配置已关闭两项隐藏。
