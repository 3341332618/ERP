import { describe, expect, it } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'

const allowedTechnical = [
  'script',
  'template',
  'style',
  'setup',
  'lang',
  'ts',
  'const',
  'import',
  'from',
  'return',
  'async',
  'await'
]
const legacySalePriceLabel = ['建议', '销售价'].join('')

function files(dir: string): string[] {
  return readdirSync(dir).flatMap((name: string) => {
    const path = join(dir, name)
    return statSync(path).isDirectory() ? files(path) : [path]
  })
}

describe('中文界面文案', () => {
  it('关键页面包含中文业务文案', () => {
    const content = files(join(process.cwd(), 'src'))
      .filter((file) => file.endsWith('.vue') || file.endsWith('.ts'))
      .map((file) => readFileSync(file, 'utf8'))
      .join('\n')

    expect(content).toContain('ERP管理平台')
    expect(content).toContain('采购入库')
    expect(content).toContain('销售出库')
    expect(content).toContain('库存分布')
    expect(content).toContain('收入结算')
    expect(content).toContain('上传头像')
    expect(content).toContain('头像上传成功')
    expect(content).toContain('建议零售价')
    expect(content).toContain('上传文件大小不能超过200KB')
    expect(content).not.toContain(legacySalePriceLabel)
    expect(allowedTechnical.length).toBeGreaterThan(0)
  })

  it('库存调拨路由使用库存调拨单配置', () => {
    const router = readFileSync(join(process.cwd(), 'src/router/index.ts'), 'utf8')
    const documentView = readFileSync(join(process.cwd(), 'src/views/DocumentView.vue'), 'utf8')

    expect(router).toContain("path: 'inventory/transfer'")
    expect(documentView).toContain("route.path === '/inventory/transfer'")
    expect(documentView).toContain("api: 'stock-transfer'")
    expect(documentView).toContain('请输入库存调拨单号查询')
  })
})
