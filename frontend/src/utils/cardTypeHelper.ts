import type { CardKind, TypeColor } from '@shared/types'
import { i18n } from '@/i18n'

const KIND_ICONS: Record<CardKind, string> = {
  physical: 'tabler:device-sim',
  adapter: 'tabler:device-sd-card',
  embedded: 'tabler:device-mobile',
  virtual: 'tabler:cloud',
}

/** 类型图标颜色到 Tailwind 文字颜色的映射 */
const COLOR_CLASSES: Record<TypeColor, string> = {
  default: 'text-slate-500',
  gray: 'text-slate-500',
  brown: 'text-amber-700 dark:text-amber-500',
  orange: 'text-orange-500',
  yellow: 'text-yellow-500',
  green: 'text-emerald-600',
  blue: 'text-sky-500',
  purple: 'text-violet-500',
  pink: 'text-pink-500',
  red: 'text-rose-500',
}

/** 访客视图中卡片类型未公开时为 null：显示通用图标、不显示类型名，避免误显示为实体 SIM。 */
export function getKindIcon(kind: CardKind | null): string {
  return kind ? KIND_ICONS[kind] ?? KIND_ICONS.physical : 'tabler:lock'
}

export function getKindName(kind: CardKind | null): string {
  return kind ? i18n.global.t(`cardKinds.${kind in KIND_ICONS ? kind : 'physical'}`) : ''
}

export function getTypeColorClass(color: TypeColor): string {
  return COLOR_CLASSES[color] ?? COLOR_CLASSES.default
}
