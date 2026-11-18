import { describe, expect, it } from 'vitest'
import type { CardSpec, ManagedCardView } from '../api/generated'
import { newExpense, suggestedExtension } from './cardEntry'
import { filterCardViews, tidyPhoneNumber } from './cardManagement'

describe('号卡录入', () => {
 it('新消费按记录数决定类型，续费从当前有效期顺延一个周期', () => {
  const card: CardSpec = { name: '测试卡', carrier: '运营商', number: '+86 13800123456', currency: 'USD', cycleDays: 30 }
  expect(newExpense(card, '2026-11-07')).toMatchObject({ type: '首次购买/开卡', currency: 'USD', title: '运营商 · 3456 · 首次购买/开卡 · 2026-11-07' })
  expect(newExpense({ ...card, records: [{}] }, '2026-11-07').type).toBe('续费/保号')

  const renewed = { ...card, records: [{ date: '2026-10-01', extendsTo: '2026-12-01' }] }
  expect(suggestedExtension(renewed, '2026-11-07', '续费/保号')).toBe('2026-12-31')
  // 已过期时从交易日起算；非续费类消费和无效日期不给建议。
  expect(suggestedExtension({ ...card, records: [{ date: '2026-09-01', extendsTo: '2026-10-01' }] }, '2026-11-07', 'Renewal')).toBe('2026-12-07')
  expect(suggestedExtension(card, '2026-11-07', '额外充值')).toBe('')
  expect(suggestedExtension(card, '2026-02-31', '首次购买/开卡')).toBe('')
 })

 it('号码去掉分隔符并在国际区号后加空格', () => {
  expect(tidyPhoneNumber(' +372 5314-6889 ')).toBe('+372 53146889')
  expect(tidyPhoneNumber('+1 (415) 555-0123')).toBe('+1 4155550123')
  expect(tidyPhoneNumber('+447700900123')).toBe('+44 7700900123')
  expect(tidyPhoneNumber('+85255550123')).toBe('+852 55550123')
  // 没有 + 时无法判断区号，只去分隔符。
  expect(tidyPhoneNumber('138 0000 1234')).toBe('13800001234')
  expect(tidyPhoneNumber('  ')).toBeUndefined()
 })
})

describe('号卡列表筛选', () => {
 const cards: ManagedCardView[] = [
  { id: 'a', name: '移动 · 1234', carrier: '中国移动', number: '+86 13800001234', countryName: '中国', status: 'active', urgencyCode: 'days7', nextRenewalAt: '2026-11-15', typeKind: 'physical' },
  { id: 'b', name: 'Second', countryName: '美国', status: 'expired', urgencyCode: 'overdue', nextRenewalAt: '2026-11-01', typeKind: 'adapter' },
  { id: 'c', name: 'No date', status: 'active', urgencyCode: 'noDate' },
  { id: 'd', name: 'Closed', status: 'cancelled', urgencyCode: 'cancelled' },
  { id: 'e', name: 'Later', status: 'active', urgencyCode: 'days30' },
 ]
 const none = { keyword: '', country: '', status: '', urgency: '' }

 it('按紧急度排序，并支持格式化号码搜索和组合筛选', () => {
  expect(filterCardViews(cards, none).map(card => card.id)).toEqual(['b', 'a', 'e', 'c', 'd'])
  expect(filterCardViews(cards, { ...none, keyword: '+8613800001234' }).map(card => card.id)).toEqual(['a'])
  expect(filterCardViews(cards, { ...none, keyword: 'second', country: '美国', status: 'expired', urgency: 'overdue', type: 'adapter' }).map(card => card.id)).toEqual(['b'])
  expect(filterCardViews(cards, { ...none, keyword: 'Second', country: '中国' })).toEqual([])
 })
})
