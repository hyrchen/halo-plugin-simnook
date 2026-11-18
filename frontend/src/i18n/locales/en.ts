/**
 * English UI strings; must provide every key of `zh-CN.ts` (enforced by `MessageSchema`).
 */
import type { MessageSchema } from '@/i18n'

const en: MessageSchema = {
  halo: {
    pendingConversion: 'Awaiting conversion',
    missingRate: 'Reference exchange rate unavailable',
    missingRateCards: '{count} card has no reference exchange rate and is excluded from estimates. | {count} cards have no reference exchange rate and are excluded from estimates.',
    managerOnly: 'Card managers only',
    managementLoadFailed: 'Failed to load management view: {status}',
  },
  common: {
    listSeparator: ', ',
    noData: 'No data',
    backToTop: 'Back to top',
    close: 'Close',
  },

  header: {
    theme: {
      auto: 'System theme',
      light: 'Light theme',
      dark: 'Dark theme',
    },
    switchLanguage: '中文',
    admin: 'Admin',
  },

  home: {
    allNumbers: 'All numbers',
    cardStyle: 'Card style',
    cardStyleTitle: '{style} view',
    searchPlaceholder: 'Search carrier, country, or last 4 digits',
    connectionErrorTitle: 'Failed to connect to server',
    connectionErrorBody: 'Please check your network settings or refresh the page and try again.',
    noMatch: 'No matching numbers found',
    empty: 'No numbers yet',
  },

  continents: {
    asia: 'Asia',
    europe: 'Europe',
    northAmerica: 'North America',
    southAmerica: 'South America',
    africa: 'Africa',
    oceania: 'Oceania',
    antarctica: 'Antarctica',
    other: 'Other',
  },

  cardStyles: {
    namecard: 'Card',
    dial: 'Dial',
    bar: 'Bar',
  },

  cardKinds: {
    physical: 'Physical SIM',
    adapter: 'eSIM adapter',
    embedded: 'Built-in eSIM',
    virtual: 'Virtual number',
  },

  status: {
    active: 'Active',
    expired: 'Expired',
    cancelled: 'Cancelled',
    short: {
      expired: 'Expired',
      cancelled: 'Closed',
    },
  },

  keepAlive: {
    topup: 'Top up balance',
    addon: 'Buy or enable an add-on',
    use: 'Regular use (incl. SMS)',
    balanceChange: 'Balance change (e.g. roaming SMS)',
    autoCharge: 'Auto-charge',
    plan: 'Buy a renewal plan',
  },

  recordKinds: {
    purchase: 'Initial purchase',
    renewal: 'Renewal',
    topup: 'Extra top-up',
    other: 'Other',
  },

  cycle: {
    every: 'Every day | Every {n} days',
    varies: 'Varies (depends on top-ups or add-ons)',
  },

  billingPeriod: {
    monthly: 'mo',
    quarterly: 'qtr',
    semi_annual: '6 mo',
    annual: 'yr',
    biennial: '2 yr',
    triennial: '3 yr',
    quinquennial: '5 yr',
  },

  days: {
    count: '{n} day | {n} days',
    overdue: '{n} day overdue | {n} days overdue',
    today: 'Today',
    unit: 'day | days',
  },

  validity: {
    cancelledOn: 'Cancelled on {date}',
    noRecord: 'No validity record',
    tooltip: 'This cycle {total}, {left} left · until {end}',
  },

  keepCost: {
    noMethod: 'No keep-alive method recorded',
    variesShort: 'Varies',
    varies: 'Variable cycle',
    freeShort: 'Free',
    free: 'Free keep-alive',
    freeEvery: 'Free every day | Free every {n} days',
    perYear: '{amount}/yr',
    perCycle: '{amount} / {days}',
  },

  balance: {
    none: 'No balance recorded',
    noUpdateDate: 'No update date recorded',
    updatedToday: 'Updated today',
    updatedDaysAgo: 'Updated {days} ago',
  },

  hold: {
    noDate: 'No purchase date recorded',
    since: 'Held since {date}',
  },

  card: {
    firstPurchase: 'Initial purchase {amount}',
    heldFor: 'Held for {days}',
    remaining: 'Remaining',
    keepPerYear: 'Yearly cost',
    validity: 'Validity',
    until: 'Until {date}',
    thisPeriod: 'This cycle {days}',
    rows: {
      expiry: 'Expires',
      purchase: 'Purchased',
      keep: 'Keep-alive',
    },
  },

  summary: {
    activeNumbers: 'Active numbers',
    cardUnit: 'card | cards',
    countries: 'Countries & regions',
    countryUnit: 'country | countries',
    totalValue: 'Total value',
    monthlyCost: 'Monthly cost',
    perMonth: '{currency}/mo',
    remainingValue: 'Remaining value',
    todayRates: 'Today\'s rates',
    rateBaseCurrency: 'Switch base currency',
    totalSpent: 'Total spent',
    spendPurchase: 'Initial purchase',
    spendRenewal: 'Renewal',
    spendOther: 'Other',
    upcomingWithin: 'Due within {days}',
    noUpcoming: 'No renewals due within {days}',
    nextRenewal: 'Next renewal',
    overdueUnit: 'day overdue | days overdue',
    inDaysUnit: 'day left | days left',
  },

  globe: {
    activeCards: '{n} card | {n} cards',
    days: '{n}d | {n}d',
    mapLoadFailed: 'Failed to load map resources',
  },

  detail: {
    notFound: 'Number does not exist or has been deleted',
    backHome: 'Back to home',
    totalSpent: 'Total spent {amount}',
    free: 'Free',
    notApplicable: 'N/A',
    perMonth: '{currency} / mo',
    locked: 'Sign in to view',
    metrics: {
      price: 'Renewal price',
      monthly: 'Monthly cost',
      remaining: 'Time remaining',
      remainingValue: 'Remaining value',
    },
    sections: {
      number: 'Number details',
      keepAlive: 'Keep-alive details',
      balance: 'Balance details',
      account: 'Account details',
    },
    fields: {
      number: 'Number',
      country: 'Country / region',
      type: 'Card type',
      device: 'Device',
      keepAlive: 'Keep-alive method',
      cycle: 'Renewal cycle',
      nextRenewal: 'Next renewal',
      expiresAt: 'Valid until',
      balance: 'Current balance',
      balanceUpdatedAt: 'Balance updated',
      held: 'Held for',
      account: 'Account login',
      services: 'Linked services',
    },
  },

  history: {
    title: 'Renewal history',
    count: '{n} record | {n} records',
    empty: 'No expense records yet',
    free: 'Free',
    extendsTo: 'Extended to {date}',
    legend: {
      purchase: 'Initial purchase',
      renewal: 'Renewal / keep-alive',
      topup: 'Extra top-up',
      other: 'Other',
    },
    columns: {
      date: 'Date',
      type: 'Type',
      amount: 'Amount',
      amountCNY: 'CNY equivalent',
      extendsTo: 'Extended to',
    },
  },

  visitor: {
    anonymous: 'Web visitor',
    unknownLocation: 'Unknown location',
    ipUnavailable: 'IP unavailable',
    networkUnavailable: 'Network info unavailable',
    unknownIsp: 'Unknown ISP',
    unknownBrowser: 'Unknown browser',
    devices: {
      desktop: 'Desktop',
      android: 'Android phone',
      iphone: 'iPhone',
      ipad: 'iPad',
      tablet: 'Tablet',
    },
  },

  errors: {
    http: {
      requestFailed: 'Request failed ({status})',
    },
  },
}

export default en
