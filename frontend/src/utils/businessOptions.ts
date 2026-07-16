export type NumericValue = number | string

export interface MasterOption {
  id: NumericValue
  code?: string | null
  name: string
  status: string
  workspaceOwnerId?: NumericValue | null
}

export interface StockOption {
  warehouseId: NumericValue
  productId: NumericValue
  productCode: string
  productName: string
  availableQuantity: NumericValue
}

function numericEquals(left: NumericValue | null | undefined, right: NumericValue | null | undefined) {
  if (left === null || left === undefined || right === null || right === undefined) return false
  return Number(left) === Number(right)
}

function isLocalMaster(record: MasterOption) {
  return record.workspaceOwnerId !== null && record.workspaceOwnerId !== undefined
}

function sourceKey(record: MasterOption) {
  return record.code?.replace(/-S\d+$/, '') || record.name.trim()
}

export function dedupeMasterOptions<T extends MasterOption>(
  records: T[],
  selectedIds?: NumericValue | NumericValue[] | null,
  selectedNames?: string | string[] | null
): T[] {
  const localSourceKeys = new Set(
    records.filter(isLocalMaster).map(sourceKey)
  )
  const localNames = new Set(
    records.filter(isLocalMaster).map((record) => record.name.trim())
  )
  const options = new Map<string, T>()

  records
    .filter((record) => isLocalMaster(record)
      || (!localSourceKeys.has(sourceKey(record)) && !localNames.has(record.name.trim())))
    .forEach((record) => {
      const key = sourceKey(record)
      const existing = options.get(key)
      if (!existing || (isLocalMaster(record) && !isLocalMaster(existing))) {
        options.set(key, record)
      }
    })

  const selectedIdList = selectedIds === null || selectedIds === undefined
    ? []
    : Array.isArray(selectedIds) ? selectedIds : [selectedIds]
  const selectedNameList = selectedNames === null || selectedNames === undefined
    ? []
    : Array.isArray(selectedNames) ? selectedNames : [selectedNames]

  return [...options.values()]
    .filter((record) => record.status === 'ENABLED'
      || selectedIdList.some((selectedId) => numericEquals(record.id, selectedId))
      || selectedNameList.includes(record.name))
}

export function stockProductOptions<T extends StockOption>(rows: T[], warehouseId: NumericValue | null | undefined): T[] {
  const options = new Map<number, T>()
  rows
    .filter((row) => numericEquals(row.warehouseId, warehouseId))
    .filter((row) => Number(row.availableQuantity) > 0)
    .forEach((row) => {
      const productId = Number(row.productId)
      if (!options.has(productId)) options.set(productId, { ...row, productId } as T)
    })
  return [...options.values()]
}

export function stockWarehouseOptions<T extends MasterOption>(
  warehouses: T[],
  rows: Array<Pick<StockOption, 'warehouseId' | 'availableQuantity'>>,
  selectedWarehouseId?: NumericValue | null
): T[] {
  const warehouseIdsWithStock = new Set(
    rows
      .filter((row) => Number(row.availableQuantity) > 0)
      .map((row) => Number(row.warehouseId))
  )
  return dedupeMasterOptions(warehouses, selectedWarehouseId)
    .filter((warehouse) => warehouseIdsWithStock.has(Number(warehouse.id)))
}

export function transferTargetOptions<T extends MasterOption>(
  warehouses: T[],
  sourceWarehouseId: NumericValue | null | undefined,
  selectedTargetWarehouseId?: NumericValue | null
): T[] {
  return dedupeMasterOptions(warehouses, selectedTargetWarehouseId)
    .filter((warehouse) => !numericEquals(warehouse.id, sourceWarehouseId))
}