/**
 * Simplified Chinese UI strings: the source dictionary. Its shape is the message schema that
 * `en.ts` must match. vue-i18n syntax: `{name}` interpolates, `|` separates plural forms
 * (`t(key, n)` exposes `n` as `{n}`), and `{ } @ $ |` must be escaped as `{'@'}` when meant literally.
 */
export default {
  halo: {
    pendingConversion: '待折算',
    missingRate: '缺少参考汇率',
    missingRateCards: '{count} 张号卡缺少参考汇率，未计入估值。',
    managerOnly: '仅号卡管理员可见',
    managementLoadFailed: '管理视图加载失败：{status}',
  },
  common: {
    listSeparator: '、',
    noData: '暂无数据',
    backToTop: '返回顶部',
    close: '关闭',
  },

  header: {
    theme: {
      auto: '自动主题',
      light: '浅色主题',
      dark: '深色主题',
    },
    // Names the language the toggle switches to, written in that language
    switchLanguage: 'English',
    admin: '后台管理',
  },

  home: {
    allNumbers: '全部号码',
    cardStyle: '卡片样式',
    cardStyleTitle: '{style}样式',
    searchPlaceholder: '搜索运营商、国家、号码后 4 位',
    connectionErrorTitle: '连接服务器失败',
    connectionErrorBody: '请检查网络设置或刷新页面后再试。',
    noMatch: '没有找到匹配的号码',
    empty: '暂无号码',
  },

  continents: {
    asia: '亚洲',
    europe: '欧洲',
    northAmerica: '北美洲',
    southAmerica: '南美洲',
    africa: '非洲',
    oceania: '大洋洲',
    antarctica: '南极洲',
    other: '其他',
  },

  cardStyles: {
    namecard: '名片',
    dial: '表盘',
    bar: '横条',
  },

  // 与后端 messages_zh_CN.properties 的 type.* 保持一致，前后台显示同一文案。
  cardKinds: {
    physical: '实体 SIM',
    adapter: 'eSIM · 卡托',
    embedded: 'eSIM · 直写手机',
    virtual: '虚拟号码（仅 App）',
  },

  status: {
    active: '使用中',
    expired: '已过期',
    cancelled: '已注销',
    // Fits inside the dial ring
    short: {
      expired: '过期',
      cancelled: '注销',
    },
  },

  keepAlive: {
    topup: '充值余额',
    addon: '购买/启用增值服务',
    use: '有效使用（含收发短信）',
    balanceChange: '产生余额变动（如发漫游短信）',
    autoCharge: '自动扣费',
    plan: '购买续费套餐',
  },

  recordKinds: {
    purchase: '首次购买/开卡',
    renewal: '续费/保号',
    topup: '额外充值',
    other: '其他',
  },

  cycle: {
    every: '每 {n} 天',
    varies: '不固定（取决于充值/服务组合）',
  },

  billingPeriod: {
    monthly: '月',
    quarterly: '季',
    semi_annual: '半年',
    annual: '年',
    biennial: '两年',
    triennial: '三年',
    quinquennial: '五年',
  },

  days: {
    count: '{n} 天',
    overdue: '过期 {n} 天',
    today: '今天',
    // Unit shown under or after a bare number
    unit: '天',
  },

  validity: {
    cancelledOn: '{date} 注销',
    noRecord: '没有有效期记录',
    tooltip: '本期 {total}，还剩 {left} · 至 {end}',
  },

  keepCost: {
    noMethod: '未记录保号方式',
    variesShort: '不固定',
    varies: '周期不固定',
    freeShort: '免费',
    free: '免费保号',
    freeEvery: '每 {n} 天免费',
    perYear: '{amount}/年',
    perCycle: '{amount} / {days}',
  },

  balance: {
    none: '未记录余额',
    noUpdateDate: '未记录更新时间',
    updatedToday: '今天更新',
    updatedDaysAgo: '{days}前更新',
  },

  hold: {
    noDate: '未记录入手日期',
    since: '{date} 入手',
  },

  card: {
    firstPurchase: '首次购买 {amount}',
    heldFor: '已持有 {days}',
    remaining: '剩余',
    keepPerYear: '每年保号',
    validity: '有效期',
    until: '至 {date}',
    thisPeriod: '本期 {days}',
    rows: {
      expiry: '到期',
      purchase: '购入',
      keep: '保号',
    },
  },

  summary: {
    activeNumbers: '在用号码',
    cardUnit: '张',
    countries: '覆盖国家或地区',
    countryUnit: '个',
    totalValue: '总价值',
    monthlyCost: '月均支出',
    perMonth: '{currency}/月',
    remainingValue: '剩余价值',
    todayRates: '今日汇率',
    rateBaseCurrency: '切换汇率基准币种',
    totalSpent: '累计花费',
    spendPurchase: '首购',
    spendRenewal: '续费',
    spendOther: '其他',
    upcomingWithin: '{days}内续费',
    noUpcoming: '{days}内没有要续费的号码',
    nextRenewal: '即将续费',
    overdueUnit: '天前到期',
    inDaysUnit: '天后',
  },

  globe: {
    activeCards: '{n} 张',
    days: '{n}天',
    mapLoadFailed: '地图资源加载失败',
  },

  detail: {
    notFound: '号码不存在或已被删除',
    backHome: '返回首页',
    totalSpent: '累计花费 {amount}',
    free: '免费',
    notApplicable: '不适用',
    perMonth: '{currency} / 月',
    locked: '登录后可见',
    noIssues: '没有问题',
    metrics: {
      price: '续费价格',
      monthly: '月均支出',
      remaining: '剩余时间',
      remainingValue: '剩余价值',
    },
    sections: {
      number: '号码信息',
      keepAlive: '保号信息',
      balance: '余额信息',
      account: '账号信息',
    },
    fields: {
      number: '号码',
      country: '国家/地区',
      type: '卡片类型',
      device: '使用设备',
      keepAlive: '保号方式',
      cycle: '续费周期',
      nextRenewal: '下次续费',
      expiresAt: '有效期至',
      balance: '当前余额',
      balanceUpdatedAt: '余额更新',
      held: '已持有',
      account: '管理账号',
      services: '绑定的服务',
      dataCheck: '数据检查',
    },
  },

  history: {
    title: '续费历史',
    count: '{n} 笔',
    empty: '还没有消费记录',
    free: '免费',
    extendsTo: '有效期延至 {date}',
    legend: {
      purchase: '首次购买',
      renewal: '续费 / 保号',
      topup: '额外充值',
      other: '其他',
    },
    columns: {
      date: '日期',
      type: '类型',
      amount: '金额',
      amountCNY: '等额人民币',
      extendsTo: '有效期延至',
    },
  },

  visitor: {
    anonymous: '网络访客',
    unknownLocation: '未知位置',
    ipUnavailable: '暂无法获取',
    networkUnavailable: '网络信息不可用',
    unknownIsp: '未知运营商',
    unknownBrowser: '未知浏览器',
    devices: {
      desktop: '桌面设备',
      android: 'Android 手机',
      iphone: 'iPhone',
      ipad: 'iPad',
      tablet: '平板电脑',
    },
  },

  errors: {
    http: {
      requestFailed: '请求失败（{status}）',
    },
  },
}
