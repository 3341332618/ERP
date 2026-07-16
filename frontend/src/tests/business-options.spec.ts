import { describe, expect, it } from 'vitest'
import {
  dedupeMasterOptions,
  stockProductOptions,
  stockWarehouseOptions,
  transferTargetOptions
} from '../utils/businessOptions'

describe('业务关联选项', () => {
  it('同名主数据优先保留工作区本地记录', () => {
    const options = dedupeMasterOptions([
      { id: 1, name: '华东仓库', status: 'ENABLED', workspaceOwnerId: null },
      { id: 2, name: '华东仓库', status: 'ENABLED', workspaceOwnerId: 101 },
      { id: 3, name: '停用仓库', status: 'DISABLED', workspaceOwnerId: 101 }
    ])

    expect(options).toHaveLength(1)
    expect(options[0].id).toBe(2)
  })

  it('停用的本地副本遮住启用的公共源记录', () => {
    const records = [
      { id: 1, code: 'CK001', name: '华东仓库', status: 'ENABLED', workspaceOwnerId: null },
      { id: 2, code: 'CK001-S101', name: '华东仓库（本地）', status: 'DISABLED', workspaceOwnerId: 101 }
    ]

    expect(dedupeMasterOptions(records)).toEqual([])
    expect(dedupeMasterOptions(records, 2)).toEqual([records[1]])
    expect(dedupeMasterOptions(records, undefined, '华东仓库（本地）')).toEqual([records[1]])
  })

  it('库存商品只保留所选仓库中可用数量大于零的记录并去重', () => {
    const options = stockProductOptions([
      { warehouseId: 10, productId: 100, productCode: 'SP001', productName: '笔记本', availableQuantity: '2.00' },
      { warehouseId: '10', productId: '100', productCode: 'SP001', productName: '笔记本', availableQuantity: 1 },
      { warehouseId: 10, productId: 101, productCode: 'SP002', productName: '显示器', availableQuantity: '0.00' },
      { warehouseId: 11, productId: 102, productCode: 'SP003', productName: '键盘', availableQuantity: '5.00' }
    ], '10')

    expect(options).toHaveLength(1)
    expect(options[0].productId).toBe(100)
  })

  it('调入仓库排除调出仓库并兼容数字字符串', () => {
    const options = transferTargetOptions([
      { id: 10, name: '华东仓库', status: 'ENABLED', workspaceOwnerId: null },
      { id: '11', name: '华南仓库', status: 'ENABLED', workspaceOwnerId: null },
      { id: 12, name: '停用仓库', status: 'DISABLED', workspaceOwnerId: null }
    ], '10')

    expect(options.map((option) => Number(option.id))).toEqual([11])
  })

  it('调出仓库只保留存在正可用库存的仓库', () => {
    const warehouses = [
      { id: 10, code: 'CK001', name: '华东仓库', status: 'ENABLED', workspaceOwnerId: null },
      { id: 11, code: 'CK002', name: '华南仓库', status: 'ENABLED', workspaceOwnerId: null },
      { id: 12, code: 'CK003', name: '停用仓库', status: 'DISABLED', workspaceOwnerId: null }
    ]
    const stock = [
      { warehouseId: 10, productId: 100, productCode: 'SP001', productName: '笔记本', availableQuantity: 0 },
      { warehouseId: 10, productId: 101, productCode: 'SP002', productName: '显示器', availableQuantity: 2 },
      { warehouseId: 11, productId: 102, productCode: 'SP003', productName: '键盘', availableQuantity: 0 },
      { warehouseId: 12, productId: 103, productCode: 'SP004', productName: '鼠标', availableQuantity: 5 }
    ]

    expect(stockWarehouseOptions(warehouses, stock).map((option) => Number(option.id))).toEqual([10])
  })
})