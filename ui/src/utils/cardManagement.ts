import type { ManagedCardView } from '../api/generated'

export const URGENCY_CODES = ['window', 'overdue', 'days3', 'days7', 'days15', 'days30', 'days60', 'beyond', 'noDate', 'cancelled']

/** 号码只保留开头的 + 和数字，去掉空格、全角空格、连字符、括号和点。 */
export function compactNumber(value?: string): string | undefined {
 const trimmed = value?.trim()
 if (!trimmed) return undefined
 return trimmed.replace(/[\s\u3000\-–—().·]/g, '')
}

// ITU 国际区号是前缀码：只有 1、7 两个一位码和下列两位码，其余合法区号均为三位，按前缀即可唯一切分。
const TWO_DIGIT_CODES = new Set('20 27 30 31 32 33 34 36 39 40 41 43 44 45 46 47 48 49 51 52 53 54 55 56 57 58 60 61 62 63 64 65 66 81 82 84 86 90 91 92 93 94 95 98'.split(' '))

/** 国际区号长度；号码不以 + 开头或位数不足时返回 0。 */
export function callingCodeLength(compact: string): number {
 const digits = /^\+(\d+)$/.exec(compact)?.[1] ?? ''
 const length = /^[17]/.test(digits) ? 1 : TWO_DIGIT_CODES.has(digits.slice(0, 2)) ? 2 : 3
 return digits.length > length ? length : 0
}

/** 整理号码：先去掉分隔符，再在国际区号后加一个空格，如 +372 53146889；无 + 前缀时只做去分隔。 */
export function tidyPhoneNumber(value?: string): string | undefined {
 const compact = compactNumber(value)
 if (!compact) return undefined
 const length = callingCodeLength(compact)
 return length ? `${compact.slice(0, length + 1)} ${compact.slice(length + 1)}` : compact
}

export function filterCardViews(cards: ManagedCardView[], filters: { keyword: string; country: string; status: string; urgency: string; type?: string }): ManagedCardView[] {
 const keyword = filters.keyword.trim().toLocaleLowerCase()
 const digits = keyword.replace(/[^0-9]/g, '')
 return cards.filter(card => {
  if (filters.country && card.countryName !== filters.country) return false
  if (filters.status && card.status !== filters.status) return false
  if (filters.urgency && card.urgencyCode !== filters.urgency) return false
  if (filters.type && card.typeKind !== filters.type) return false
  if (!keyword) return true
  return [card.name, card.number, card.carrier].some(value => value?.toLocaleLowerCase().includes(keyword))
   || Boolean(digits && /^[+\d\s()-]+$/.test(keyword) && card.number?.replace(/[^0-9]/g, '').includes(digits))
 }).sort((a, b) => {
  const rank = (card: ManagedCardView) => {
   const index = URGENCY_CODES.indexOf(card.urgencyCode ?? 'noDate')
   return index < 0 ? URGENCY_CODES.length : index
  }
  return rank(a) - rank(b)
   || (a.nextRenewalEnd || a.nextRenewalAt || '\uffff').localeCompare(b.nextRenewalEnd || b.nextRenewalAt || '\uffff')
   || (a.name ?? '').localeCompare(b.name ?? '')
   || (a.id ?? '').localeCompare(b.id ?? '')
 })
}
