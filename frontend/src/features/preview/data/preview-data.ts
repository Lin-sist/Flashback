import type {
  PageQuery,
  PaginationResponse,
  RecordDetailVO,
  RecordListItemVO,
  ReplyVO,
  TagVO,
  TimelineGroupVO,
  TimelinePageVO,
  TimelineQuery,
  UserInfoVO,
} from '../../../types'
import { RecordReminderStatus, RecordStatus, RecordType, ReplyType, TagType } from '../../../types'

interface PreviewRecordSeed {
  id: number
  title: string
  content: string
  contentPreview: string
  recordType: RecordType
  status: RecordStatus
  unlockReminderStatus?: RecordReminderStatus
  createdAt: string
  updatedAt: string
  unlockAt?: string
  sealedAt?: string
  unlockedAt?: string
  tagIds: number[]
  canReply: boolean
  hasReply: boolean
  aiSummary?: string
  aiPromptResults?: string[]
}

const previewTags: TagVO[] = [
  { id: 1, name: '夜色', type: TagType.MOOD },
  { id: 2, name: '回望', type: TagType.TOPIC },
  { id: 3, name: '安静', type: TagType.MOOD },
  { id: 4, name: '春信', type: TagType.TOPIC },
]

const previewUser: UserInfoVO = {
  id: 'preview-user',
  username: 'preview_guest',
  nickname: '来访者',
  email: 'preview@flashback.local',
}

const previewRecords: PreviewRecordSeed[] = [
  {
    id: 101,
    title: '凌晨四点，窗沿还留着一线白',
    content:
      '那晚没有发生什么大事，只是终于肯把心里反复绕行的句子，慢慢写了下来。路灯很安静，风从窗缝里进来，像替未来的人轻轻按住了纸页。\n\n如果你后来看到这一段，希望你记得，当时的我虽然犹豫，却已经开始愿意相信，生活会在缓慢处转向。',
    contentPreview: '那晚没有发生什么大事，只是终于肯把心里反复绕行的句子，慢慢写了下来。',
    recordType: RecordType.FUTURE_LETTER,
    status: RecordStatus.DRAFT,
    createdAt: '2026-04-18 23:40:00',
    updatedAt: '2026-04-20 00:15:00',
    unlockAt: '2026-10-01 09:00:00',
    tagIds: [1, 2],
    canReply: false,
    hasReply: false,
    aiSummary: '一封还在写作途中的未来来信，语气克制而温柔。',
    aiPromptResults: ['你想留给未来的，不是答案，而是一种仍愿意继续生活的证据。'],
  },
  {
    id: 201,
    title: '写给初秋的一页封存',
    content:
      '如果秋天来得比想象中早，也没关系。你已经认真度过了这段日子，把那些来不及解释的疲惫、迟疑与一点点重新振作，都妥帖地封存在这里。\n\n等到合适的时候，再把它打开。',
    contentPreview: '你已经认真度过了这段日子，把那些来不及解释的疲惫与迟疑，都妥帖地封存在这里。',
    recordType: RecordType.NODE_RECORD,
    status: RecordStatus.SEALED,
    unlockReminderStatus: RecordReminderStatus.AUTHORIZED,
    createdAt: '2026-03-08 21:12:00',
    updatedAt: '2026-03-08 21:20:00',
    sealedAt: '2026-03-08 21:20:00',
    unlockAt: '2026-09-12 20:00:00',
    tagIds: [2, 3],
    canReply: false,
    hasReply: false,
  },
  {
    id: 202,
    title: '留给冬天的一盏灯',
    content:
      '总会有一些难以立刻分辨的夜晚。写下这段话的时候，我只是想替那时的自己留一盏灯，让以后的人知道，黑暗并不意味着没有出口。',
    contentPreview: '写下这段话的时候，我只是想替那时的自己留一盏灯。',
    recordType: RecordType.EMOTION_NOTE,
    status: RecordStatus.SEALED,
    unlockReminderStatus: RecordReminderStatus.REQUESTED,
    createdAt: '2025-12-14 19:36:00',
    updatedAt: '2025-12-14 19:50:00',
    sealedAt: '2025-12-14 19:50:00',
    unlockAt: '2026-12-14 20:00:00',
    tagIds: [1, 3],
    canReply: false,
    hasReply: false,
  },
  {
    id: 301,
    title: '给春天的回信',
    content:
      '原来许多以为过不去的时刻，真的会在某一天被温柔地翻过去。现在再读当时写下的话，没有那么疼了，只剩一点轻微的心酸，和更多想要安慰当时自己的冲动。\n\n谢谢你没有轻易放弃。',
    contentPreview: '原来许多以为过不去的时刻，真的会在某一天被温柔地翻过去。',
    recordType: RecordType.FUTURE_LETTER,
    status: RecordStatus.UNLOCKED,
    unlockReminderStatus: RecordReminderStatus.SEND_SUCCESS,
    createdAt: '2025-04-03 22:10:00',
    updatedAt: '2026-04-03 09:12:00',
    sealedAt: '2025-04-03 22:18:00',
    unlockAt: '2026-04-03 09:00:00',
    unlockedAt: '2026-04-03 09:12:00',
    tagIds: [3, 4],
    canReply: false,
    hasReply: true,
  },
  {
    id: 302,
    title: '雨停之后的站台',
    content:
      '那天雨停得很突然，像一句没说完的话被轻轻收了尾。站台上的风有些凉，但人群慢慢散去的时候，我第一次觉得，原来等待本身也会留下痕迹。\n\n如果你后来依旧常常迟疑，希望你至少记得，那天你已经学会了不再急着责怪自己。',
    contentPreview: '站台上的风有些凉，但人群慢慢散去的时候，我第一次觉得，原来等待本身也会留下痕迹。',
    recordType: RecordType.NODE_RECORD,
    status: RecordStatus.UNLOCKED,
    unlockReminderStatus: RecordReminderStatus.NOT_CONFIGURED,
    createdAt: '2025-11-21 18:24:00',
    updatedAt: '2026-04-16 08:02:00',
    sealedAt: '2025-11-21 18:40:00',
    unlockAt: '2026-04-16 08:00:00',
    unlockedAt: '2026-04-16 08:02:00',
    tagIds: [2, 4],
    canReply: true,
    hasReply: false,
  },
]

const previewReplies: Record<number, ReplyVO> = {
  301: {
    id: 1,
    recordId: 301,
    content: '现在的我想告诉你，后来真的慢慢好起来了。',
    replyType: ReplyType.SHORT_REPLY,
    createdAt: '2026-04-03 09:20:00',
  },
}

const deepCopy = <T>(value: T): T => JSON.parse(JSON.stringify(value)) as T

const sortByNewest = <T extends { createdAt: string; id?: number }>(list: T[]) =>
  [...list].sort((left, right) => {
    const createdAtDiff = new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()
    if (createdAtDiff !== 0) return createdAtDiff
    return (right.id || 0) - (left.id || 0)
  })

const resolveTags = (tagIds: number[]) =>
  previewTags.filter((tag) => tagIds.includes(tag.id))

const toListItem = (record: PreviewRecordSeed): RecordListItemVO => ({
  id: record.id,
  title: record.title,
  contentPreview: record.contentPreview,
  recordType: record.recordType,
  status: record.status,
  unlockAt: record.unlockAt,
  createdAt: record.createdAt,
  tagNames: resolveTags(record.tagIds).map((tag) => tag.name),
})

const toDetail = (record: PreviewRecordSeed): RecordDetailVO => ({
  id: record.id,
  title: record.title,
  content: record.content,
  recordType: record.recordType,
  coreQuestion: '',
  status: record.status,
  unlockAt: record.unlockAt,
  sealedAt: record.sealedAt,
  unlockedAt: record.unlockedAt,
  aiSummary: record.aiSummary,
  aiPromptResults: record.aiPromptResults || [],
  tags: resolveTags(record.tagIds),
  canReply: record.canReply,
  hasReply: record.hasReply,
  createdAt: record.createdAt,
  updatedAt: record.updatedAt,
})

const paginate = <T>(list: T[], query: PageQuery): PaginationResponse<T> => {
  const safePageNum = Math.max(1, query.pageNum || 1)
  const safePageSize = Math.max(1, query.pageSize || 10)
  const start = (safePageNum - 1) * safePageSize

  return {
    list: list.slice(start, start + safePageSize),
    total: list.length,
    pageNum: safePageNum,
    pageSize: safePageSize,
  }
}

const getSortedPreviewRecords = () => sortByNewest(previewRecords)

export const getPreviewUserInfo = () => deepCopy(previewUser)

export const getPreviewTags = () => deepCopy(previewTags)

export const getPreviewRecordList = (status: RecordStatus | 'ALL', query: PageQuery) => {
  const filtered = getSortedPreviewRecords().filter((record) => status === 'ALL' || record.status === status)
  return paginate(filtered.map(toListItem), query)
}

export const getPreviewUnlockedRecords = (pageNum = 1, pageSize = 10) =>
  paginate(
    getSortedPreviewRecords()
      .filter((record) => record.status === RecordStatus.UNLOCKED)
      .map(toListItem),
    { pageNum, pageSize }
  )

const parsePreviewDate = (value: string) => {
  const [datePart] = value.split(/[ T]/)
  const [year, month, day] = datePart.split('-').map(Number)
  return { year, month, day }
}

export const getPreviewTimeline = (query: TimelineQuery = {}): TimelinePageVO => {
  const filtered = getSortedPreviewRecords().filter((record) => {
    if (query.tagId && !record.tagIds.includes(query.tagId)) return false
    const createdDate = parsePreviewDate(record.createdAt)
    if (query.year && createdDate.year !== query.year) return false
    if (query.month && createdDate.month !== query.month) return false
    if (query.day && createdDate.day !== query.day) return false
    return true
  })

  const pageNum = Math.max(1, query.pageNum || 1)
  const pageSize = Math.min(50, Math.max(1, query.pageSize || 20))
  const offset = (pageNum - 1) * pageSize
  const pageRecords = filtered.slice(offset, offset + pageSize)

  const grouped = pageRecords.reduce<Record<string, TimelineGroupVO>>((acc, record) => {
    const date = new Date(record.createdAt.replace(' ', 'T'))
    const yearMonth = `${date.getFullYear()}年${String(date.getMonth() + 1).padStart(2, '0')}月`

    if (!acc[yearMonth]) {
      acc[yearMonth] = {
        yearMonth,
        items: [],
      }
    }

    acc[yearMonth].items.push({
      id: record.id,
      title: record.title,
      status: record.status,
      recordType: record.recordType,
      createdAt: record.createdAt,
      tagNames: resolveTags(record.tagIds).map((tag) => tag.name),
    })

    return acc
  }, {})

  return {
    groups: Object.values(grouped),
    total: filtered.length,
    pageNum,
    pageSize,
    hasMore: offset + pageSize < filtered.length,
  }
}

export const getPreviewRecordDetail = (id: string | number) => {
  const record = previewRecords.find((item) => Number(item.id) === Number(id))
  return record ? toDetail(record) : null
}

export const getPreviewReply = (recordId: number) => {
  const reply = previewReplies[recordId]
  return reply ? deepCopy(reply) : null
}
