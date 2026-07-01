# ERP 备份说明

## 备份点

- 提交：`d079f47`
- 分支：`feature/erp-core-loop`
- 备份分支：`backup/pre-bug-training-20260620`
- 备份标签：`backup-pre-bug-training-20260620`

## 文件

- `erp-d079f47-source.zip`：从 `d079f47` 生成的干净源码快照。
- `VERSION.txt`：备份提交的基础信息。

## 恢复方式

恢复到备份分支：

```powershell
git switch backup/pre-bug-training-20260620
```

或从 zip 解压到新目录单独查看。

## 注意

该备份只包含已提交源码，不包含当前工作区未提交的需求文档替换文件，也不包含 `node_modules`、`target`、`dist` 等构建产物。
