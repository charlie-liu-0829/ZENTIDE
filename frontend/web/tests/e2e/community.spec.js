import { expect, test } from '@playwright/test'

const hub = {
  hubId: 1,
  name: '现场音乐',
  slug: 'live-music',
  description: '演唱会、音乐节和真实现场体验',
  memberCount: 128,
  postCount: 1,
  joined: true,
  owned: false,
  membershipStatus: 'ACTIVE',
}

const post = {
  postId: 101,
  hubId: 1,
  hubName: '现场音乐',
  authorId: '1000000001',
  authorLabel: '海边听众',
  postType: 'EXPERIENCE',
  postTypeLabel: '经验',
  title: '第一次去音乐节，需要准备什么？',
  body: '<p>整理了一份真实的现场清单。</p>',
  likeCount: 18,
  commentCount: 4,
  createdAt: '2026-08-28T12:00:00',
}

async function mockCommunityApi(page, { loggedIn = false } = {}) {
  const published = []
  await page.route('**/api/**', async (route) => {
    const url = new URL(route.request().url())
    let data = []
    if (url.pathname.endsWith('/account/autoLogin')) {
      data = loggedIn ? { userId: '1000000002', nickName: '知潮用户', token: 'e2e-token' } : null
    } else if (url.pathname.endsWith('/account/checkCode')) {
      data = { checkCode: 'data:image/png;base64,AA==', checkCodeKey: 'e2e-check' }
    } else if (url.pathname.endsWith('/community/hubs')) {
      data = [hub]
    } else if (url.pathname.endsWith('/community/feed')) {
      data = [post, ...published]
    } else if (url.pathname.endsWith('/community/post-types')) {
      data = [
        {
          postTypeId: 1,
          typeCode: 'DISCUSSION',
          displayName: '讨论',
          description: '交换观点',
          systemFixed: true,
        },
        {
          postTypeId: 2,
          typeCode: 'EXPERIENCE',
          displayName: '经验',
          description: '分享经历',
          systemFixed: true,
        },
      ]
    } else if (url.pathname.endsWith('/community/posts')) {
      const created = { ...post, postId: 102, title: 'E2E 发布的帖子' }
      published.unshift(created)
      data = created
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, info: 'ok', data }),
    })
  })
}

test('访客可以浏览社区内容，参与操作会进入登录流程', async ({ page }) => {
  await mockCommunityApi(page)
  await page.goto('/community')

  await expect(page.getByRole('heading', { name: '社区首页' })).toBeVisible()
  await expect(page.getByText('第一次去音乐节，需要准备什么？')).toBeVisible()
  await page.locator('.composer-trigger').click()
  await expect(page.getByText('欢迎回来')).toBeVisible()
})

test('登录用户可以打开富文本发布器并提交帖子', async ({ page }) => {
  await mockCommunityApi(page, { loggedIn: true })
  await page.addInitScript(() => localStorage.setItem('token', 'e2e-token'))
  await page.goto('/community')

  await page.locator('.composer-trigger').click()
  await expect(page.getByRole('dialog', { name: '发布分享' })).toBeVisible()
  await page.getByPlaceholder('用一句话概括').fill('E2E 发布的帖子')
  await page.getByLabel('帖子正文').fill('这是通过自动化测试发布的社区正文。')
  await page.getByRole('dialog', { name: '发布分享' }).getByRole('button', { name: '发布' }).click()
  await expect(page.getByRole('dialog', { name: '发布分享' })).toBeHidden()
})

test('登录成员可以在独立页面向社区小助手提问并打开引用帖子', async ({ page }) => {
  await page.route('**/api/**', async (route) => {
    const url = new URL(route.request().url())
    let data = []
    if (url.pathname.endsWith('/account/autoLogin')) {
      data = { userId: '1000000002', nickName: '知潮用户', token: 'e2e-token' }
    } else if (url.pathname.endsWith('/community/hubs')) {
      data = [hub]
    } else if (url.pathname.endsWith('/agent/chat')) {
      data = {
        conversation_id: 'e2econversation',
        answer: '可以先读音乐节准备清单。',
        blocks: [
          { type: 'text', text: '可以先读' },
          { type: 'reference', entity_type: 'POST', entity_id: 101, label: '音乐节准备清单' },
          { type: 'text', text: '。' },
        ],
      }
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, info: 'ok', data }),
    })
  })
  await page.addInitScript(() => localStorage.setItem('token', 'e2e-token'))
  await page.goto('/community/assistant/1')

  await expect(page.getByRole('heading', { name: '现场音乐' })).toBeVisible()
  await page.getByRole('button', { name: '帮我找几篇值得继续阅读的帖子' }).click()
  const reference = page.getByRole('link', { name: '音乐节准备清单' })
  await expect(reference).toBeVisible()
  await reference.click()
  await expect(page).toHaveURL(/\/community\/posts\/101$/)
})

test('兴趣现场创建者可以选择活动开始和结束时间', async ({ page }) => {
  let savedRequest = ''
  await page.route('**/api/**', async (route) => {
    const url = new URL(route.request().url())
    let data = []
    if (url.pathname.endsWith('/account/autoLogin')) {
      data = { userId: '1000000002', nickName: '知潮用户', token: 'e2e-token' }
    } else if (url.pathname.endsWith('/community/hubs')) {
      data = [{ ...hub, ownerId: '1000000002', owned: true, memberRole: 'OWNER' }]
    } else if (url.pathname.endsWith('/community/feed')) {
      data = [post]
    } else if (url.pathname.endsWith('/community/post-types')) {
      data = []
    } else if (url.pathname.endsWith('/community/events')) {
      data = []
    } else if (url.pathname.endsWith('/hubs/1/events/managed')) {
      data = []
    } else if (url.pathname.endsWith('/hubs/1/events')) {
      savedRequest = route.request().postData() || ''
      data = { eventId: 8, title: '秋日音乐会', startsAt: '2026-09-10T19:30:00' }
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, info: 'ok', data }),
    })
  })
  await page.addInitScript(() => localStorage.setItem('token', 'e2e-token'))
  await page.goto('/community?hubId=1')

  await page.getByRole('button', { name: '管理现场' }).click()
  await page.getByRole('button', { name: '管理近期活动' }).click()
  await page.getByRole('button', { name: '＋ 新建活动' }).click()
  await page.getByLabel('活动标题').fill('秋日音乐会')
  await page.getByRole('textbox', { name: '开始时间' }).fill('2026-09-10T19:30')
  await page.getByRole('textbox', { name: '结束时间' }).fill('2026-09-10T21:30')
  await expect(page.getByRole('textbox', { name: '开始时间' })).toHaveValue('2026-09-10T19:30')
  await expect(page.getByRole('textbox', { name: '结束时间' })).toHaveValue('2026-09-10T21:30')
  await page.getByRole('button', { name: '保存活动' }).click()
  expect(savedRequest).toContain('2026-09-10T19:30')
  expect(savedRequest).toContain('2026-09-10T21:30')
})

test('登录用户可以打开私信会话并发送消息', async ({ page }) => {
  const messages = [
    {
      messageId: 1,
      senderId: '1000000003',
      recipientId: '1000000002',
      body: '演出周末见',
      createdAt: '2026-09-03T09:00:00',
      readAt: null,
    },
  ]
  await page.route('**/api/**', async (route) => {
    const url = new URL(route.request().url())
    let data = []
    if (url.pathname.endsWith('/account/autoLogin')) {
      data = { userId: '1000000002', nickName: '知潮用户', token: 'e2e-token' }
    } else if (url.pathname.endsWith('/messages/unread-count')) {
      data = 1
    } else if (url.pathname.endsWith('/messages/conversations')) {
      data = [
        {
          peerId: '1000000003',
          peerName: '海边听众',
          body: messages.at(-1).body,
          unreadCount: 1,
          createdAt: messages.at(-1).createdAt,
        },
      ]
    } else if (url.pathname.endsWith('/messages/with/1000000003/send')) {
      const sent = {
        messageId: messages.length + 1,
        senderId: '1000000002',
        recipientId: '1000000003',
        body: '我会准时到',
        createdAt: '2026-09-03T09:05:00',
      }
      messages.push(sent)
      data = sent
    } else if (url.pathname.endsWith('/messages/with/1000000003')) {
      data = messages
    } else if (url.pathname.endsWith('/community/users/1000000003')) {
      data = { userId: '1000000003', nickName: '海边听众' }
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, info: 'ok', data }),
    })
  })
  await page.addInitScript(() => localStorage.setItem('token', 'e2e-token'))
  await page.goto('/messages/1000000003')

  await expect(page.getByRole('article').getByText('演出周末见')).toBeVisible()
  await page.getByPlaceholder('写一条私信…').fill('我会准时到')
  await page.getByRole('button', { name: '发送', exact: true }).click()
  await expect(page.getByRole('article').getByText('我会准时到')).toBeVisible()
})
