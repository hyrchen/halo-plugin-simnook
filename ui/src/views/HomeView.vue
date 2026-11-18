<script setup lang="ts">
import { useSimnookI18n } from '../locales'
import { computed, ref, onMounted, watch, nextTick } from 'vue'
import { axiosInstance } from '@halo-dev/api-client'
import { Dialog, Toast, VAlert, VAvatar, VPageHeader, VCard, VButton, VDropdownDivider, VDropdownItem, VEntityContainer, VEntity, VEntityField, VPagination, VLoading, VEmpty, VModal, VSpace, VStatusDot, VTabbar, VTag } from '@halo-dev/components'
import { CardSpecPublicFieldsEnum, SimCardV1alpha1Api, SimnookManagementV1alpha1Api, type SimCard, type CardSpec, type ManagedCardView } from '../api/generated'
import ExpenseFields from './ExpenseFields.vue'
import QuickRenewal from './QuickRenewal.vue'
import ReminderPanel from './ReminderPanel.vue'
import { filterCardViews, tidyPhoneNumber, URGENCY_CODES } from '../utils/cardManagement'
import { BUILTIN_CARRIERS, builtinLogoOptions, carrierLogo } from '../../../frontend/shared/carriers'
import { CYCLE_OPTIONS, cardName, copyCardConfiguration, cycleChoice, entryExpiry, localDay } from '../utils/cardEntry'

const { t, locale, label, recordTypeLabel } = useSimnookI18n()

const api = new SimCardV1alpha1Api(undefined, '', axiosInstance)
const managementApi = new SimnookManagementV1alpha1Api(undefined, '', axiosInstance)
const tab = ref('cards')
const reminderPanel = ref<InstanceType<typeof ReminderPanel>>()
const tabs = computed(() => [{ id: 'cards', label: t('号卡') }, { id: 'todo', label: t('续费提醒') }])
const cards = ref<ManagedCardView[]>([])
const page = ref(1), size = ref(10)
const keyword = ref('')
const countryFilter = ref<string>(), statusFilter = ref<string>(), urgencyFilter = ref<string>(), typeFilter = ref<string>()
const renewing = ref<ManagedCardView>()
const urgencyLabels: Record<string, string> = { window: '续费窗口开放', overdue: '已逾期', days3: '未来 3 天内', days7: '未来 7 天内', days15: '未来 15 天内', days30: '未来 30 天内', days60: '未来 60 天内', beyond: '超过 60 天', noDate: '无续费日期', cancelled: '已注销' }
const filteredCards = computed(() => filterCardViews(cards.value, { keyword: keyword.value, country: countryFilter.value ?? '', status: statusFilter.value ?? '', urgency: urgencyFilter.value ?? '', type: typeFilter.value ?? '' }))
const total = computed(() => filteredCards.value.length)
const pagedCards = computed(() => filteredCards.value.slice((page.value - 1) * size.value, page.value * size.value))
const countryOptions = computed(() => [...new Set(cards.value.map(card => card.countryName).filter((value): value is string => Boolean(value)))].sort().map(value => ({ value, label: value })))
const urgencyState = (code?: string) =>
 code === 'overdue' ? 'error' : ['window', 'days3', 'days7'].includes(code ?? '') ? 'warning' : ['noDate', 'cancelled'].includes(code ?? '') ? 'default' : 'success'
const renewalDate = (card: ManagedCardView) => [card.nextRenewalAt, card.nextRenewalEnd].filter(Boolean).join(' ~ ')
const cardLogo = (card: ManagedCardView) => card.logo || carrierLogo(card.carrier)
function clearFilters() { keyword.value = ''; countryFilter.value = undefined; statusFilter.value = undefined; urgencyFilter.value = undefined; typeFilter.value = undefined }
const loading = ref(false), saving = ref(false), visible = ref(false), conflict = ref(false), loadError = ref(false)
const deleting = ref('')
const editing = ref<SimCard>()
const form = ref<CardSpec>({ name: '', records: [] })
const formTab = ref('basic')
const formTabs = computed(() => [{ id: 'basic', label: t('基本信息') }, { id: 'renewal', label: t('续费与消费') }, { id: 'personal', label: t('个人信息') }, { id: 'appearance', label: t('外观与排序') }, { id: 'visibility', label: t('公开设置') }])
// 字段清单与顺序来自后端模型生成的枚举；勾选的字段对访客公开，未勾选的只有管理员可见。
const PUBLIC_FIELDS = Object.values(CardSpecPublicFieldsEnum)
// 未设置过公开列表的号卡，沿用早期版本访客可见的字段。
const DEFAULT_PUBLIC_FIELDS: CardSpecPublicFieldsEnum[] = ['name', 'carrier', 'last4', 'country', 'type', 'keepAlive', 'cycle', 'price', 'dates', 'firstPurchase', 'history']
const resetPublicFields = () => { form.value.publicFields = [...DEFAULT_PUBLIC_FIELDS] }
const publicFieldOptions = computed(() => PUBLIC_FIELDS.map(value => ({ value, label: t(({
 name: '展示名称', carrier: '运营商与图标', last4: '号码末四位', number: '完整号码', country: '号码国家', type: '卡片类型', keepAlive: '保号动作',
 cycle: '续费周期', price: '续费价格', dates: '有效期与续费日期', firstPurchase: '首次购买日期', history: '消费记录', device: '主要使用设备',
 balance: '余额与更新日期', account: '账户名称', services: '绑定的服务',
} as Record<string, string>)[value]!) })))
// FormKit 文本输入默认有约 20ms 防抖，失焦时等它把最后的输入写回模型后再整理，否则会被原值覆盖。
function tidyNumber() { setTimeout(() => { form.value.number = tidyPhoneNumber(form.value.number) }, 50) }
const cycle = ref('')
let initializing = false
const checking = ref('')
const check = ref<ManagedCardView>()
const typeOptions = computed(() => [
 { label: t('实体 SIM'), value: 'physical' },
 { label: t('eSIM · 直写手机'), value: 'embedded' },
 { label: t('eSIM · 卡托'), value: 'adapter' },
 { label: t('虚拟号码（仅 App）'), value: 'virtual' },
] as const)
const typeColors = computed(() => [
 { label: t('中性（默认）'), value: 'default' },
 { label: t('灰色'), value: 'gray' },
 { label: t('棕色'), value: 'brown' },
 { label: t('橙色'), value: 'orange' },
 { label: t('黄色'), value: 'yellow' },
 { label: t('绿色'), value: 'green' },
 { label: t('蓝色'), value: 'blue' },
 { label: t('紫色'), value: 'purple' },
 { label: t('粉色'), value: 'pink' },
 { label: t('红色'), value: 'red' },
])
const keepAliveOptions = ['充值余额', '购买/启用增值服务', '有效使用（含收发短信）', '产生余额变动（如发漫游短信）', '自动扣费', '购买续费套餐']
const devices = ['主力机', '备用机', '卡托', '未装机']
// 常用服务作预设；其他服务可在选择框中直接输入名称后回车添加。
const services = ['Telegram', 'WhatsApp', 'Signal', 'Apple ID', 'Google', '银行/支付']
const countries = [
 '🇨🇳 中国', '🇭🇰 中国香港', '🇲🇴 中国澳门', '🇹🇼 中国台湾', '🇯🇵 日本', '🇰🇷 韩国', '🇸🇬 新加坡', '🇲🇾 马来西亚',
 '🇹🇭 泰国', '🇵🇭 菲律宾', '🇮🇩 印度尼西亚', '🇻🇳 越南', '🇱🇰 斯里兰卡', '🇦🇪 阿联酋', '🇹🇷 土耳其', '🇬🇧 英国',
 '🇩🇪 德国', '🇫🇷 法国', '🇳🇱 荷兰', '🇪🇸 西班牙', '🇮🇹 意大利', '🇵🇱 波兰', '🇪🇪 爱沙尼亚', '🇨🇭 瑞士',
 '🇺🇸 美国', '🇨🇦 加拿大', '🇲🇽 墨西哥', '🇧🇷 巴西', '🇦🇺 澳大利亚', '🇳🇿 新西兰',
]
const currencies = 'CNY HKD MOP TWD JPY KRW SGD MYR THB PHP IDR VND LKR AED TRY GBP EUR PLN CHF USD CAD MXN BRL AUD NZD'.split(' ')
const recordTypes = ['首次购买/开卡', '续费/保号', '额外充值', '其他费用']
const recordLabels = [{ type: 'text', label: '$value.date' }, { type: 'text', label: '$value.type' }, { type: 'text', label: '$value.amount' }, { type: 'text', label: '$value.currency' }]
const countryCurrencies: Record<string, string> = {
 '🇨🇳': 'CNY', '🇭🇰': 'HKD', '🇲🇴': 'MOP', '🇹🇼': 'TWD', '🇯🇵': 'JPY', '🇰🇷': 'KRW',
 '🇸🇬': 'SGD', '🇲🇾': 'MYR', '🇹🇭': 'THB', '🇵🇭': 'PHP', '🇮🇩': 'IDR', '🇻🇳': 'VND',
 '🇱🇰': 'LKR', '🇦🇪': 'AED', '🇹🇷': 'TRY', '🇬🇧': 'GBP', '🇩🇪': 'EUR', '🇫🇷': 'EUR',
 '🇳🇱': 'EUR', '🇪🇸': 'EUR', '🇮🇹': 'EUR', '🇵🇱': 'PLN', '🇪🇪': 'EUR', '🇨🇭': 'CHF',
 '🇺🇸': 'USD', '🇨🇦': 'CAD', '🇲🇽': 'MXN', '🇧🇷': 'BRL', '🇦🇺': 'AUD', '🇳🇿': 'NZD',
}
const withExisting = (options: string[], ...existing: (string | undefined)[]) =>
 [...new Set([...options, ...existing.filter((value): value is string => Boolean(value))])]
  .map(value => ({ value, label: options.includes(value) ? label(value) : value }))
const logoPreview = computed(() => form.value.logo || carrierLogo(form.value.carrier))
const builtinLogo = computed({
 get: () => builtinLogoOptions.some(option => option.value === form.value.logo) ? form.value.logo : '',
 set: (value: string | undefined) => {
  if (value) form.value.logo = value
  else if (builtinLogoOptions.some(option => option.value === form.value.logo)) form.value.logo = undefined
 },
})
const suggestedName = computed(() => cardName(form.value.carrier, form.value.number))
watch(suggestedName, (value, previous) => {
 if (!initializing && (!form.value.name || form.value.name === previous)) form.value.name = value
})
watch(() => form.value.countryName, (value, previous) => {
 if (initializing || editing.value) return
 const currency = countryCurrencies[value?.slice(0, 4) ?? '']
 const previousCurrency = countryCurrencies[previous?.slice(0, 4) ?? '']
 if (currency && (!form.value.currency || form.value.currency === previousCurrency)) form.value.currency = currency
})
function selectCycle(value: string) {
 if (value === cycle.value) return
 cycle.value = value
 if (value === 'custom') return
 form.value.cycleDays = value === 'varies' ? 0 : value ? Number(value) : undefined
 form.value.cycleLabel = value === 'varies' ? '不固定（取决于充值/服务组合）' : CYCLE_OPTIONS.find(option => option.value === value)?.label
}
let sequence = 0
const clone = <T,>(v: T): T => JSON.parse(JSON.stringify(v))
async function load() {
 const current = ++sequence
 loading.value = true; loadError.value = false
 try {
  const { data } = await managementApi.listCardViews({ headers: { 'Accept-Language': locale.value } })
  if (current !== sequence) return
  cards.value = data.cards ?? []
  const lastPage = Math.max(1, Math.ceil(total.value / size.value))
  if (page.value > lastPage) page.value = lastPage
 } catch (error) { if (current === sequence) { loadError.value = true; Toast.error(t('加载失败，请重试')); console.error(error) } }
 finally { if (current === sequence) loading.value = false }
}
async function openCard(card: ManagedCardView, copy = false) {
 try {
  const { data } = await api.getSimCard({ name: card.id! })
  await edit(copy ? undefined : data, copy ? data.spec : undefined)
 } catch (error) { Toast.error(t('加载失败，请重试')); console.error(error) }
}
async function edit(card?: SimCard, template?: CardSpec) {
 tab.value = 'cards'
 initializing = true
 editing.value = card ? clone(card) : undefined
 form.value = card ? clone(card.spec) : template ? copyCardConfiguration(template) : { name: '', records: [] }
 for (const record of form.value.records ?? []) record.id ||= crypto.randomUUID()
 if (form.value.region) form.value.countryName = [form.value.region, form.value.countryName].filter(Boolean).join(' ')
 if (!card) form.value.name = cardName(form.value.carrier, form.value.number)
 cycle.value = cycleChoice(form.value)
 // 公开列表在表单里始终是完整清单；未设置过的号卡显示默认公开字段。
 form.value.publicFields = [...(form.value.publicFields ?? DEFAULT_PUBLIC_FIELDS)]
 formTab.value = 'basic'
 conflict.value = false; visible.value = true
 await nextTick()
 initializing = false
}
async function reloadEditing() {
 if (!editing.value) return
 try { const { data } = await api.getSimCard({ name: editing.value.metadata.name }); await edit(data); Toast.info(t('已加载最新版本，请重新编辑')) }
 catch (error) { Toast.error(t('重新加载失败，卡片可能已删除')); console.error(error) }
}
function normalize(spec: CardSpec): CardSpec {
 const result = clone(spec) as CardSpec & Record<string, unknown>
 for (const key of ['price', 'balance', 'cycleDays', 'order']) {
  if (result[key] === '' || result[key] == null) delete result[key]
  else result[key] = Number(result[key])
 }
 for (const key of ['nextRenewalAt', 'nextRenewalEnd', 'cancelledAt', 'balanceUpdatedAt', 'currency', 'typeColor', 'typeKind']) {
  if (!result[key]) delete result[key]
 }
 const country = result.countryName?.trim() ?? ''
 result.number = tidyPhoneNumber(result.number)
 result.publicFields = PUBLIC_FIELDS.filter(field => result.publicFields?.includes(field))
 const flag = country.match(/^[\u{1F1E6}-\u{1F1FF}]{2}/u)?.[0] ?? ''
 result.region = flag || undefined
 result.countryName = country.slice(flag.length).trim() || undefined
 result.records = (result.records ?? []).map(row => {
  const record = { ...row, id: row.id || crypto.randomUUID() } as typeof row & Record<string, unknown>
  for (const key of ['type', 'currency', 'date', 'extendsTo']) {
   if (!record[key]) delete record[key]
  }
  for (const key of ['amount', 'rate']) {
   if (record[key] === '' || record[key] == null) delete record[key]
   else record[key] = Number(record[key])
  }
  return record
 })
 return result
}
function validRenewalWindow(spec: CardSpec): boolean {
 if (!spec.nextRenewalEnd || spec.cancelledAt) return true
 const start = spec.nextRenewalAt || entryExpiry(spec)
 if (start && spec.nextRenewalEnd >= start) return true
 formTab.value = 'renewal'
 Toast.warning(t('请填写续费起始日，且截止日不能早于起始日'))
 return false
}
function showInvalidTab() {
 formTab.value = !form.value.name?.trim() || form.value.name.length > 100 ? 'basic' : 'renewal'
}
async function inspect(card: ManagedCardView) {
 if (checking.value) return
 checking.value = card.id!
 try {
  const { data } = await managementApi.getCardView({ name: card.id! }, { headers: { 'Accept-Language': locale.value } })
  check.value = data
 } catch (error) { Toast.error(t('数据检查加载失败')); console.error(error) }
 finally { checking.value = '' }
}
async function save() {
 if (saving.value || conflict.value) return
 saving.value = true
 try {
  const spec = normalize(form.value)
  if (!validRenewalWindow(spec)) return
  const resource: SimCard = editing.value ? { ...editing.value, spec } : {
   apiVersion: 'simnook.halo.run/v1alpha1', kind: 'SimCard', metadata: { name: crypto.randomUUID() }, spec,
  }
  if (editing.value) await api.updateSimCard({ name: resource.metadata.name, simCard: resource })
  else await api.createSimCard({ simCard: resource })
  visible.value = false; Toast.success(t('已保存')); await load()
 } catch (error) {
  if ((error as { response?: { status: number } }).response?.status === 409) { conflict.value = true; Toast.warning(t('其他人已修改此卡片，请重新加载后编辑')) }
  else { Toast.error(t('保存失败，请检查表单或稍后重试')); console.error(error) }
 } finally { saving.value = false }
}
function remove(card: ManagedCardView) {
 Dialog.warning({ title: t('删除号卡'), description: t('删除号卡确认', { name: card.name }), confirmText: t('删除'), showCancel: true,
  onConfirm: async () => {
   deleting.value = card.id!
   try { await api.deleteSimCard({ name: card.id! }); Toast.success(t('已删除')); await load() }
   catch (error) { Toast.error(t('删除失败')); console.error(error) }
   finally { deleting.value = '' }
  },
 })
}
watch([keyword, countryFilter, statusFilter, urgencyFilter, typeFilter, size], () => { page.value = 1 })
watch(filteredCards, () => { page.value = Math.min(page.value, Math.max(1, Math.ceil(total.value / size.value))) })
watch(locale, load)
onMounted(load)
function submit() { (document.getElementById('simnook-form') as HTMLFormElement | null)?.requestSubmit() }
function openFrontend() { window.open('/simnook/', '_blank', 'noopener,noreferrer') }
</script>

<template>
 <VPageHeader title="SIMnook">
  <template #actions>
   <VButton @click="openFrontend">{{ t('打开前台页面') }}</VButton>
   <VButton v-if="tab === 'cards'" type="secondary" @click="edit()">{{ t('新增号卡') }}</VButton>
   <VButton v-else type="secondary" :disabled="!reminderPanel?.ready" @click="reminderPanel?.add()">{{ t('添加渠道') }}</VButton>
  </template>
 </VPageHeader>
 <div class="m-0 md:m-4">
 <VCard :body-class="['!p-0']">
  <template #header><VTabbar v-model:active-id="tab" :items="tabs" class="w-full" type="outline" /></template>
 <ReminderPanel v-if="tab === 'todo'" ref="reminderPanel" />
  <template v-else>
  <!-- 沿用 Halo 列表页筛选栏的结构：灰底条，搜索在左，筛选靠右；筛选顺序与列表从左到右的展示顺序一致。 -->
  <div class="block w-full bg-gray-50 px-4 py-3">
   <div class="relative flex flex-col flex-wrap items-start gap-4 sm:flex-row sm:items-center">
    <div class="flex w-full flex-1 items-center sm:w-auto"><SearchInput v-model="keyword" :placeholder="t('搜索名称、号码或运营商')" /></div>
    <VSpace spacing="lg" class="flex-wrap">
     <FilterCleanButton v-if="keyword || countryFilter || typeFilter || statusFilter || urgencyFilter" @click="clearFilters" />
     <FilterDropdown v-model="typeFilter" :label="t('卡片类型')" :items="[{ label: t('全部') }, ...typeOptions]" />
     <FilterDropdown v-model="countryFilter" :label="t('号码国家')" :items="[{ label: t('全部') }, ...countryOptions]" />
     <FilterDropdown v-model="urgencyFilter" :label="t('续费紧急度')" :items="[{ label: t('全部') }, ...URGENCY_CODES.map(value => ({ value, label: t(urgencyLabels[value]!) }))]" />
     <FilterDropdown v-model="statusFilter" :label="t('状态')" :items="[{ label: t('全部') }, { value: 'active', label: t('使用中') }, { value: 'expired', label: t('已过期') }, { value: 'cancelled', label: t('已注销') }]" />
    </VSpace>
   </div>
  </div>
  <VLoading v-if="loading" />
  <VEmpty v-else-if="loadError" :title="t('加载失败')" :message="t('请检查权限或网络后重试')"><template #actions><VButton @click="load">{{ t('重试') }}</VButton></template></VEmpty>
  <VEmpty v-else-if="!filteredCards.length" :title="t(cards.length ? '没有匹配的号卡' : '还没有号卡')" :message="t(cards.length ? '调整筛选条件查看其他号卡。' : '首次安装为空库，点击新增号卡开始录入。')" />
  <VEntityContainer v-else><VEntity v-for="card in pagedCards" :key="card.id">
   <template #start>
    <VEntityField><template #description><VAvatar :src="cardLogo(card)" :alt="card.carrier || card.name" size="md" /></template></VEntityField>
    <VEntityField :title="card.name" :description="[card.carrier?.trim(), card.number?.trim() || t('未填写号码')].filter(Boolean).join(' · ')" @click="openCard(card)">
     <template v-if="card.type || card.countryName" #extra><VSpace spacing="xs"><VTag v-if="card.type">{{ card.type }}</VTag><VTag v-if="card.countryName">{{ [card.region, card.countryName].filter(Boolean).join(' ') }}</VTag></VSpace></template>
    </VEntityField>
   </template>
   <template #end>
    <VEntityField v-if="renewalDate(card)" :description="renewalDate(card)" />
    <VEntityField :description="t('消费记录数量', card.history?.length || 0)" />
    <VEntityField v-if="card.status !== 'cancelled'"><template #description><VStatusDot :state="urgencyState(card.urgencyCode)" :animate="card.urgencyCode === 'overdue'" :text="card.urgency" /></template></VEntityField>
    <VEntityField><template #description><VTag :theme="card.status === 'active' ? 'default' : 'danger'">{{ card.statusLabel }}</VTag></template></VEntityField>
   </template>
   <template #dropdownItems>
    <VDropdownItem @click="openCard(card)">{{ t('编辑') }}</VDropdownItem>
    <VDropdownItem v-if="card.status !== 'cancelled'" @click="renewing = card">{{ t('快捷续费') }}</VDropdownItem>
    <VDropdownItem :disabled="Boolean(checking)" @click="inspect(card)">{{ t('数据检查') }}</VDropdownItem>
    <VDropdownItem @click="openCard(card, true)">{{ t('再添同款') }}</VDropdownItem>
    <VDropdownDivider />
    <VDropdownItem type="danger" :disabled="deleting === card.id" @click="remove(card)">{{ t('删除') }}</VDropdownItem>
   </template>
  </VEntity></VEntityContainer>
 </template>
  <template v-if="tab === 'cards'" #footer><VPagination v-model:page="page" v-model:size="size" :total="total" :size-options="[10, 20, 50]" :page-label="t('页')" :size-label="t('条 / 页')" :total-label="t('分页总数', { count: total })" /></template>
 </VCard></div>
 <QuickRenewal v-if="renewing?.id" :name="renewing.id" :today="renewing.asOfDate || localDay()" @close="renewing = undefined" @saved="renewing = undefined; load()" />
 <VModal v-if="visible" :title="editing ? t('编辑号卡') : t('新增号卡')" :width="760" mount-to-body :layer-closable="false" @close="!saving && (visible = false)">
  <VAlert v-if="conflict" type="warning" class="mb-4" :description="t('编辑冲突提示')"><template #actions><VButton size="sm" @click="reloadEditing">{{ t('重新加载') }}</VButton></template></VAlert>
  <VTabbar v-model:active-id="formTab" :items="formTabs" class="mb-4" type="outline" />
  <FormKit id="simnook-form" v-model="form" type="form" :actions="false" :config="{ validationVisibility: 'submit' }" @submit="save" @submit-invalid="showInvalidTab">
   <!-- 表单自带 divide-y，各分页包在同一个容器里，避免隐藏分页之间多出分割线。 -->
   <div>
   <div v-show="formTab === 'basic'">
    <FormKit name="carrier" type="select" :label="t('运营商')" :placeholder="t('选择或输入运营商')" :options="withExisting(BUILTIN_CARRIERS.map(item => item.name), form.carrier)" searchable allow-create clearable />
    <VSpace v-if="logoPreview" spacing="md" class="mb-4"><VAvatar :src="logoPreview" :alt="t('运营商图标预览')" size="md" /><span class="text-sm text-gray-500">{{ t('已匹配运营商图标，可在外观与排序中更换。') }}</span></VSpace>
    <div class="simnook-form-grid grid grid-cols-1 gap-x-4 md:grid-cols-2">
     <!-- FormKit 由宿主提供，组件级 @blur 不可靠；在外层捕获原生 focusout，离开输入框时整理号码。 -->
     <div @focusout="tidyNumber"><FormKit name="number" type="text" :label="t('完整号码')" :help="t('号码整理说明')" /></div>
     <FormKit name="countryName" type="select" :label="t('号码国家')" :placeholder="t('选择号码国家')" :options="withExisting(countries, form.countryName)" searchable allow-create clearable :help="t('自定义地区请用「国旗 emoji + 空格 + 名称」，用于地图定位。')" />
     <FormKit name="typeKind" type="select" :label="t('卡片类型')" :placeholder="t('选择卡片类型')" :options="typeOptions" clearable />
     <FormKit name="name" type="text" :label="t('展示名称')" validation="required|length:1,100" :help="t('自动生成「运营商 · 号码末四位」，可以修改。')" />
    </div>
   </div>
   <div v-show="formTab === 'renewal'">
    <FormKit name="keepAlive" type="select" :label="t('保号动作')" :options="withExisting(keepAliveOptions, ...(form.keepAlive ?? []))" multiple searchable allow-create />
    <FormKit :model-value="cycle" type="select" :label="t('续费周期')" :placeholder="t('选择续费周期')" :options="CYCLE_OPTIONS.map(option => ({ ...option, label: t(option.label) }))" :ignore="true" clearable @update:model-value="selectCycle" />
    <div class="simnook-form-grid grid grid-cols-1 gap-x-4 md:grid-cols-2">
     <FormKit v-if="cycle === 'custom'" name="cycleDays" type="number" :label="t('续费周期（天）')" validation="min:0" min="0" step="1" preserve />
     <FormKit v-if="cycle === 'custom'" name="cycleLabel" type="text" :label="t('周期说明')" preserve />
     <FormKit name="price" type="number" :label="t('续费价格')" validation="min:0" min="0" step="0.01" />
     <FormKit name="currency" type="select" :label="t('续费币种')" :placeholder="t('选择续费币种')" :options="withExisting(currencies, form.currency)" searchable allow-create clearable validation="matches:/^[A-Z]{3}$/" :help="t('新增时按号码国家推荐币种，可以修改。')" />
    </div>
    <template v-if="cycle !== 'custom'">
     <FormKit name="cycleDays" type="hidden" :value="form.cycleDays" preserve />
     <FormKit name="cycleLabel" type="hidden" :value="form.cycleLabel" preserve />
    </template>
    <div class="simnook-form-grid grid grid-cols-1 gap-x-4 md:grid-cols-2">
     <FormKit name="nextRenewalAt" type="date" :label="t('下次续费起始日')" :help="t('留空时跟随有效期。')" />
     <FormKit name="nextRenewalEnd" type="date" :label="t('续费截止日')" :help="t('可选。填写后，前台显示续费日期区间。')" />
     <FormKit name="cancelledAt" type="date" :label="t('停用日期')" :help="t('填写后自动标记为已注销。')" />
    </div>
    <FormKit name="records" type="array" :label="t('消费记录')" :max="1000" :item-labels="recordLabels">
     <template #default="{ value }">
      <ExpenseFields :record="value ?? {}" :initialize="Boolean(value)" :card="form" :types="withExisting(recordTypes, ...(form.records ?? []).map(record => record.type)).map(option => ({ ...option, label: recordTypeLabel(option.value) }))" :currencies="withExisting(currencies, ...(form.records ?? []).map(record => record.currency))" />
     </template>
    </FormKit>
   </div>
   <div v-show="formTab === 'personal'">
    <div class="simnook-form-grid grid grid-cols-1 gap-x-4 md:grid-cols-2">
     <FormKit name="balance" type="number" :label="t('余额')" step="0.01" />
     <FormKit name="balanceUpdatedAt" type="date" :label="t('余额更新日期')" />
     <FormKit name="device" type="select" :label="t('主要使用设备')" :placeholder="t('选择主要使用设备')" :options="withExisting(devices, form.device)" searchable allow-create clearable />
     <FormKit name="account" type="text" :label="t('账户名称')" :help="t('仅记录名称，不存储密码、密钥或令牌。')" />
    </div>
    <FormKit name="services" type="select" :label="t('绑定的服务')" :options="withExisting(services, ...(form.services ?? []))" multiple searchable allow-create :help="t('绑定服务说明')" />
   </div>
   <div v-show="formTab === 'appearance'">
    <div class="simnook-form-grid grid grid-cols-1 gap-x-4 md:grid-cols-2">
     <FormKit name="typeColor" type="select" :label="t('类型图标颜色')" :placeholder="t('随卡片类型')" :options="typeColors" clearable />
     <FormKit v-model="builtinLogo" type="select" :label="t('内置运营商图标')" :placeholder="t('按运营商自动匹配')" :options="builtinLogoOptions" :ignore="true" clearable />
     <FormKit name="order" type="number" :label="t('展示排序')" step="1" />
    </div>
    <FormKit name="logo" type="attachment" :label="t('自定义运营商图标')" :help="t('选择或上传后优先使用此图标。')" />
   </div>
   <div v-show="formTab === 'visibility'">
    <FormKit name="publicFields" type="checkbox" :label="t('公开字段')" :options="publicFieldOptions" />
    <VButton size="sm" @click="resetPublicFields">{{ t('恢复默认') }}</VButton>
   </div>
   </div>
  </FormKit>
  <template #footer><VButton type="secondary" :loading="saving" :disabled="conflict" @click="submit">{{ t('保存') }}</VButton></template>
 </VModal>
 <VModal v-if="check" :title="`${check.name} · ${t('数据检查')}`" :width="640" mount-to-body @close="check = undefined">
  <VEntityContainer>
   <VEntity>
    <template #start><VEntityField :title="check.status === 'cancelled' ? check.statusLabel : `${check.statusLabel} · ${check.urgency}`" :description="check.dataCheck || t('号卡没有检查提示')" /></template>
   </VEntity>
   <VEntity v-for="record in check.history?.filter(item => item.checks?.length)" :key="record.id">
    <template #start><VEntityField :title="record.title?.trim() || [record.date, recordTypeLabel(record.type)].map(value => value?.trim()).filter(Boolean).join(' · ') || t('未命名消费记录')" :description="record.checks?.join(locale.startsWith('zh') ? '；' : '; ')" /></template>
   </VEntity>
  </VEntityContainer>
 </VModal>
</template>

<style>
/* FormKit 的 first:pt-0 / last:pb-0 只认 DOM 首尾，并排时会让同一行字段上下错位，统一内边距。 */
.simnook-form-grid > .formkit-outer, .simnook-form-grid > div > .formkit-outer { padding-top: 0; padding-bottom: 1rem; }
</style>
