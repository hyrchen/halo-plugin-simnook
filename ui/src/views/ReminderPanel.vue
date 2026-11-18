<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { axiosInstance } from '@halo-dev/api-client'
import { Dialog, Toast, VAlert, VButton, VDescription, VDescriptionItem, VDropdownItem, VEmpty, VEntity, VEntityContainer, VEntityField, VLoading, VModal, VSpace, VSwitch } from '@halo-dev/components'
import { ReminderChannelsApi, type ReminderChannelInput, type ReminderChannelView, type ReminderChannelsView } from '../api/generated'
import { useSimnookI18n } from '../locales'

const { t } = useSimnookI18n()
const api = new ReminderChannelsApi(undefined, '', axiosInstance)
const state = ref<ReminderChannelsView>()
const loading = ref(false), failed = ref(false), busy = ref(''), saving = ref(false)
const visible = ref(false), editorError = ref('')
const draft = ref<ReminderChannelInput>({})
const identity = ref<Pick<ReminderChannelInput, 'name' | 'version'>>({})
const eventOptions = computed(() => [
 { value: 'upcoming', label: t('即将到期') }, { value: 'window', label: t('续费窗口开放') }, { value: 'overdue', label: t('已逾期') },
])
// 0 表示续费起始日当天；只在勾选的这些天提醒即将到期。
const dayOptions = computed(() => [60, 30, 15, 7, 3, 1, 0].map(value => ({ value, label: value ? t('提前天数', { days: value }) : t('到期当天') })))
const scopeOptions = computed(() => [{ value: 'all', label: t('全部号卡') }, { value: 'selected', label: t('指定号卡') }])
const recipients = computed(() => {
 const options = (state.value?.recipients ?? []).filter(user => user.name).map(user => ({ value: user.name!, label: user.displayName ? `${user.displayName} (${user.name})` : user.name! }))
 for (const name of draft.value.recipients ?? []) if (!options.some(option => option.value === name)) options.push({ value: name, label: `${name} · ${t('用户不可用')}` })
 return options
})
const cardOptions = computed(() => {
 const options = (state.value?.cards ?? []).filter(card => card.name).map(card => ({ value: card.name!, label: card.displayName || card.name! }))
 for (const name of draft.value.cards ?? []) if (!options.some(option => option.value === name)) options.push({ value: name, label: `${name} · ${t('号卡已删除')}` })
 return options
})
const recipientLabel = (name: string) => state.value?.recipients?.find(user => user.name === name)?.displayName || name
const cardLabel = (name: string) => state.value?.cards?.find(card => card.name === name)?.displayName || name
const eventLabel = (values: string[] = []) => values.map(value => eventOptions.value.find(option => option.value === value)?.label || value).join('、')
const daysLabel = (values: number[] = []) => values.map(value => dayOptions.value.find(option => option.value === value)?.label || value).join('、')
const scopeLabel = (channel: ReminderChannelView) => channel.cardScope === 'selected' ? (channel.cards ?? []).map(cardLabel).join('、') : t('全部号卡')
const dateLabel = (value?: string) => value ? new Date(value).toLocaleString() : t('尚未发送')
function errorMessage(error: unknown) {
 const data = (error as { response?: { data?: { detail?: string; message?: string } } }).response?.data
 return data?.detail || data?.message || t('提醒设置保存失败')
}
async function load() {
 loading.value = true; failed.value = false
 try { state.value = (await api.listReminderChannels()).data }
 catch (error) { failed.value = true; console.error(error) }
 finally { loading.value = false }
}
function edit(channel?: ReminderChannelView) {
 identity.value = channel ? { name: channel.name, version: channel.version } : {}
 draft.value = channel
  ? { name: channel.name, version: channel.version, displayName: channel.displayName, enabled: channel.enabled, recipients: [...channel.recipients ?? []], events: [...channel.events ?? []],
    sendTime: channel.sendTime, cardScope: channel.cardScope, cards: [...channel.cards ?? []], daysAhead: [...channel.daysAhead ?? []] }
  : { displayName: '', enabled: true, recipients: [], events: ['upcoming', 'window', 'overdue'], sendTime: '09:00', cardScope: 'all', cards: [], daysAhead: [7, 3, 1] }
 editorError.value = ''; visible.value = true
}
async function save() {
 if (saving.value) return
 saving.value = true; editorError.value = ''
 try {
  const input = { ...draft.value, ...identity.value }
  // FormKit 复选框可能返回字符串，天数统一转为数字。
  input.daysAhead = (input.daysAhead ?? []).map(Number)
  if (input.cardScope !== 'selected') input.cards = []
  await api.saveReminderChannel({ reminderChannelInput: input })
  visible.value = false; Toast.success(t('已保存')); await load()
 } catch (error) { editorError.value = errorMessage(error); console.error(error) }
 finally { saving.value = false }
}
async function toggle(channel: ReminderChannelView, enabled: boolean) {
 if (busy.value || !channel.name) return
 busy.value = channel.name
 try {
  await api.saveReminderChannel({ reminderChannelInput: { name: channel.name, version: channel.version, displayName: channel.displayName, recipients: channel.recipients, events: channel.events,
   sendTime: channel.sendTime, cardScope: channel.cardScope, cards: channel.cards, daysAhead: channel.daysAhead, enabled } })
  Toast.success(t('已保存')); await load()
 } catch (error) { Toast.error(errorMessage(error)); console.error(error) }
 finally { busy.value = '' }
}
function remove(channel: ReminderChannelView) {
 Dialog.warning({ title: t('删除通知渠道'), description: t('删除渠道说明', { name: channel.displayName }), confirmText: t('删除'), showCancel: true,
  onConfirm: async () => {
   if (busy.value || !channel.name || channel.version === undefined) return
   busy.value = channel.name
   try { await api.removeReminderChannel({ name: channel.name, version: channel.version }); Toast.success(t('已删除')); await load() }
   catch (error) { Toast.error(errorMessage(error)); console.error(error) }
   finally { busy.value = '' }
  },
 })
}
async function test(channel: ReminderChannelView) {
 if (busy.value || !channel.name) return
 busy.value = channel.name
 try {
  const { data } = await api.testReminderChannel({ name: channel.name })
  if (data.lastTestError) Toast.error(data.lastTestError)
  else Toast.success(t('测试已提交给 Halo'))
  await load()
 } catch (error) { Toast.error(errorMessage(error)); console.error(error) }
 finally { busy.value = '' }
}
function submit() { (document.getElementById('simnook-channel') as HTMLFormElement | null)?.requestSubmit() }
const ready = computed(() => !loading.value && !failed.value && !busy.value)
// 页头的「添加渠道」按钮由父组件渲染，通过这里打开编辑器。
defineExpose({ add: () => edit(), ready })
onMounted(load)
</script>

<template>
 <div class="w-full p-4">
  <VLoading v-if="loading" />
  <VEmpty v-else-if="failed" :title="t('提醒状态加载失败')"><template #actions><VButton @click="load">{{ t('重试') }}</VButton></template></VEmpty>
  <template v-else-if="state">
   <VEmpty v-if="!state.channels?.length" :title="t('暂无通知渠道')" :message="t('添加渠道说明')" />
   <VEntityContainer v-else>
    <VEntity v-for="channel in state.channels" :key="channel.name">
     <template #start>
      <VEntityField :title="channel.displayName" :description="`Halo · ${(channel.recipients ?? []).map(recipientLabel).join('、')}`" />
     </template>
     <template #end>
      <VButton size="sm" :loading="busy === channel.name" :disabled="Boolean(busy)" @click="test(channel)">{{ t('发送测试') }}</VButton>
      <VSwitch :model-value="channel.enabled" :disabled="Boolean(busy)" :aria-label="`${t('启用')} ${channel.displayName}`" @update:model-value="toggle(channel, $event)" />
     </template>
     <template #dropdownItems>
      <VDropdownItem :disabled="Boolean(busy)" @click="edit(channel)">{{ t('编辑') }}</VDropdownItem>
      <VDropdownItem :disabled="Boolean(busy)" type="danger" @click="remove(channel)">{{ t('删除') }}</VDropdownItem>
     </template>
     <template #footer>
      <VSpace direction="column" align="stretch" spacing="sm" class="w-full p-4">
       <VDescription>
        <VDescriptionItem :label="t('每日发送时间')" :content="channel.sendTime" />
        <VDescriptionItem :label="t('提醒号卡')" :content="scopeLabel(channel)" />
        <VDescriptionItem :label="t('接收提醒')" :content="eventLabel(channel.events)" />
        <VDescriptionItem v-if="channel.events?.includes('upcoming')" :label="t('提前提醒天数')" :content="daysLabel(channel.daysAhead)" />
        <VDescriptionItem v-if="channel.enabled" :label="t('下次检查时间')" :content="dateLabel(channel.nextRunAt)" />
        <VDescriptionItem :label="t('上次提交给 Halo')" :content="dateLabel(channel.lastSubmittedAt)" />
        <VDescriptionItem v-if="channel.lastTestAt" :label="t('上次测试')" :content="dateLabel(channel.lastTestAt)" />
       </VDescription>
       <VAlert v-if="channel.lastError" type="error" :description="channel.lastError" />
       <VAlert v-if="channel.lastTestError" type="error" :description="channel.lastTestError" />
      </VSpace>
     </template>
    </VEntity>
   </VEntityContainer>
  </template>
 </div>
 <VModal v-if="visible" :title="t(draft.name ? '编辑通知渠道' : '添加通知渠道')" :width="640" mount-to-body :layer-closable="false" @close="!saving && (visible = false)">
  <VAlert v-if="editorError" type="error" class="mb-4" :description="editorError" />
  <FormKit id="simnook-channel" v-model="draft" type="form" :actions="false" :disabled="saving" @submit="save">
   <FormKit name="displayName" type="text" :label="t('渠道名称')" validation="required|length:1,100" />
   <FormKit name="recipients" type="select" :label="t('接收用户')" :options="recipients" multiple searchable validation="required" :help="t('接收用户说明')" />
   <FormKit name="sendTime" type="time" :label="t('每日发送时间')" validation="required" :help="t('发送时间说明')" />
   <FormKit name="cardScope" type="radio" :label="t('提醒号卡')" :options="scopeOptions" />
   <FormKit v-if="draft.cardScope === 'selected'" name="cards" type="select" :label="t('选择号卡')" :options="cardOptions" multiple searchable validation="required" />
   <FormKit name="events" type="checkbox" :label="t('接收提醒')" :options="eventOptions" validation="required" />
   <FormKit v-if="draft.events?.includes('upcoming')" name="daysAhead" type="checkbox" :label="t('提前提醒天数')" :options="dayOptions" validation="required" :help="t('提前天数说明')" />
   <FormKit name="enabled" type="checkbox" :label="t('启用渠道')" />
  </FormKit>
  <template #footer>
   <VSpace><VButton type="secondary" :loading="saving" @click="submit">{{ t('保存渠道') }}</VButton><VButton :disabled="saving" @click="visible = false">{{ t('取消') }}</VButton></VSpace>
  </template>
 </VModal>
</template>
