<template>
  <main class="community-shell">
    <CommunityLeftRail
      :logged-in="loggedIn()"
      :grouped-hubs="groupedHubs"
      :hubs="hubs"
      :selected-hub="selectedHub"
      :resource-url="resourceUrl"
      @select-hub="selectHub"
      @manage-categories="openCategoryManager"
      @create-hub="showHubComposer"
    />

    <section class="feed-column">
      <header class="feed-header" :class="{ 'with-cover': activeHub?.coverUrl }">
        <img
          v-if="activeHub?.coverUrl"
          class="feed-header-cover"
          :src="resourceUrl(activeHub.coverUrl)"
          alt=""
        />
        <div class="feed-header-copy">
          <span class="feed-kicker">{{ activeHub ? '兴趣现场' : '知潮社区' }}</span>
          <h1>{{ selectedHubName || '社区首页' }}</h1>
          <p>
            {{
              activeHub?.description ||
              (loggedIn()
                ? '这里汇总你已经加入或创建的兴趣现场。'
                : '登录后加入兴趣现场，建立属于自己的社区首页。')
            }}
          </p>
        </div>
        <div v-if="activeHub" class="hub-header-actions">
          <button v-if="canManageHub" type="button" class="manage-hub-button" @click="openHubManager">
            管理现场</button
          ><button
            v-else
            type="button"
            class="join-button"
            :class="{ joined: activeHub.joined, pending: activeHub.membershipStatus === 'PENDING' }"
            :disabled="activeHub.membershipStatus === 'PENDING'"
            @click="toggleHubMembership"
          >
            {{ joinButtonLabel(activeHub) }}
          </button>
        </div>
      </header>

      <div class="feed-controls">
        <span class="feed-control-label">浏览</span>
        <div class="filters">
          <button
            v-for="filter in filters"
            :key="filter.value"
            type="button"
            :class="{ active: selectedType === filter.value }"
            @click="selectType(filter.value)"
          >
            {{ filter.label }}
          </button>
        </div>
      </div>

      <button type="button" class="composer-trigger" @click="openComposer">
        <span class="mini-avatar">{{
          loggedIn() ? (loginStore.userInfo.nickName || '我').slice(0, 1) : '访'
        }}</span>
        <span>分享一个发现、问题或真实体验</span>
        <b>发布</b>
      </button>

      <div v-if="posts.length" class="post-list">
        <article v-for="post in posts" :key="post.postId" class="post-card" @click="openPost(post.postId)">
          <div class="vote-rail">
            <button
              type="button"
              :class="{ active: post.liked }"
              aria-label="赞同"
              @click.stop="toggleLike(post)"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 14 6-6 6 6" /></svg>
            </button>
            <strong>{{ post.likeCount || 0 }}</strong>
            <span class="vote-label">赞同</span>
          </div>
          <div class="post-content">
            <header class="post-meta">
              <router-link class="community-name" to="/community" @click.stop>{{ post.hubName }}</router-link>
              <span>由</span>
              <router-link :to="{ name: 'people', params: { userId: post.authorId } }" @click.stop
                >@{{ post.authorLabel || '社区成员' }}</router-link
              >
              <span>发布 · {{ formatTime(post.createdAt) }}</span>
              <el-dropdown
                v-if="canManage(post)"
                trigger="click"
                @command="(command) => managePost(post, command)"
              >
                <button type="button" class="post-owner-menu" aria-label="管理帖子" @click.stop>•••</button>
                <template #dropdown
                  ><el-dropdown-menu
                    ><el-dropdown-item command="edit">编辑帖子</el-dropdown-item
                    ><el-dropdown-item command="delete" divided>删除帖子</el-dropdown-item></el-dropdown-menu
                  ></template
                >
              </el-dropdown>
              <em>{{ typeLabel(post.postType, post.postTypeLabel) }}</em>
            </header>

            <h2 v-if="post.title" class="open-post">{{ post.title }}</h2>
            <button
              v-if="feedCover(post)"
              type="button"
              class="post-cover"
              @click.stop="openPost(post.postId)"
            >
              <img :src="resourceUrl(feedCover(post))" alt="" />
            </button>
            <p class="post-body open-post">{{ postExcerpt(post.body) }}</p>

            <div v-if="post.changeId" class="verified-context">
              <span
                ><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m7 12.5 3.1 3.1L17.5 8" /></svg
                >已确认变化</span
              >
              <button type="button" @click.stop="openChange(post.changeId)">
                {{ post.changeTitle || '查看变化和原始证据' }} →
              </button>
            </div>
            <div class="context-tags">
              <span v-if="post.topicNames"># {{ post.topicNames }}</span>
              <span v-if="post.eventTitle">活动 · {{ post.eventTitle }}</span>
            </div>

            <footer class="post-actions" @click.stop>
              <button type="button" @click="openComments(post)">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path
                    d="M20 15a3 3 0 0 1-3 3H9l-5 3v-6a3 3 0 0 1-1-2.2V7a3 3 0 0 1 3-3h11a3 3 0 0 1 3 3Z"
                  /></svg
                >{{ post.commentCount || 0 }} 条评论
              </button>
              <span class="post-view-count">浏览 {{ post.viewCount || 0 }}</span>
              <button type="button" :class="{ active: post.bookmarked }" @click="toggleBookmark(post)">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 4.5h12v16l-6-3.8-6 3.8Z" /></svg
                >{{ post.bookmarked ? '已收藏' : '收藏' }}
              </button>
              <el-dropdown trigger="click" @command="(action) => setAction(post, action)">
                <button type="button">{{ actionLabel(post.myAction) }} ···</button>
                <template #dropdown
                  ><el-dropdown-menu
                    ><el-dropdown-item
                      v-for="action in actions"
                      :key="action.value"
                      :command="action.value"
                      >{{ action.label }}</el-dropdown-item
                    ><el-dropdown-item command="" divided>清除状态</el-dropdown-item></el-dropdown-menu
                  ></template
                >
              </el-dropdown>
            </footer>

            <div v-if="post.expanded" class="comments" @click.stop>
              <div v-if="post.comments?.length" class="comment-list">
                <div v-for="comment in post.comments" :key="comment.commentId">
                  <b>@{{ comment.authorLabel }}</b
                  ><span>{{ comment.body }}</span>
                </div>
              </div>
              <p v-else>还没有评论，来留下第一句。</p>
              <div class="comment-compose">
                <el-input
                  v-model="post.commentDraft"
                  placeholder="写下你的评论…"
                  @keyup.enter="submitComment(post)"
                /><el-button type="primary" @click="submitComment(post)">发送</el-button>
              </div>
            </div>
          </div>
        </article>
      </div>

      <div v-else class="empty-state">
        <div class="empty-mark"><span></span><span></span><span></span></div>
        <b>这里还没有动态</b>
        <p>成为第一个开启讨论的人。</p>
        <el-button type="primary" @click="openComposer">发布第一条</el-button>
      </div>
    </section>

    <CommunityRightRail
      :logged-in="loggedIn()"
      :user-name="loginStore.userInfo.nickName || ''"
      :active-hub="activeHub"
      :can-manage-hub="canManageHub"
      :can-manage-events="hasHubPermission('MANAGE_EVENTS')"
      :can-use-assistant="Boolean(loggedIn() && activeHub?.joined)"
      :topics="topics"
      :events="events"
      :attendance-statuses="attendanceStatuses"
      :event-day="eventDay"
      :event-month="eventMonth"
      :attendance-label="attendanceLabel"
      @open-composer="openComposer"
      @open-assistant="openAssistant"
      @choose-topic="chooseTopic"
      @set-attendance="setAttendance"
      @manage-events="openEventManager"
      @open-event-stats="showEventStats"
    />

    <el-dialog
      v-model="eventManagerVisible"
      class="event-manager-dialog"
      title="管理近期活动"
      width="min(94vw, 720px)"
    >
      <div class="event-manager-toolbar">
        <p>活动由兴趣现场创建者维护，成员可以在活动卡片中标记参与状态。</p>
        <el-button type="primary" @click="startCreateEvent">＋ 新建活动</el-button>
      </div>
      <div v-if="managedEvents.length" class="managed-event-list">
        <article v-for="event in managedEvents" :key="event.eventId">
          <div>
            <b>{{ event.title }}</b
            ><small>{{ formatFullTime(event.startsAt) }} · {{ event.venue || '地点待确认' }}</small>
          </div>
          <div>
            <el-button link @click="editEvent(event)">编辑</el-button
            ><el-button link class="event-stats-button" @click="showEventStats(event)">参与度</el-button
            ><el-button link type="danger" @click="removeEvent(event)">删除</el-button>
          </div>
        </article>
      </div>
      <div v-else class="manager-empty">还没有活动，创建第一场活动吧。</div>
      <el-form v-if="eventEditing" label-position="top" class="event-form">
        <el-form-item label="活动标题"><el-input v-model="eventDraft.title" maxlength="220" /></el-form-item>
        <el-form-item label="活动说明"
          ><el-input v-model="eventDraft.description" type="textarea" :rows="3" maxlength="2000"
        /></el-form-item>
        <div class="event-form-grid">
          <el-form-item label="开始时间">
            <input
              v-model="eventDraft.startsAt"
              class="event-datetime-input"
              type="datetime-local"
              step="60"
              aria-label="开始时间"
            />
          </el-form-item>
          <el-form-item label="结束时间">
            <input
              v-model="eventDraft.endsAt"
              class="event-datetime-input"
              type="datetime-local"
              step="60"
              :min="eventDraft.startsAt || undefined"
              aria-label="结束时间"
            />
          </el-form-item>
        </div>
        <el-form-item label="地点"><el-input v-model="eventDraft.venue" maxlength="220" /></el-form-item>
        <div class="event-form-actions">
          <el-button @click="eventEditing = false">取消</el-button
          ><el-button type="primary" :loading="savingEvent" @click="saveEvent">保存活动</el-button>
        </div>
      </el-form>
    </el-dialog>

    <el-dialog
      v-model="eventStatsVisible"
      class="event-stats-dialog"
      title="活动参与度"
      width="min(92vw, 460px)"
      ><div v-if="eventStats" class="event-stats">
        <div class="pie-chart" :style="pieStyle"></div>
        <div class="pie-legend">
          <span><i class="want"></i>想参加 {{ eventStats.wantCount || 0 }}</span
          ><span><i class="attending"></i>参加中 {{ eventStats.attendingCount || 0 }}</span
          ><span><i class="attended"></i>已参加 {{ eventStats.attendedCount || 0 }}</span>
        </div>
        <p>共 {{ eventStats.totalParticipants || 0 }} 人标记过参与状态</p>
      </div></el-dialog
    >

    <el-dialog
      v-model="composerVisible"
      class="post-composer-dialog"
      :title="composerTitle"
      width="min(96vw, 1080px)"
    >
      <div v-if="composer.changeId" class="composer-context">
        <span>基于已确认变化</span><b>{{ changeContext?.changeTitle || '这条变化' }}</b
        ><button type="button" @click="openChange(composer.changeId)">查看证据</button>
      </div>
      <div class="composer-review-layout">
        <el-form class="composer-form" label-position="top">
          <el-form-item label="兴趣现场"
            ><el-select
              v-model="composer.hubId"
              :disabled="isEditing"
              placeholder="选择兴趣现场"
              style="width: 100%"
              ><el-option
                v-for="hub in hubs"
                :key="hub.hubId"
                :label="hub.name"
                :value="hub.hubId" /></el-select
          ></el-form-item>
          <el-form-item label="内容类型">
            <div class="post-type-picker">
              <el-radio-group v-model="composer.type"
                ><el-radio-button
                  v-for="type in composerPostTypes"
                  :key="type.typeCode"
                  :value="type.typeCode"
                  >{{ type.displayName }}</el-radio-button
                ></el-radio-group
              ><small v-if="selectedComposerType?.description">{{ selectedComposerType.description }}</small>
            </div>
          </el-form-item>
          <el-form-item label="关联话题（可选）">
            <div class="topic-picker">
              <el-select v-model="composer.topicId" clearable placeholder="选择已有话题" style="width: 100%"
                ><el-option
                  v-for="topic in composerTopics"
                  :key="topic.topicId"
                  :label="`# ${topic.canonicalName}`"
                  :value="topic.topicId"
              /></el-select>
              <button type="button" class="topic-create-toggle" @click="creatingTopic = !creatingTopic">
                {{ creatingTopic ? '取消新建' : '＋ 自定义话题' }}
              </button>
              <div v-if="creatingTopic" class="topic-create-row">
                <el-input
                  v-model="newTopicName"
                  maxlength="40"
                  placeholder="输入话题名称，例如：巡演现场"
                  @keyup.enter="createTopic"
                /><el-button :loading="savingTopic" @click="createTopic">创建并关联</el-button>
              </div>
            </div>
          </el-form-item>
          <el-form-item v-if="composerEvents.length" label="关联活动（可选）"
            ><el-select v-model="composer.eventId" clearable placeholder="选择活动" style="width: 100%"
              ><el-option
                v-for="event in composerEvents"
                :key="event.eventId"
                :label="event.title"
                :value="event.eventId" /></el-select
          ></el-form-item>
          <el-form-item class="composer-title-field" label="标题" required
            ><el-input v-model="composer.title" maxlength="220" placeholder="用一句话概括"
          /></el-form-item>
          <el-form-item label="正文"
            ><RichTextEditor
              v-model="composer.body"
              :max-length="8000"
              placeholder="说说你的发现、体验或问题…"
          /></el-form-item>
          <el-form-item label="封面图片（可选）">
            <div class="cover-uploader">
              <img v-if="composer.coverUrl" :src="resourceUrl(composer.coverUrl)" alt="封面预览" />
              <el-upload :show-file-list="false" accept="image/*" :http-request="uploadCover"
                ><button type="button" :disabled="uploadingCover">
                  {{ uploadingCover ? '上传中…' : composer.coverUrl ? '更换封面' : '上传自定义封面' }}
                </button></el-upload
              >
              <button
                v-if="composer.coverUrl"
                type="button"
                class="remove-cover"
                @click="composer.coverUrl = ''"
              >
                移除
              </button>
            </div>
          </el-form-item>
        </el-form>
        <aside class="smart-review-panel">
          <div class="review-heading">
            <div>
              <b><span class="review-heading-mark">✦</span> 智能检查</b
              ><small>{{
                reviewResult?.analysis_mode === 'llm' ? '语义分析已完成' : '发布前质量副驾'
              }}</small>
            </div>
            <el-button :loading="reviewing" @click="runSmartReview">智能检查</el-button>
          </div>
          <p class="review-intro">帮你检查重复风险、内容完整度和表达质量，所有修改都由你确认。</p>
          <div v-if="localReviewHints.length" class="local-review-hints">
            <span v-for="hint in localReviewHints" :key="hint">△ {{ hint }}</span>
          </div>
          <div v-if="reviewResult" class="review-result" :class="{ stale: !reviewIsCurrent }">
            <p v-if="!reviewIsCurrent" class="review-stale review-status">内容已变化，请重新检查</p>
            <p v-else-if="reviewResult.publish_blocked" class="review-danger review-status">
              ! 禁止发布：检测到以下违规风险
            </p>
            <ul v-if="reviewIsCurrent && reviewResult.publish_blocked && reviewResult.blocking_reasons?.length" class="review-blocking-reasons">
              <li v-for="reason in reviewResult.blocking_reasons" :key="reason">{{ reason }}</li>
            </ul>
            <p v-else-if="reviewResult.analysis_mode === 'rules'" class="review-warn review-status">
              模型暂不可用，当前为基础规则检查
            </p>
            <p v-else-if="reviewResult.advice === 'ready'" class="review-ok review-status">✓ 可以发布</p>
            <p v-else-if="reviewResult.advice === 'manual_review'" class="review-danger review-status">
              ! 需要人工审核
            </p>
            <p v-else class="review-warn review-status">△ 建议发布前确认</p>

            <section v-if="reviewResult.sensitive_information_warnings?.length">
              <b>敏感信息</b>
              <span v-for="item in reviewResult.sensitive_information_warnings" :key="item">{{ item }}</span>
            </section>
            <p v-else class="review-ok">✓ 未发现手机号、邮箱或密钥</p>
            <section v-if="reviewResult.missing_information?.length">
              <b>建议补充</b>
              <span v-for="item in reviewResult.missing_information" :key="item">{{ item }}</span>
            </section>
            <section v-if="reviewResult.community_rule_warnings?.length">
              <b>社区规则</b>
              <span v-for="item in reviewResult.community_rule_warnings" :key="item">{{ item }}</span>
            </section>
            <section v-if="reviewResult.content_improvements?.length">
              <b>内容优化建议</b>
              <span v-for="item in reviewResult.content_improvements" :key="item">{{ item }}</span>
            </section>
            <section
              v-if="reviewIsCurrent && reviewResult.optimized_content && !reviewResult.publish_blocked"
              class="content-optimization"
            >
              <b>智能优化稿已生成</b>
              <button type="button" class="accept-suggestion" @click="optimizedContentOfferVisible = true">
                查看并选择是否采用
              </button>
              <small>采用后会直接替换编辑器正文，并要求重新检查。</small>
            </section>
            <section v-if="unselectedSuggestedTopics.length">
              <b>话题建议</b>
              <button
                v-for="topic in unselectedSuggestedTopics"
                :key="topic.topicId"
                type="button"
                class="accept-suggestion"
                @click="acceptSuggestedTopic(topic.topicId)"
              >
                采用 # {{ topic.canonicalName }}
              </button>
            </section>
            <section v-if="reviewResult.similar_posts?.length">
              <b>相似帖子 · {{ Math.round(reviewResult.duplicate_probability * 100) }}%</b>
              <button
                v-for="post in reviewResult.similar_posts"
                :key="post.post_id"
                type="button"
                class="similar-post"
                @click="viewSimilarPost(post.post_id)"
              >
                <strong>{{ post.title }}</strong
                ><small>{{ post.reason }}</small>
              </button>
            </section>
            <p v-else class="review-ok">✓ 未发现高度相似帖子</p>
            <button
              v-if="
                reviewIsCurrent &&
                reviewResult.suggested_post_type &&
                reviewResult.suggested_post_type !== composer.type
              "
              type="button"
              class="accept-suggestion"
              @click="composer.type = reviewResult.suggested_post_type"
            >
              采用类型建议：{{ typeLabel(reviewResult.suggested_post_type) }}
            </button>
            <button
              v-if="
                reviewIsCurrent &&
                reviewResult.suggested_title &&
                reviewResult.suggested_title !== composer.title
              "
              type="button"
              class="accept-suggestion"
              @click="acceptSuggestedTitle"
            >
              采用标题建议
            </button>
          </div>
          <p v-else class="review-empty">检查结果会显示在这里；所有建议均由你决定是否采用。</p>
        </aside>
      </div>
      <template #footer
        ><el-button @click="saveComposerDraft">保存草稿</el-button
        ><el-button @click="composerVisible = false">取消</el-button
        ><el-button
          type="primary"
          :loading="publishing || reviewing"
          :disabled="reviewIsCurrent && reviewResult?.publish_blocked"
          @click="publishPost"
          >{{
            isEditing
              ? '保存修改'
              : reviewIsCurrent && reviewResult?.publish_blocked
                ? '禁止发布'
                : reviewIsCurrent && reviewResult?.advice === 'manual_review'
                  ? '提交人工审核'
                  : reviewIsCurrent && reviewResult?.advice !== 'ready'
                    ? '继续发布'
                    : '发布'
          }}</el-button
        ></template
      >
    </el-dialog>

    <el-dialog
      v-model="optimizedContentOfferVisible"
      append-to-body
      class="content-optimization-dialog"
      title="智能助手已优化正文"
      width="min(92vw, 680px)"
      :close-on-click-modal="false"
    >
      <p class="dialog-tip">是否采用下面的优化内容？选择采用后将直接替换正文。</p>
      <div v-if="reviewResult?.content_improvements?.length" class="optimization-summary">
        <b>本次优化</b>
        <span v-for="item in reviewResult.content_improvements" :key="item">• {{ item }}</span>
      </div>
      <div class="optimized-content-preview" v-html="safeOptimizedContent"></div>
      <template #footer>
        <el-button @click="optimizedContentOfferVisible = false">保留原文</el-button>
        <el-button type="primary" @click="acceptOptimizedContent">采用并替换正文</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="hubComposerVisible"
      class="hub-composer-dialog"
      title="创建兴趣现场"
      width="min(92vw, 520px)"
    >
      <p class="dialog-tip">兴趣现场适合长期聚合同好、讨论、活动和经验。</p>
      <el-form label-position="top">
        <el-form-item label="名称"
          ><el-input v-model="hubDraft.name" maxlength="40" placeholder="例如：独立电影放映、城市骑行"
        /></el-form-item>
        <el-form-item label="介绍"
          ><el-input
            v-model="hubDraft.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            placeholder="大家会在这里讨论什么？"
        /></el-form-item>
        <el-form-item label="现场封面（可选）">
          <div class="hub-cover-uploader">
            <div class="hub-cover-preview" :class="{ fallback: !hubDraft.coverUrl }">
              <img v-if="hubDraft.coverUrl" :src="resourceUrl(hubDraft.coverUrl)" alt="现场封面预览" /><span
                v-else
                >{{ hubDraft.name.trim().slice(0, 1) || '潮' }}</span
              >
            </div>
            <div>
              <el-upload :show-file-list="false" accept="image/*" :http-request="uploadHubCover"
                ><button type="button" :disabled="uploadingHubCover">
                  {{ uploadingHubCover ? '上传中…' : hubDraft.coverUrl ? '更换封面' : '上传封面' }}
                </button></el-upload
              ><button
                v-if="hubDraft.coverUrl"
                type="button"
                class="remove-hub-cover"
                @click="hubDraft.coverUrl = ''"
              >
                恢复默认</button
              ><small>未上传时会使用由现场名称生成的默认封面。</small>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="所属方向"
          ><el-select v-model="hubDraft.category" style="width: 100%"
            ><el-option
              v-for="direction in hubDirections"
              :key="direction.code"
              :label="direction.displayName"
              :value="direction.code" /></el-select
        ></el-form-item>
        <el-form-item label="加入方式">
          <el-radio-group v-model="hubDraft.joinPolicy" class="join-policy-options">
            <el-radio value="OPEN"
              ><span><b>开放加入</b><small>任何登录用户都可以直接加入</small></span></el-radio
            >
            <el-radio value="APPROVAL"
              ><span><b>需要审核</b><small>加入申请由创建者通过后生效</small></span></el-radio
            >
            <el-radio value="QUESTION"
              ><span><b>问答加入</b><small>回答正确后直接加入</small></span></el-radio
            >
          </el-radio-group>
        </el-form-item>
        <template v-if="hubDraft.joinPolicy === 'QUESTION'"
          ><el-form-item label="入场问题"
            ><el-input
              v-model="hubDraft.joinQuestion"
              maxlength="200"
              placeholder="例如：这个现场主要讨论什么？" /></el-form-item
          ><el-form-item label="标准答案"
            ><el-input
              v-model="hubDraft.joinAnswer"
              maxlength="200"
              placeholder="设置一个明确答案" /></el-form-item
        ></template>
      </el-form>
      <template #footer
        ><el-button @click="hubComposerVisible = false">取消</el-button
        ><el-button type="primary" :loading="creatingHub" @click="createHub">创建并加入</el-button></template
      >
    </el-dialog>

    <el-dialog
      v-model="hubManagerVisible"
      class="hub-manager-dialog"
      :title="activeHub ? `管理「${activeHub.name}」` : '管理兴趣现场'"
      width="min(94vw, 640px)"
    >
      <div v-if="activeHub" class="hub-manager">
        <section v-if="hasHubPermission('REVIEW_MEMBERS')" class="manager-section">
          <header>
            <div>
              <h3>加入审核</h3>
              <p>
                {{
                  activeHub.joinPolicy === 'APPROVAL'
                    ? '审核通过后，申请者会成为正式成员。'
                    : '当前为开放加入，无需审核。'
                }}
              </p>
            </div>
            <span v-if="pendingMembers.length">{{ pendingMembers.length }} 条待处理</span>
          </header>
          <div v-if="pendingLoading" class="manager-empty">正在加载申请…</div>
          <div v-else-if="pendingMembers.length" class="pending-list">
            <div v-for="member in pendingMembers" :key="member.userId" class="pending-member">
              <i>{{ (member.nickName || '成员').slice(0, 1) }}</i>
              <div>
                <b>{{ member.nickName || '社区成员' }}</b
                ><small>{{ formatFullTime(member.createdAt) }} 申请加入</small>
                <small v-if="member.applicationNote" class="member-application-note"
                  >“{{ member.applicationNote }}”</small
                >
              </div>
              <button type="button" @click="reviewMember(member, false)">拒绝</button
              ><button type="button" class="approve" @click="reviewMember(member, true)">通过</button>
            </div>
          </div>
          <div v-else class="manager-empty">目前没有待审核的加入申请。</div>
        </section>

        <section class="manager-section">
          <header>
            <div>
              <h3>现场管理员</h3>
              <p>创建者可以为正式成员分配管理员角色和具体权限。</p>
            </div>
            <span v-if="hubMembers.length">{{ hubMembers.length }} 位成员</span>
          </header>
          <div v-if="hubMembers.length" class="member-toolbar">
            <el-input
              v-model="memberSearch"
              clearable
              size="small"
              placeholder="搜索成员昵称或账号"
              aria-label="搜索成员"
            />
            <el-select v-model="memberRoleFilter" size="small" aria-label="筛选成员角色">
              <el-option label="全部角色" value="ALL" />
              <el-option label="管理员" value="ADMIN" />
              <el-option label="普通成员" value="MEMBER" />
            </el-select>
          </div>
          <div v-if="memberLoading" class="manager-empty">正在加载成员…</div>
          <div v-else-if="filteredHubMembers.length" class="hub-member-list">
            <div v-for="member in filteredHubMembers" :key="member.userId" class="hub-member-row">
              <i>{{ (member.nickName || '成员').slice(0, 1) }}</i>
              <div class="hub-member-main">
                <b>{{ member.nickName || '社区成员' }}</b>
                <small>{{
                  member.role === 'OWNER' ? '创建者' : member.role === 'ADMIN' ? '管理员' : '成员'
                }}</small>
              </div>
              <template v-if="member.role !== 'OWNER' && activeHub.owned">
                <el-select v-model="member.role" size="small" class="member-role-select">
                  <el-option label="成员" value="MEMBER" /><el-option label="管理员" value="ADMIN" />
                </el-select>
                <el-checkbox-group
                  v-if="member.role === 'ADMIN'"
                  v-model="member.permissionList"
                  class="member-permissions"
                >
                  <el-checkbox
                    v-for="permission in hubPermissionOptions"
                    :key="permission.value"
                    :value="permission.value"
                    >{{ permission.label }}</el-checkbox
                  >
                </el-checkbox-group>
                <el-button
                  class="member-save"
                  size="small"
                  :loading="savingMemberPermission"
                  @click="saveMemberAdmin(member)"
                  >保存权限</el-button
                >
                <el-button
                  v-if="activeHub.owned"
                  class="member-remove"
                  size="small"
                  type="danger"
                  link
                  @click="removeMember(member)"
                  >移除</el-button
                >
              </template>
              <span v-else class="member-owner-badge">{{
                member.role === 'OWNER' ? '创建者' : member.role === 'ADMIN' ? '管理员' : '成员'
              }}</span>
            </div>
          </div>
          <div v-else-if="hubMembers.length" class="manager-empty">没有符合条件的成员。</div>
          <div v-else class="manager-empty">目前没有正式成员。</div>
        </section>

        <section v-if="hasHubPermission('INVITE_MEMBERS')" class="manager-section">
          <header>
            <div>
              <h3>邀请链接</h3>
              <p>通过链接加入的用户无需审核；每条链接 7 天有效。</p>
            </div>
          </header>
          <div v-if="inviteUrl" class="invite-link-row">
            <el-input :model-value="inviteUrl" readonly /><el-button @click="copyInviteLink"
              >复制链接</el-button
            >
          </div>
          <el-button v-else :loading="creatingInvitation" @click="createInvitation">生成邀请链接</el-button>
          <small v-if="invitation?.expiresAt" class="invite-expiry"
            >有效期至 {{ formatFullTime(invitation.expiresAt) }}</small
          >
        </section>

        <section v-if="hasHubPermission('MANAGE_POST_TYPES')" class="manager-section">
          <header>
            <div>
              <h3>现场帖子类型</h3>
              <p>平台固定类型由管理员维护；你可以为这个现场增加专属类型。</p>
            </div>
          </header>
          <div class="fixed-type-list">
            <span v-for="type in postTypes.filter((item) => item.systemFixed)" :key="type.typeCode"
              >{{ type.displayName }}<small>平台固定</small></span
            >
          </div>
          <div v-if="customPostTypes.length" class="custom-type-list">
            <div v-for="type in customPostTypes" :key="type.postTypeId">
              <div>
                <el-input v-model="type.displayName" maxlength="20" size="small" /><el-input
                  v-model="type.description"
                  maxlength="160"
                  size="small"
                  placeholder="类型说明"
                />
              </div>
              <div>
                <button type="button" @click="updateCustomPostType(type)">保存</button
                ><button type="button" @click="deleteCustomPostType(type)">删除</button>
              </div>
            </div>
          </div>
          <div class="custom-type-create">
            <el-input
              v-model="newPostTypeName"
              maxlength="20"
              placeholder="新类型名称，例如：票务情报"
            /><el-input
              v-model="newPostTypeDescription"
              maxlength="160"
              placeholder="一句话说明适合发布什么内容"
            /><el-button :loading="savingPostType" @click="createCustomPostType">增加类型</el-button>
          </div>
          <p class="type-history-note">删除后不能再发布此类型，但历史帖子仍会保留类型名称。</p>
        </section>

        <section v-if="hasHubPermission('MANAGE_EVENTS')" class="manager-section manager-action-section">
          <div>
            <h3>近期活动</h3>
            <p>创建或编辑现场活动，并查看成员参与度。</p>
          </div>
          <el-button @click="openEventManagerFromHubManager">管理近期活动</el-button>
        </section>

        <section v-if="activeHub.owned" class="manager-section danger-zone">
          <header>
            <div>
              <h3>删除兴趣现场</h3>
              <p>会一并删除现场内的帖子、评论、成员关系和邀请，且无法恢复。</p>
            </div>
            <button v-if="activeHub.owned" type="button" @click="deleteHub">删除现场</button>
          </header>
        </section>
      </div>
    </el-dialog>

    <el-dialog
      v-model="categoryManagerVisible"
      class="category-manager-dialog"
      title="整理我的兴趣现场"
      width="min(94vw, 680px)"
    >
      <p class="dialog-tip">这些分类只对你自己可见。你可以把多个现场放进“音乐”“游戏”或任意自定义分类。</p>
      <div class="category-create-row">
        <el-input
          v-model="newCategoryName"
          maxlength="20"
          placeholder="新分类名称"
          @keyup.enter="createCategory"
        /><el-button type="primary" :loading="creatingCategory" @click="createCategory">新建分类</el-button>
      </div>
      <div v-if="categories.length" class="category-chips">
        <span v-for="category in categories" :key="category.categoryId"
          >{{ category.name }} <small>{{ category.hubCount || 0 }}</small
          ><button type="button" title="删除分类" @click="deleteCategory(category)">×</button></span
        >
      </div>
      <div v-if="hubs.length" class="hub-category-list">
        <div v-for="hub in hubs" :key="hub.hubId">
          <i class="hub-icon"
            ><img v-if="hub.coverUrl" :src="resourceUrl(hub.coverUrl)" alt="" /><span v-else>{{
              hub.name.slice(0, 1)
            }}</span></i
          >
          <div>
            <b>{{ hub.name }}</b
            ><small>{{ hub.owned ? '我创建的现场' : '已加入的现场' }}</small>
          </div>
          <el-select
            :model-value="hub.personalCategoryId"
            clearable
            placeholder="未分类"
            @change="(categoryId) => assignCategory(hub, categoryId)"
            ><el-option
              v-for="category in categories"
              :key="category.categoryId"
              :label="category.name"
              :value="category.categoryId"
          /></el-select>
        </div>
      </div>
      <div v-else class="manager-empty">加入或创建兴趣现场后，就可以在这里整理。</div>
    </el-dialog>
  </main>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginStore } from '@/stores/loginStore.js'
import { uploadImage } from '@/utils/Api.js'
import CommunityLeftRail from '@/components/community/CommunityLeftRail.vue'
import CommunityRightRail from '@/components/community/CommunityRightRail.vue'
import RichTextEditor from '@/components/community/RichTextEditor.vue'
import { richTextExcerpt, richTextLength, sanitizeRichText } from '@/utils/richText.js'
import { postReviewFingerprint } from '@/utils/smartPosting.js'

const { proxy } = getCurrentInstance()
const loginStore = useLoginStore()
const route = useRoute()
const router = useRouter()
const hubs = ref([])
const hubDirections = ref([])
const topics = ref([])
const events = ref([])
const posts = ref([])
const composerEvents = ref([])
const composerTopics = ref([])
const postTypes = ref([])
const composerPostTypes = ref([])
const customPostTypes = ref([])
const selectedHub = ref(null)
const selectedType = ref(null)
const composerVisible = ref(false)
const hubComposerVisible = ref(false)
const hubManagerVisible = ref(false)
const categoryManagerVisible = ref(false)
const publishing = ref(false)
const reviewing = ref(false)
const reviewResult = ref(null)
const reviewedFingerprint = ref('')
const optimizedContentOfferVisible = ref(false)
const creatingHub = ref(false)
const uploadingCover = ref(false)
const uploadingHubCover = ref(false)
const creatingTopic = ref(false)
const savingTopic = ref(false)
const creatingInvitation = ref(false)
const creatingCategory = ref(false)
const pendingLoading = ref(false)
const savingPostType = ref(false)
const changeContext = ref(null)
const categories = ref([])
const pendingMembers = ref([])
const hubMembers = ref([])
const memberLoading = ref(false)
const savingMemberPermission = ref(false)
const removingMember = ref(false)
const memberSearch = ref('')
const memberRoleFilter = ref('ALL')
const hubPermissionOptions = [
  { value: 'REVIEW_MEMBERS', label: '审核成员' },
  { value: 'MANAGE_EVENTS', label: '管理活动' },
  { value: 'MANAGE_POST_TYPES', label: '管理帖子类型' },
  { value: 'INVITE_MEMBERS', label: '生成邀请' },
]
const eventManagerVisible = ref(false)
const eventStatsVisible = ref(false)
const eventEditing = ref(false)
const savingEvent = ref(false)
const managedEvents = ref([])
const eventStats = ref(null)
const eventDraft = reactive({
  eventId: null,
  title: '',
  description: '',
  startsAt: '',
  endsAt: '',
  venue: '',
  sourceUrl: '',
})
const invitation = ref(null)
const inviteUrl = ref('')
const newCategoryName = ref('')
const newTopicName = ref('')
const newPostTypeName = ref('')
const newPostTypeDescription = ref('')
const composer = reactive({
  editingPostId: null,
  hubId: null,
  topicId: null,
  eventId: null,
  changeId: null,
  type: 'DISCUSSION',
  title: '',
  body: '',
  coverUrl: '',
  images: [],
})
const hubDraft = reactive({
  name: '',
  description: '',
  category: 'GENERAL',
  coverUrl: '',
  joinPolicy: 'OPEN',
  joinQuestion: '',
  joinAnswer: '',
})
const filters = computed(() => [
  { value: null, label: '热门' },
  ...postTypes.value.map((type) => ({ value: type.typeCode, label: type.displayName })),
])
const actions = [
  { value: 'WANT', label: '想去' },
  { value: 'ATTENDED', label: '已去' },
  { value: 'USING', label: '正在用' },
  { value: 'WATCHING', label: '观望' },
  { value: 'RECOMMEND', label: '推荐' },
]
const attendanceStatuses = [
  { value: 'WANT', label: '想参加' },
  { value: 'ATTENDING', label: '参加中' },
  { value: 'ATTENDED', label: '已参加' },
]
const activeHub = computed(() => hubs.value.find((hub) => hub.hubId === selectedHub.value))
const filteredHubMembers = computed(() => {
  const query = memberSearch.value.trim().toLowerCase()
  const role = memberRoleFilter.value
  return hubMembers.value.filter((member) => {
    if (role !== 'ALL' && member.role !== role) return false
    if (!query) return true
    return [member.nickName, member.userId]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query))
  })
})
const canManageHub = computed(() =>
  Boolean(activeHub.value && (activeHub.value.owned || activeHub.value.memberRole === 'ADMIN')),
)
const hasHubPermission = (permission) => {
  if (activeHub.value?.owned) return true
  try {
    const permissions = JSON.parse(activeHub.value?.memberPermissions || '[]')
    return Array.isArray(permissions) && permissions.includes(permission)
  } catch {
    return false
  }
}
const selectedHubName = computed(() => activeHub.value?.name)
const isEditing = computed(() => Boolean(composer.editingPostId))
const composerTitle = computed(() => (isEditing.value ? '编辑帖子' : '发布分享'))
const selectedComposerType = computed(() =>
  composerPostTypes.value.find((type) => type.typeCode === composer.type),
)
const composerFingerprint = computed(() => postReviewFingerprint(composer))
const reviewIsCurrent = computed(
  () => Boolean(reviewResult.value) && reviewedFingerprint.value === composerFingerprint.value,
)
const safeOptimizedContent = computed(() => sanitizeRichText(reviewResult.value?.optimized_content || ''))
const localReviewHints = computed(() => {
  const hints = []
  if (!composer.title.trim()) hints.push('标题为空')
  const length = richTextLength(composer.body)
  if (length > 0 && length < 30) hints.push('正文较短')
  if (/(?:\+?86[- ]?)?1[3-9]\d{9}/.test(composer.body)) hints.push('正文中可能含手机号')
  if (/\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/i.test(composer.body)) hints.push('正文中可能含邮箱地址')
  if (
    /(?:api[_ -]?key|access[_ -]?token|secret|password|密钥|密码)\s*[:=：]/i.test(composer.body) ||
    /\bbearer\s+[A-Za-z0-9._~+/=-]{12,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|\beyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\b/i.test(
      composer.body,
    )
  )
    hints.push('正文中可能含密钥或密码')
  if (['QUESTION', '问题'].includes(composer.type) && !/环境|版本|系统|设备|运行/i.test(composer.body))
    hints.push('问题帖缺少运行环境')
  return hints
})
const unselectedSuggestedTopics = computed(() => {
  if (!reviewResult.value?.suggested_topic_ids) return []
  return reviewResult.value.suggested_topic_ids
    .filter((topicId) => Number(topicId) !== Number(composer.topicId))
    .map((topicId) => composerTopics.value.find((topic) => Number(topic.topicId) === Number(topicId)))
    .filter(Boolean)
})
const loggedIn = () => Object.keys(loginStore.userInfo || {}).length > 0
const groupedHubs = computed(() => {
  if (!loggedIn()) return hubs.value.length ? [{ key: 'popular', name: '热门现场', hubs: hubs.value }] : []
  const categoryIds = new Set(categories.value.map((category) => Number(category.categoryId)))
  const groups = categories.value
    .map((category) => ({
      key: `category-${category.categoryId}`,
      name: category.name,
      hubs: hubs.value.filter((hub) => Number(hub.personalCategoryId) === Number(category.categoryId)),
    }))
    .filter((group) => group.hubs.length)
  const orphanCategorized = hubs.value.filter(
    (hub) => hub.personalCategoryId && !categoryIds.has(Number(hub.personalCategoryId)),
  )
  if (orphanCategorized.length) {
    groups.push({
      key: 'categorized-pending-sync',
      name: '已分类',
      hubs: orphanCategorized,
    })
  }
  const uncategorized = hubs.value.filter((hub) => !hub.personalCategoryId)
  const owned = uncategorized.filter((hub) => hub.owned)
  const joined = uncategorized.filter((hub) => !hub.owned)
  if (owned.length) groups.push({ key: 'owned', name: '我创建的', hubs: owned })
  if (joined.length) groups.push({ key: 'uncategorized', name: '未分类', hubs: joined })
  return groups
})
const canManage = (post) => loggedIn() && String(post.authorId) === String(loginStore.userInfo?.userId)
const requestEvents = async (hubId) =>
  (
    await proxy.Request({
      url: proxy.Api.zentideCommunityEvents,
      params: { hubId, limit: 12 },
      showLoading: false,
      showError: false,
    })
  )?.data || []
const requestTopics = async (hubId) =>
  hubId
    ? (
        await proxy.Request({
          url: proxy.Api.zentideCommunityTopics,
          params: { hubId, limit: 20 },
          showLoading: false,
          showError: false,
        })
      )?.data || []
    : []
const requestPostTypes = async (hubId) =>
  (
    await proxy.Request({
      url: proxy.Api.zentideCommunityPostTypes,
      params: { hubId },
      showLoading: false,
      showError: false,
    })
  )?.data || []
const loadPostTypes = async (hubId) => {
  postTypes.value = await requestPostTypes(hubId)
  if (selectedType.value && !postTypes.value.some((type) => type.typeCode === selectedType.value))
    selectedType.value = null
}
const loadCategories = async () => {
  if (!loggedIn()) {
    categories.value = []
    return
  }
  const result = await proxy.Request({
    url: proxy.Api.zentideCommunityHubCategories,
    params: {},
    showLoading: false,
    showError: false,
  })
  if (result) categories.value = result.data || []
}
const loadHubDirections = async () => {
  const result = await proxy.Request({
    url: proxy.Api.zentideCommunityHubDirections,
    params: {},
    showLoading: false,
    showError: false,
  })
  if (result) {
    hubDirections.value = result.data || []
    if (!hubDirections.value.some((item) => item.code === hubDraft.category))
      hubDraft.category = hubDirections.value[0]?.code || 'GENERAL'
  }
}
const loadHubs = async () => {
  const result = await proxy.Request({
    url: proxy.Api.zentideCommunityHubs,
    params: {},
    showLoading: false,
    showError: false,
  })
  if (!result) return
  hubs.value = result.data || []
  if (!composer.hubId && hubs.value[0]) composer.hubId = hubs.value[0].hubId
  if (selectedHub.value && !hubs.value.some((hub) => Number(hub.hubId) === Number(selectedHub.value)))
    selectedHub.value = null
}
const loadFeed = async () => {
  const isContextHub = !route.query.hubId || Number(route.query.hubId) === Number(selectedHub.value)
  const result = await proxy.Request({
    url: proxy.Api.zentideCommunityFeed,
    params: {
      hubId: selectedHub.value,
      type: selectedType.value,
      topicId: isContextHub ? route.query.topicId || null : null,
      changeId: isContextHub ? route.query.changeId || null : null,
      limit: 50,
    },
    showLoading: false,
    showError: false,
  })
  if (result)
    posts.value = (result.data || []).map((post) => ({
      ...post,
      expanded: false,
      comments: [],
      commentDraft: '',
    }))
}
const loadHubContext = async () => {
  if (!selectedHub.value) {
    topics.value = []
    events.value = []
    await loadPostTypes(null)
    return
  }
  const [topicResult, eventResult, typeResult] = await Promise.all([
    requestTopics(selectedHub.value),
    requestEvents(selectedHub.value),
    requestPostTypes(selectedHub.value),
  ])
  topics.value = topicResult
  events.value = eventResult
  postTypes.value = typeResult
  if (selectedType.value && !typeResult.some((type) => type.typeCode === selectedType.value))
    selectedType.value = null
}
const selectHub = async (value) => {
  selectedHub.value = value
  const query = { ...route.query }
  if (value) query.hubId = value
  else delete query.hubId
  delete query.createHub
  delete query.topicId
  delete query.changeId
  await router.replace({ path: '/community', query })
  await loadHubContext()
  await loadFeed()
}
const selectType = (value) => {
  selectedType.value = value
  loadFeed()
}
const openAssistant = () => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  if (!activeHub.value) return proxy.Message.warning('请先选择一个兴趣现场')
  if (!activeHub.value.joined) return proxy.Message.warning('加入兴趣现场后才能使用社区小助手')
  router.push({ name: 'community-assistant', params: { hubId: activeHub.value.hubId } })
}
const resetComposer = () => {
  const hubId = selectedHub.value || composer.hubId || hubs.value[0]?.hubId || null
  Object.assign(composer, {
    editingPostId: null,
    hubId,
    topicId: null,
    eventId: null,
    changeId: null,
    type: '',
    title: '',
    body: '',
    coverUrl: '',
    images: [],
  })
  changeContext.value = null
  creatingTopic.value = false
  newTopicName.value = ''
  reviewResult.value = null
  reviewedFingerprint.value = ''
  optimizedContentOfferVisible.value = false
}
const draftStorageKey = (hubId = composer.hubId) =>
  `zentide-post-draft:${loginStore.userInfo.userId || 'anonymous'}:${hubId || 'none'}`
const saveComposerDraft = () => {
  if (isEditing.value || (!composer.title.trim() && !composer.body.trim())) {
    return proxy.Message.warning('还没有可保存的草稿内容')
  }
  localStorage.setItem(draftStorageKey(), JSON.stringify({ ...composer, savedAt: Date.now() }))
  composerVisible.value = false
  proxy.Message.success('草稿已保存')
}
const restoreComposerDraft = () => {
  if (isEditing.value || !composer.hubId) return
  try {
    const saved = JSON.parse(localStorage.getItem(draftStorageKey()) || 'null')
    if (!saved) return
    Object.assign(composer, {
      title: saved.title || '',
      body: saved.body || '',
      type: saved.type || composer.type,
      topicId: saved.topicId || null,
      eventId: saved.eventId || null,
      coverUrl: saved.coverUrl || '',
      images: Array.isArray(saved.images) ? saved.images : [],
    })
    proxy.Message.info('已恢复上次保存的草稿')
  } catch {
    localStorage.removeItem(draftStorageKey())
  }
}
const openComposer = async () => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  resetComposer()
  composerVisible.value = true
  const [eventList, topicList, typeList] = await Promise.all([
    requestEvents(composer.hubId),
    requestTopics(composer.hubId),
    requestPostTypes(composer.hubId),
  ])
  composerEvents.value = eventList
  composerTopics.value = topicList
  composerPostTypes.value = typeList
  composer.type = typeList[0]?.typeCode || ''
  restoreComposerDraft()
}
const openEditComposer = async (postId) => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityPosts}/${postId}`,
    params: {},
    showLoading: false,
  })
  const post = result?.data
  if (!post || !canManage(post)) return proxy.Message.warning('只能编辑自己发布的帖子')
  composerVisible.value = false
  Object.assign(composer, {
    editingPostId: post.postId,
    hubId: post.hubId,
    topicId: post.topicId || null,
    eventId: post.eventId || null,
    changeId: post.changeId || null,
    type: post.postType || '',
    title: post.title || '',
    body: post.body || '',
    coverUrl: post.coverUrl || '',
    images: postImages(post),
  })
  changeContext.value = post.changeId ? { changeTitle: post.changeTitle } : null
  const [eventList, topicList, typeList] = await Promise.all([
    requestEvents(post.hubId),
    requestTopics(post.hubId),
    requestPostTypes(post.hubId),
  ])
  composerEvents.value = eventList
  composerTopics.value = topicList
  composerPostTypes.value = typeList
  if (!typeList.some((type) => type.typeCode === post.postType))
    composerPostTypes.value = [
      {
        typeCode: post.postType,
        displayName: post.postTypeLabel || '原类型',
        description: '该类型已停止新发布，保存时可保留。',
        status: 'DELETED',
      },
      ...typeList,
    ]
  composerVisible.value = true
}
const showHubComposer = () => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  Object.assign(hubDraft, {
    name: '',
    description: '',
    category: 'GENERAL',
    coverUrl: '',
    joinPolicy: 'OPEN',
    joinQuestion: '',
    joinAnswer: '',
  })
  hubComposerVisible.value = true
  if (route.query.createHub) router.replace('/community')
}
const chooseTopic = async (topic) => {
  await openComposer()
  composer.topicId = topic.topicId
}
const joinButtonLabel = (hub) =>
  hub.membershipStatus === 'PENDING' ? '等待审核' : hub.joined ? '退出现场' : '加入现场'
const toggleHubMembership = async () => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  if (!activeHub.value) return
  const hub = activeHub.value
  const active = !hub.joined
  let answer = ''
  let applicationNote = ''
  if (active && hub.joinPolicy === 'QUESTION') {
    answer = window.prompt(hub.joinQuestion || '请回答加入问题') || ''
    if (!answer.trim()) return
  } else if (active && hub.joinPolicy === 'APPROVAL') {
    applicationNote = window.prompt('请填写加入申请说明（可选）') || ''
  }
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityHubMembership}/${hub.hubId}/membership`,
    params: { active, answer, applicationNote },
    showLoading: false,
  })
  if (!result) return
  const status = result.data
  if (status === 'PENDING') {
    hub.membershipStatus = 'PENDING'
    hub.joined = false
    return proxy.Message.success('加入申请已提交，等待创建者审核')
  }
  if (status === 'ACTIVE') {
    hub.membershipStatus = 'ACTIVE'
    hub.joined = true
    hub.memberCount = Number(hub.memberCount || 0) + 1
    return proxy.Message.success(`已加入「${hub.name}」`)
  }
  proxy.Message.success(`已退出「${hub.name}」`)
  selectedHub.value = null
  await Promise.all([loadHubs(), loadCategories()])
  await Promise.all([loadFeed(), loadHubContext()])
  await router.replace('/community')
}
const createHub = async () => {
  if (hubDraft.name.trim().length < 2) return proxy.Message.warning('兴趣现场名称至少需要两个字符')
  creatingHub.value = true
  const result = await proxy.Request({
    url: proxy.Api.zentideCommunityHubCreate,
    params: {
      name: hubDraft.name.trim(),
      description: hubDraft.description.trim(),
      category: hubDraft.category,
      coverUrl: hubDraft.coverUrl,
      joinPolicy: hubDraft.joinPolicy,
      joinQuestion: hubDraft.joinQuestion.trim(),
      joinAnswer: hubDraft.joinAnswer.trim(),
    },
    showLoading: false,
  })
  creatingHub.value = false
  if (!result) return
  hubComposerVisible.value = false
  const created = result.data
  selectedHub.value = created.hubId
  await Promise.all([loadHubs(), loadCategories()])
  await Promise.all([loadFeed(), loadHubContext()])
  await router.replace({ path: '/community', query: { hubId: created.hubId } })
  proxy.Message.success(`已创建「${created.name}」`)
}
const createTopic = async () => {
  const name = newTopicName.value.trim().replace(/^#+/, '').trim()
  if (!composer.hubId) return proxy.Message.warning('请先选择兴趣现场')
  if (name.length < 2) return proxy.Message.warning('话题名称至少需要两个字符')
  savingTopic.value = true
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityHubMembership}/${composer.hubId}/topics/create`,
    params: { name },
    showLoading: false,
  })
  savingTopic.value = false
  if (!result) return
  const topic = result.data
  if (!composerTopics.value.some((item) => Number(item.topicId) === Number(topic.topicId)))
    composerTopics.value.unshift(topic)
  composer.topicId = topic.topicId
  newTopicName.value = ''
  creatingTopic.value = false
  proxy.Message.success(`已关联话题 #${topic.canonicalName}`)
}
const openHubManager = async () => {
  if (!canManageHub.value) return
  pendingMembers.value = []
  hubMembers.value = []
  invitation.value = null
  inviteUrl.value = ''
  customPostTypes.value = []
  memberSearch.value = ''
  memberRoleFilter.value = 'ALL'
  newPostTypeName.value = ''
  newPostTypeDescription.value = ''
  hubManagerVisible.value = true
  pendingLoading.value = true
  memberLoading.value = true
  const [result, customResult, membersResult] = await Promise.all([
    hasHubPermission('REVIEW_MEMBERS')
      ? proxy.Request({
          url: `${proxy.Api.zentideCommunityHubMembership}/${activeHub.value.hubId}/members/pending`,
          params: {},
          showLoading: false,
          showError: false,
        })
      : Promise.resolve(null),
    hasHubPermission('MANAGE_POST_TYPES')
      ? proxy.Request({
          url: `${proxy.Api.zentideCommunityHubMembership}/${activeHub.value.hubId}/post-types`,
          params: {},
          showLoading: false,
          showError: false,
        })
      : Promise.resolve(null),
    proxy.Request({
      url: `${proxy.Api.zentideCommunityHubMembership}/${activeHub.value.hubId}/members`,
      params: {},
      showLoading: false,
      showError: false,
    }),
  ])
  pendingLoading.value = false
  memberLoading.value = false
  if (result) pendingMembers.value = result.data || []
  if (customResult) customPostTypes.value = customResult.data || []
  if (membersResult) {
    hubMembers.value = (membersResult.data || []).map((member) => ({
      ...member,
      permissionList: parsePermissions(member.permissionsJson),
      savedRole: member.role,
      savedPermissionList: parsePermissions(member.permissionsJson),
    }))
  }
}
const parsePermissions = (value) => {
  try {
    const parsed = JSON.parse(value || '[]')
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}
const saveMemberAdmin = async (member) => {
  savingMemberPermission.value = true
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityHubMembership}/${activeHub.value.hubId}/members/${encodeURIComponent(member.userId)}/admin`,
    params: {
      role: member.role,
      permissionsJson: JSON.stringify(member.role === 'ADMIN' ? member.permissionList || [] : []),
    },
    showLoading: false,
  })
  savingMemberPermission.value = false
  if (!result) {
    member.role = member.savedRole
    member.permissionList = [...(member.savedPermissionList || [])]
    return
  }
  const updated = result.data || {}
  member.role = updated.role || member.role
  member.permissionList = parsePermissions(updated.permissionsJson)
  member.savedRole = member.role
  member.savedPermissionList = [...member.permissionList]
  proxy.Message.success(member.role === 'ADMIN' ? '管理员权限已保存' : '已取消管理员身份')
}
const removeMember = (member) =>
  proxy.Confirm({
    message: `确定将「${member.nickName || '该成员'}」移出兴趣现场吗？移除后对方可以重新申请加入。`,
    okfun: async () => {
      if (removingMember.value) return
      removingMember.value = true
      const result = await proxy.Request({
        url: `${proxy.Api.zentideCommunityHubMembership}/${activeHub.value.hubId}/members/${encodeURIComponent(member.userId)}/remove`,
        params: {},
        showLoading: false,
      })
      removingMember.value = false
      if (!result) return
      hubMembers.value = hubMembers.value.filter((item) => item.userId !== member.userId)
      if (activeHub.value)
        activeHub.value.memberCount = Math.max(0, Number(activeHub.value.memberCount || 1) - 1)
      proxy.Message.success('成员已移出兴趣现场')
    },
  })
const createCustomPostType = async () => {
  const displayName = newPostTypeName.value.trim()
  if (!displayName) return proxy.Message.warning('请输入帖子类型名称')
  savingPostType.value = true
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityHubMembership}/${activeHub.value.hubId}/post-types/create`,
    params: { displayName, description: newPostTypeDescription.value.trim() },
    showLoading: false,
  })
  savingPostType.value = false
  if (!result) return
  customPostTypes.value.push(result.data)
  postTypes.value.push(result.data)
  newPostTypeName.value = ''
  newPostTypeDescription.value = ''
  proxy.Message.success(`已增加帖子类型「${result.data.displayName}」`)
}
const updateCustomPostType = async (type) => {
  if (!type.displayName?.trim()) return proxy.Message.warning('帖子类型名称不能为空')
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityHubMembership}/${activeHub.value.hubId}/post-types/${type.postTypeId}/update`,
    params: { displayName: type.displayName.trim(), description: type.description?.trim() || '' },
    showLoading: false,
  })
  if (!result) return
  proxy.Message.success('帖子类型已更新')
  await Promise.all([loadHubContext(), openHubManager()])
}
const deleteCustomPostType = (type) =>
  proxy.Confirm({
    message: `删除帖子类型「${type.displayName}」吗？历史帖子会保留这个类型名称，但以后不能再选择它发布。`,
    okfun: async () => {
      const result = await proxy.Request({
        url: `${proxy.Api.zentideCommunityHubMembership}/${activeHub.value.hubId}/post-types/${type.postTypeId}/delete`,
        params: {},
        showLoading: false,
      })
      if (!result) return
      customPostTypes.value = customPostTypes.value.filter((item) => item.postTypeId !== type.postTypeId)
      postTypes.value = postTypes.value.filter((item) => item.postTypeId !== type.postTypeId)
      if (selectedType.value === type.typeCode) {
        selectedType.value = null
        await loadFeed()
      }
      proxy.Message.success('帖子类型已删除')
    },
  })
const reviewMember = async (member, approve) => {
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityHubMembership}/${activeHub.value.hubId}/members/${encodeURIComponent(member.userId)}/review`,
    params: { approve },
    showLoading: false,
  })
  if (!result) return
  pendingMembers.value = pendingMembers.value.filter((item) => item.userId !== member.userId)
  if (approve) activeHub.value.memberCount = Number(activeHub.value.memberCount || 0) + 1
  proxy.Message.success(approve ? '已通过加入申请' : '已拒绝加入申请')
}
const createInvitation = async () => {
  creatingInvitation.value = true
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityHubMembership}/${activeHub.value.hubId}/invitations`,
    params: {},
    showLoading: false,
  })
  creatingInvitation.value = false
  if (!result) return
  invitation.value = result.data
  inviteUrl.value = `${window.location.origin}${router.resolve({ path: `/community/invite/${result.data.inviteToken}` }).href}`
}
const copyInviteLink = async () => {
  try {
    await navigator.clipboard.writeText(inviteUrl.value)
    proxy.Message.success('邀请链接已复制')
  } catch {
    proxy.Message.warning('无法自动复制，请选中链接后手动复制')
  }
}
const deleteHub = () =>
  proxy.Confirm({
    message: `确定删除「${activeHub.value?.name}」吗？现场内的帖子、评论、成员与邀请都会被永久删除。`,
    okfun: async () => {
      const hubId = activeHub.value?.hubId
      const result = await proxy.Request({
        url: `${proxy.Api.zentideCommunityHubMembership}/${hubId}/delete`,
        params: {},
        showLoading: false,
      })
      if (!result) return
      hubManagerVisible.value = false
      selectedHub.value = null
      await Promise.all([loadHubs(), loadCategories()])
      await Promise.all([loadFeed(), loadHubContext()])
      await router.replace('/community')
      proxy.Message.success('兴趣现场已删除')
    },
  })
const openCategoryManager = async () => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  await Promise.all([loadCategories(), loadHubs()])
  categoryManagerVisible.value = true
}
const createCategory = async () => {
  const name = newCategoryName.value.trim()
  if (!name) return proxy.Message.warning('请输入分类名称')
  creatingCategory.value = true
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityHubCategories}/create`,
    params: { name },
    showLoading: false,
  })
  creatingCategory.value = false
  if (!result) return
  categories.value.push(result.data)
  newCategoryName.value = ''
  proxy.Message.success(`已新建分类「${result.data.name}」`)
}
const deleteCategory = (category) =>
  proxy.Confirm({
    message: `删除分类「${category.name}」吗？其中的兴趣现场会回到未分类，不会退出现场。`,
    okfun: async () => {
      const result = await proxy.Request({
        url: `${proxy.Api.zentideCommunityHubCategories}/${category.categoryId}/delete`,
        params: {},
        showLoading: false,
      })
      if (!result) return
      hubs.value.forEach((hub) => {
        if (Number(hub.personalCategoryId) === Number(category.categoryId)) {
          hub.personalCategoryId = null
          hub.personalCategoryName = null
        }
      })
      categories.value = categories.value.filter((item) => item.categoryId !== category.categoryId)
      proxy.Message.success('分类已删除')
    },
  })
const assignCategory = async (hub, categoryId) => {
  const normalized = categoryId || null
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityHubMembership}/${hub.hubId}/category`,
    params: { categoryId: normalized },
    showLoading: false,
  })
  if (!result) return
  hub.personalCategoryId = normalized
  hub.personalCategoryName =
    categories.value.find((item) => Number(item.categoryId) === Number(normalized))?.name || null
  await loadCategories()
  // Re-read the server projection so the left rail and category manager share the same state.
  await loadHubs()
}
const publishPost = async () => {
  if (!composer.hubId) return proxy.Message.warning('请先选择兴趣现场')
  if (!composer.type) return proxy.Message.warning('请选择帖子类型')
  if (!composer.title.trim()) return proxy.Message.warning('请填写标题')
  const bodyLength = richTextLength(composer.body)
  if (bodyLength < 2) return proxy.Message.warning('内容至少需要两个字符')
  if (bodyLength > 8000) return proxy.Message.warning('正文不能超过 8000 个字符')
  if (!isEditing.value && !reviewIsCurrent.value) {
    const reviewed = await runSmartReview()
    if (!reviewed) return
    if (reviewed.publish_blocked) {
      const reasons = Array.isArray(reviewed.blocking_reasons) ? reviewed.blocking_reasons.filter(Boolean) : []
      return proxy.Message.error(`禁止发布：${reasons.join('；') || '检测到不符合社区规则的内容'}。请修改后重新检查`)
    }
    if (optimizedContentOfferVisible.value) return
    if (reviewed.advice !== 'ready') return
  }
  if (!isEditing.value && reviewResult.value?.publish_blocked) {
    const reasons = Array.isArray(reviewResult.value.blocking_reasons) ? reviewResult.value.blocking_reasons.filter(Boolean) : []
    return proxy.Message.error(`禁止发布：${reasons.join('；') || '检测到不符合社区规则的内容'}。请修改后重新检查`)
  }
  publishing.value = true
  const editing = isEditing.value
  const reviewAdvice = reviewResult.value?.advice
  const url = editing
    ? `${proxy.Api.zentideCommunityPosts}/${composer.editingPostId}/update`
    : proxy.Api.zentideCommunityPosts
  const result = await proxy.Request({
    url,
    params: {
      hubId: composer.hubId,
      topicId: composer.topicId,
      eventId: composer.eventId,
      changeId: composer.changeId,
      type: composer.type,
      title: composer.title.trim(),
      body: composer.body.trim(),
      coverUrl: composer.coverUrl,
      mediaJson: JSON.stringify(composer.images),
      reviewId: editing ? undefined : reviewResult.value.review_id,
      forcePublish: !editing && reviewResult.value.advice !== 'ready',
    },
    showLoading: false,
  })
  publishing.value = false
  if (!result) return
  if (!editing) localStorage.removeItem(draftStorageKey())
  composerVisible.value = false
  resetComposer()
  proxy.Message.success(
    editing ? '帖子已更新' : reviewAdvice === 'manual_review' ? '已提交人工审核' : '已发布',
  )
  await Promise.all([loadFeed(), loadHubContext(), loadHubs()])
}
const runSmartReview = async () => {
  if (!composer.hubId) {
    proxy.Message.warning('请先选择兴趣现场')
    return null
  }
  if (richTextLength(composer.body) < 2) {
    proxy.Message.warning('请先填写正文')
    return null
  }
  if (!composer.title.trim()) {
    proxy.Message.warning('请先填写标题')
    return null
  }
  reviewing.value = true
  const fingerprint = composerFingerprint.value
  try {
    const result = await proxy.Request({
      url: proxy.Api.zentidePostReview,
      dataType: 'json',
      params: {
        sceneId: composer.hubId,
        title: composer.title.trim(),
        content: composer.body.trim(),
        selectedPostType: composer.type,
        selectedTopicIds: composer.topicId ? [composer.topicId] : [],
        draftId: `draft_${loginStore.userInfo.userId || 'current'}_${composer.hubId}`,
      },
      timeout: 22000,
      showLoading: false,
    })
    if (!result) return null
    reviewResult.value = result.data
    reviewedFingerprint.value = fingerprint
    optimizedContentOfferVisible.value = Boolean(
      result.data.optimized_content && !result.data.publish_blocked && safeOptimizedContent.value,
    )
    return result.data
  } finally {
    reviewing.value = false
  }
}
const viewSimilarPost = (postId) => {
  trackReviewEvent('similar_post_clicked', postId)
  window.open(router.resolve({ name: 'community-post', params: { postId } }).href, '_blank', 'noopener')
}
const acceptSuggestedTitle = () => {
  if (!reviewResult.value?.suggested_title) return
  trackReviewEvent('suggested_title_accepted')
  composer.title = reviewResult.value.suggested_title
}
const acceptSuggestedTopic = (topicId) => {
  trackReviewEvent('suggested_topic_accepted', topicId)
  composer.topicId = topicId
}
const acceptOptimizedContent = () => {
  if (!reviewIsCurrent.value || !safeOptimizedContent.value) {
    optimizedContentOfferVisible.value = false
    return proxy.Message.warning('帖子内容已经变化，请重新运行智能检查')
  }
  trackReviewEvent('optimized_content_accepted')
  composer.body = safeOptimizedContent.value
  optimizedContentOfferVisible.value = false
  proxy.Message.success('已采用优化正文，请重新运行智能检查')
}
const trackReviewEvent = (event, targetId = null) => {
  if (!reviewResult.value?.review_id) return
  void proxy.Request({
    url: proxy.Api.zentidePostReviewEvents,
    dataType: 'json',
    params: { event, reviewId: reviewResult.value.review_id, sceneId: composer.hubId, targetId },
    showLoading: false,
    showError: false,
  })
}
const deletePost = (post) =>
  proxy.Confirm({
    message: '删除后帖子、评论和互动记录将无法恢复，确定删除吗？',
    okfun: async () => {
      const result = await proxy.Request({
        url: `${proxy.Api.zentideCommunityPosts}/${post.postId}/delete`,
        params: {},
        showLoading: false,
      })
      if (!result) return
      posts.value = posts.value.filter((item) => item.postId !== post.postId)
      proxy.Message.success('帖子已删除')
      await Promise.all([loadHubs(), loadHubContext()])
    },
  })
const managePost = (post, command) => {
  if (command === 'edit') openEditComposer(post.postId)
  if (command === 'delete') deletePost(post)
}
const toggleLike = async (post) => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  const active = !post.liked
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityPosts}/${post.postId}/like`,
    params: { active },
    showLoading: false,
  })
  if (result) {
    post.liked = active
    post.likeCount = Math.max(0, Number(post.likeCount || 0) + (active ? 1 : -1))
  }
}
const toggleBookmark = async (post) => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  const active = !post.bookmarked
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityPosts}/${post.postId}/bookmark`,
    params: { active },
    showLoading: false,
  })
  if (result) post.bookmarked = active
}
const setAction = async (post, action) => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  const value = post.myAction === action ? '' : action
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityPosts}/${post.postId}/action`,
    params: { action: value },
    showLoading: false,
  })
  if (result) post.myAction = value || null
}
const setAttendance = async (event, status) => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityEventsAction}/${event.eventId}/attendance`,
    params: { status },
    showLoading: false,
  })
  if (result) event.attendanceStatus = status || null
}
const openEventManager = async () => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  if (!canManageHub.value) return proxy.Message.warning('只有创建者或现场管理员可以管理活动')
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityManagedEvents}/${activeHub.value.hubId}/events/managed`,
    params: {},
    showLoading: false,
  })
  managedEvents.value = result?.data || []
  eventEditing.value = false
  eventManagerVisible.value = true
}
const openEventManagerFromHubManager = async () => {
  hubManagerVisible.value = false
  await nextTick()
  await openEventManager()
}
const startCreateEvent = () => {
  Object.assign(eventDraft, {
    eventId: null,
    title: '',
    description: '',
    startsAt: '',
    endsAt: '',
    venue: '',
    sourceUrl: '',
  })
  eventEditing.value = true
}
const dateTimeInputValue = (value) => (value ? String(value).replace(' ', 'T').slice(0, 16) : '')
const editEvent = (event) => {
  Object.assign(eventDraft, {
    eventId: event.eventId,
    title: event.title || '',
    description: event.description || '',
    startsAt: dateTimeInputValue(event.startsAt),
    endsAt: dateTimeInputValue(event.endsAt),
    venue: event.venue || '',
    sourceUrl: event.sourceUrl || '',
  })
  eventEditing.value = true
}
const saveEvent = async () => {
  if (!eventDraft.title.trim()) return proxy.Message.warning('请输入活动标题')
  if (!eventDraft.startsAt) return proxy.Message.warning('请选择活动开始时间')
  if (eventDraft.endsAt && new Date(eventDraft.endsAt).getTime() < new Date(eventDraft.startsAt).getTime())
    return proxy.Message.warning('结束时间不能早于开始时间')
  savingEvent.value = true
  const base = {
    title: eventDraft.title.trim(),
    description: eventDraft.description.trim(),
    startsAt: eventDraft.startsAt,
    endsAt: eventDraft.endsAt || '',
    venue: eventDraft.venue.trim(),
    sourceUrl: eventDraft.sourceUrl.trim(),
    status: 'UPCOMING',
  }
  const url = eventDraft.eventId
    ? `${proxy.Api.zentideCommunityEvents}/${eventDraft.eventId}/update`
    : `${proxy.Api.zentideCommunityManagedEvents}/${activeHub.value.hubId}/events`
  const result = await proxy.Request({ url, params: eventDraft.eventId ? base : base, showLoading: false })
  savingEvent.value = false
  if (!result) return
  eventEditing.value = false
  await openEventManager()
  await loadHubContext()
  proxy.Message.success(eventDraft.eventId ? '活动已更新' : '活动已创建')
}
const removeEvent = (event) =>
  proxy.Confirm({
    message: `确定删除活动「${event.title}」吗？`,
    okfun: async () => {
      const result = await proxy.Request({
        url: `${proxy.Api.zentideCommunityEvents}/${event.eventId}/delete`,
        params: {},
        showLoading: false,
      })
      if (result) {
        managedEvents.value = managedEvents.value.filter((item) => item.eventId !== event.eventId)
        await loadHubContext()
        proxy.Message.success('活动已删除')
      }
    },
  })
const showEventStats = async (event) => {
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityEvents}/${event.eventId}/stats`,
    params: {},
    showLoading: false,
  })
  if (result) {
    eventStats.value = result.data
    eventStatsVisible.value = true
  }
}
const pieStyle = computed(() => {
  const total = Number(eventStats.value?.totalParticipants || 0)
  if (!total) return { background: '#e5ebe7' }
  const want = (Number(eventStats.value.wantCount || 0) / total) * 360
  const attending = (Number(eventStats.value.attendingCount || 0) / total) * 360
  return {
    background: `conic-gradient(#d99a70 0deg ${want}deg,#5b9387 ${want}deg ${want + attending}deg,#526b82 ${want + attending}deg 360deg)`,
  }
})
const openComments = async (post) => {
  post.expanded = !post.expanded
  if (post.expanded && !post.comments.length) {
    const result = await proxy.Request({
      url: `${proxy.Api.zentideCommunityPosts}/${post.postId}/comments/list`,
      params: { limit: 100 },
      showLoading: false,
      showError: false,
    })
    if (result) post.comments = result.data || []
  }
}
const submitComment = async (post) => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  if (!post.commentDraft?.trim()) return
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityPosts}/${post.postId}/comments`,
    params: { body: post.commentDraft.trim() },
    showLoading: false,
  })
  if (result) {
    post.comments.push(result.data)
    post.commentCount = Number(post.commentCount || 0) + 1
    post.commentDraft = ''
  }
}
const openChange = (changeId) => router.push({ path: '/changes', query: { changeId } })
const openPost = (postId) => router.push({ name: 'community-post', params: { postId } })
const resourceUrl = (source) => (source ? `${proxy.Api.sourcePath}${encodeURIComponent(source)}` : '')
const postImages = (post) => {
  try {
    const value = JSON.parse(post.mediaJson || '[]')
    return Array.isArray(value) ? value : []
  } catch {
    return []
  }
}
const feedCover = (post) => post.coverUrl || postImages(post)[0] || ''
const postExcerpt = (body) => richTextExcerpt(body, 360)
const uploadCover = async (options) => {
  uploadingCover.value = true
  const result = await uploadImage(options.file, false)
  uploadingCover.value = false
  if (result) composer.coverUrl = result
}
const uploadHubCover = async (options) => {
  uploadingHubCover.value = true
  try {
    const result = await uploadImage(options.file, false)
    if (result) hubDraft.coverUrl = result
  } finally {
    uploadingHubCover.value = false
  }
}
const typeLabel = (value, provided) =>
  provided || postTypes.value.find((type) => type.typeCode === value)?.displayName || value || '分享'
const actionLabel = (value) =>
  ({ WANT: '想去', ATTENDED: '已去', USING: '正在用', WATCHING: '观望', RECOMMEND: '推荐' })[value] || '更多'
const attendanceLabel = (value) =>
  ({ WANT: '想参加', ATTENDING: '参加中', ATTENDED: '已参加' })[value] || '参与'
const formatTime = (value) =>
  value ? new Date(value).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }) : '刚刚'
const formatFullTime = (value) =>
  value
    ? new Date(value).toLocaleString('zh-CN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    : '刚刚'
const eventDay = (value) => (value ? new Date(value).toLocaleDateString('zh-CN', { day: 'numeric' }) : '--')
const eventMonth = (value) =>
  value ? new Date(value).toLocaleDateString('zh-CN', { month: 'short' }) : '待定'
watch(
  () => composer.hubId,
  async (hubId, oldHubId) => {
    if (!composerVisible.value || hubId === oldHubId) return
    composer.topicId = null
    composer.eventId = null
    creatingTopic.value = false
    newTopicName.value = ''
    const [eventList, topicList, typeList] = await Promise.all([
      requestEvents(hubId),
      requestTopics(hubId),
      requestPostTypes(hubId),
    ])
    composerEvents.value = eventList
    composerTopics.value = topicList
    composerPostTypes.value = typeList
    composer.type = typeList[0]?.typeCode || ''
  },
)
onMounted(async () => {
  await Promise.all([loadHubs(), loadCategories(), loadHubDirections()])
  const hubId = Number(route.query.hubId)
  if (hubId && hubs.value.some((hub) => Number(hub.hubId) === hubId)) selectedHub.value = hubId
  const topicId = Number(route.query.topicId)
  const changeId = Number(route.query.changeId)
  if (changeId) {
    const result = await proxy.Request({
      url: `${proxy.Api.zentideCommunityChangeContext}${changeId}/context`,
      params: {},
      showLoading: false,
      showError: false,
    })
    changeContext.value =
      result?.data?.find(
        (item) => item.hubId === selectedHub.value && (!topicId || item.topicId === topicId),
      ) ||
      result?.data?.[0] ||
      null
  }
  await Promise.all([loadFeed(), loadHubContext()])
  if (route.query.createHub === '1') showHubComposer()
  else if (route.query.editPostId) await openEditComposer(route.query.editPostId)
  else if (route.query.compose === '1') {
    const requestedContext = changeContext.value
    await openComposer()
    composer.topicId = topicId || null
    composer.changeId = changeId || null
    changeContext.value = requestedContext
  }
})
</script>

<style src="./community-discuit-modern.scss"></style>
