# Debug Session: undo-redo-failure

## Status
[OPEN]

## Symptoms
- 重做按钮点击无反应
- 撤销只能撤销一次

## Hypotheses
1. `redoStack` 在每次新操作时被清空，导致重做无内容可恢复
2. `undo()` 中 `currentState` 获取不正确，导致 redoOperation 记录的状态错误
3. `HistoryManager` 实例在不同操作间没有被正确保留（例如玩家 UUID 变化或实例被替换）
4. 网络包处理时 `isUndo` 参数被错误解析，导致撤销和重做行为异常
5. `finishOperation()` 在某些情况下被调用而 `startOperation()` 没有，导致 currentOperation 状态混乱

## Evidence
通过代码审查发现 `HistoryManager.undo()` 中创建 redoOperation 时参数顺序错误：
- 当前代码：`new BlockChangeRecord(record.pos, currentState, record.oldState, currentData, record.oldData)`
- 这表示 redoOperation 的 oldState=currentState(B), newState=record.oldState(A)
- 但 redo 应该执行 A -> B，所以正确应该是 oldState=A, newState=B
- 即：`new BlockChangeRecord(record.pos, record.oldState, currentState, record.oldData, currentData)`

同样的问题存在于 `redo()` 中创建 undoOperation 时。

这个 bug 可以完美解释：
- 重做没反应：redo 实际执行 B -> A，但当前状态已经是 A，看起来没变化
- 撤销只能一次：undo 后 redoStack 有错误记录，但 undoStack 已空，无法继续撤销

## Reproduction Steps
1. 进入游戏，手持 Creative Worldshaper
2. 对世界中的方块执行一次操作（如 Fill）
3. 打开 GUI，点击撤销按钮，观察是否恢复
4. 再次点击撤销按钮，观察是否还能撤销
5. 点击重做按钮，观察是否无反应

## Environment
- OS: Windows
- Project: worldshaper-standalone
- Minecraft: 1.21.1
- NeoForge: 21.1.219
