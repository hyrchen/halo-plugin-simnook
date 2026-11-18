/**
 * Shared types between frontend and backend.
 * Executed directly via Node native type-stripping; must contain only type declarations (no runtime syntax like enums).
 */

export type CardStatus = 'active' | 'expired' | 'cancelled'

/** High-level card category, determines the icon */
export type CardKind = 'physical' | 'adapter' | 'embedded' | 'virtual'

/** 服务端从消费记录类型归类，统计和图表使用同一个值。 */
export type RecordKind = 'purchase' | 'renewal' | 'topup' | 'other'

export type KeepAliveCode = 'topup' | 'addon' | 'use' | 'balanceChange' | 'autoCharge' | 'plan'
/** 与后端 PublicCard.PUBLIC_FIELDS 一致。 */
export type PublicField
  = | 'name' | 'carrier' | 'last4' | 'number' | 'country' | 'type' | 'keepAlive' | 'cycle' | 'price' | 'dates'
    | 'firstPurchase' | 'history' | 'device' | 'balance' | 'account' | 'services'

/** 卡片类型图标的颜色 */
export type TypeColor
  = | 'default' | 'gray' | 'brown' | 'orange' | 'yellow' | 'green' | 'blue' | 'purple' | 'pink' | 'red'

export interface SimRecord {
  /** 记录在公开卡片中的 ID。 */
  id: string
  /** Transaction category (initial purchase, renewal, top-up, or other fee) */
  type: string
  kind: RecordKind
  /** Transaction date (YYYY-MM-DD) */
  date: string | null
  /** Payment amount in original currency */
  amount: number | null
  /** ISO currency code */
  currency: string
  /** Exchange rate at transaction time (1 unit of foreign currency in CNY) */
  rate: number | null
  /** 按录入时汇率折算的人民币金额。 */
  amountCNY: number | null
  /** New expiration date after this transaction (YYYY-MM-DD) */
  extendsTo: string | null
  /** Returned only when authenticated: record title */
  title?: string
}

export interface SimCard {
  /** Halo 资源的稳定 ID，用于详情路由。 */
  id: string
  /** Display name, e.g. "Harbour Mobile · 0123" */
  name: string
  /** Short carrier name, e.g. "Harbour Mobile" */
  carrier: string
  /** Last 4 digits of the phone number */
  last4: string
  /** Masked phone number for public display, e.g. "•••• 0123" */
  numberMasked: string
  /** Flag emoji, e.g. "🇪🇪", or empty string if none */
  region: string
  /** Country or region name */
  countryName: string
  typeKind: CardKind | null
  typeColor: TypeColor
  /** Actions required to keep the number active */
  keepAlive: string[]
  keepAliveCodes: (KeepAliveCode | null)[]
  /** Renewal cycle in days, or 0 if irregular */
  cycleDays: number
  /** Raw renewal cycle text */
  cycleLabel: string
  /** Renewal price */
  price: number
  /** ISO currency code for renewal price */
  currency: string
  /** Current expiration date */
  expiresAt: string | null
  /** Next renewal date (start date if a date range) */
  nextRenewalAt: string | null
  /** End date when next renewal date is a range */
  nextRenewalEnd: string | null
  /** Current period start date (date of the transaction that set the current expiration) */
  periodStart: string | null
  status: CardStatus
  statusLabel: string
  cancelledAt: string | null
  firstPurchaseAt: string | null
  totalSpentCNY: number | null
  /** Raw renewal urgency text, usable for sorting */
  urgency: string | null
  /** Halo 后台填写的展示排序。 */
  order: number
  history: SimRecord[]
  /** Halo 附件的 Logo URL。 */
  logo?: string | null
  /** 访客接口中该号卡额外公开的字段；管理员视图不依赖它。 */
  publicFields?: PublicField[]
  // Fields below are returned when authenticated, or to visitors when listed in publicFields
  /** Raw card type label */
  type?: string
  balance?: number | null
  balanceUpdatedAt?: string | null
  /** Primary device */
  device?: string | null
  number?: string | null
  services?: string[]
  account?: string | null
  dataCheck?: string | null
}

export type ThemeSettings = Record<string, string | number | boolean>

export interface SiteInfo {
  sitename: string
  version: string
  loggedIn: boolean
  theme_settings: ThemeSettings
}

export interface CardsResponse {
  cards: SimCard[]
  /** 管理员视图为 true，包含全部私有字段。 */
  full: boolean
}
