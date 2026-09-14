import moment from 'moment'
const imageModules = import.meta.glob('@/assets/*.{png,jpg,jpeg,svg,mp4,gif}', {
  eager: true,
})
import router from '@/router'

moment.locale('zh-cn', {
  months: '一月_二月_三月_四月_五月_六月_七月_八月_九月_十月_十一月_十二月'.split('_'),
  monthsShort: '1月_2月_3月_4月_5月_6月_7月_8月_9月_10月_11月_12月'.split('_'),
  weekdays: '星期日_星期一_星期二_星期三_星期四_星期五_星期六'.split('_'),
  longDateFormat: {
    LT: 'HH:mm',
    LTS: 'HH:mm:ss',
    L: 'YYYY-MM-DD',
    LL: 'YYYY年MM月DD日',
    LLL: 'YYYY年MM月DD日Ah点mm分',
    LLLL: 'YYYY年MM月DD日ddddAh点mm分',
    l: 'YYYY-M-D',
    ll: 'YYYY年M月D日',
    lll: 'YYYY年M月D日 HH:mm',
    llll: 'YYYY年M月D日dddd HH:mm',
  },
})
export default {
  convert2Amount: (amount) => {
    if (amount) {
      return amount.toFixed(2)
    } else {
      return '0.00'
    }
  },
  format2Html: (content) => {
    if (!content) {
      return content
    }
    content = content.replaceAll('\n', '<br>')
    content = content.replaceAll(' ', '&nbsp;')
    return content
  },
  formatTimeFriendly: (timestamp) => {
    const timestampTime = moment(timestamp)
    const days =
      Number.parseInt(moment().format('YYYYMMDD')) - Number.parseInt(timestampTime.format('YYYYMMDD'))
    if (days == 0) {
      return timestampTime.format('HH:mm')
    } else if (days == 1) {
      return '昨天'
    } else if (days >= 2 && days < 7) {
      //大于1天小于7天显示星期几
      return timestampTime.format('dddd')
    } else if (days >= 7) {
      //显示年月日
      return timestampTime.format('YYYY-MM-DD')
    }
  },
  format2Date: (timestamp) => {
    const timestampTime = moment(timestamp)
    return timestampTime.format('YYYY-MM-DD')
  },
  days: (startDate, endDate) => {
    const days = moment(endDate).diff(moment(startDate), 'days')
    return days
  },
  getLocalResource: (resource) => {
    const path = Object.keys(imageModules).find((path) => path.includes(resource))
    return path ? imageModules[path].default : ''
  },
  jump: (path, self = true) => {
    if (self) {
      router.push(path)
    } else {
      window.open(path, '_blank')
    }
  },
}
