<template>
  <div class="rich-editor" :class="{ focused, invalid: characterCount > maxLength }">
    <div v-if="editor" class="editor-toolbar" role="toolbar" aria-label="正文格式工具栏">
      <div class="toolbar-group text-styles">
        <button
          type="button"
          :class="{ active: editor.isActive('paragraph') }"
          title="正文"
          @click="editor.chain().focus().setParagraph().run()"
        >
          正文
        </button>
        <button
          type="button"
          :class="{ active: editor.isActive('heading', { level: 2 }) }"
          title="二级标题"
          @click="editor.chain().focus().toggleHeading({ level: 2 }).run()"
        >
          标题
        </button>
        <button
          type="button"
          :class="{ active: editor.isActive('heading', { level: 3 }) }"
          title="三级标题"
          @click="editor.chain().focus().toggleHeading({ level: 3 }).run()"
        >
          小标题
        </button>
      </div>

      <div class="toolbar-separator"></div>
      <div class="toolbar-group">
        <button
          type="button"
          :class="{ active: editor.isActive('bold') }"
          title="加粗"
          aria-label="加粗"
          @click="editor.chain().focus().toggleBold().run()"
        >
          <strong>B</strong>
        </button>
        <button
          type="button"
          :class="{ active: editor.isActive('italic') }"
          title="斜体"
          aria-label="斜体"
          @click="editor.chain().focus().toggleItalic().run()"
        >
          <em>I</em>
        </button>
        <button
          type="button"
          :class="{ active: editor.isActive('underline') }"
          title="下划线"
          aria-label="下划线"
          @click="editor.chain().focus().toggleUnderline().run()"
        >
          <u>U</u>
        </button>
        <button
          type="button"
          :class="{ active: editor.isActive('strike') }"
          title="删除线"
          aria-label="删除线"
          @click="editor.chain().focus().toggleStrike().run()"
        >
          <s>S</s>
        </button>
      </div>

      <div class="toolbar-separator"></div>
      <div class="toolbar-group">
        <button
          type="button"
          :class="{ active: editor.isActive('bulletList') }"
          title="无序列表"
          aria-label="无序列表"
          @click="editor.chain().focus().toggleBulletList().run()"
        >
          <svg viewBox="0 0 24 24">
            <circle cx="5" cy="7" r="1" />
            <circle cx="5" cy="12" r="1" />
            <circle cx="5" cy="17" r="1" />
            <path d="M9 7h10M9 12h10M9 17h10" />
          </svg>
        </button>
        <button
          type="button"
          :class="{ active: editor.isActive('orderedList') }"
          title="有序列表"
          aria-label="有序列表"
          @click="editor.chain().focus().toggleOrderedList().run()"
        >
          <svg viewBox="0 0 24 24">
            <path d="M4 6h2v4M4 10h3M4 14c3-1 3 3 0 3h3M10 7h9M10 12h9M10 17h9" />
          </svg>
        </button>
        <button
          type="button"
          :class="{ active: editor.isActive('blockquote') }"
          title="引用"
          aria-label="引用"
          @click="editor.chain().focus().toggleBlockquote().run()"
        >
          <svg viewBox="0 0 24 24">
            <path d="M5 10h5v7H4v-5c0-3 1.3-5 4-6M15 10h5v7h-6v-5c0-3 1.3-5 4-6" />
          </svg>
        </button>
        <button
          type="button"
          :class="{ active: editor.isActive('code') }"
          title="行内代码"
          aria-label="行内代码"
          @click="editor.chain().focus().toggleCode().run()"
        >
          <span class="code-mark">&lt;/&gt;</span>
        </button>
        <button
          type="button"
          :class="{ active: editor.isActive('codeBlock') }"
          title="代码块"
          aria-label="代码块"
          @click="editor.chain().focus().toggleCodeBlock().run()"
        >
          <svg viewBox="0 0 24 24"><path d="m8 8-4 4 4 4M16 8l4 4-4 4M14 5l-4 14" /></svg>
        </button>
        <button
          type="button"
          :class="{ active: editor.isActive('link') }"
          title="添加链接"
          aria-label="添加链接"
          @click="editLink"
        >
          <svg viewBox="0 0 24 24">
            <path
              d="M10 13a4 4 0 0 0 5.7 0l2.3-2.3A4 4 0 0 0 12.3 5L11 6.3M14 11a4 4 0 0 0-5.7 0L6 13.3A4 4 0 0 0 11.7 19l1.3-1.3"
            />
          </svg>
        </button>
      </div>

      <div class="toolbar-separator"></div>
      <div class="toolbar-group media-tools">
        <button
          type="button"
          :disabled="uploadingMedia"
          title="在正文插入图片"
          aria-label="插入图片"
          @click="imageInput?.click()"
        >
          <svg viewBox="0 0 24 24">
            <rect x="3.5" y="4" width="17" height="16" rx="2" />
            <circle cx="9" cy="9" r="1.5" />
            <path d="m5.5 17 4.2-4.2 3.1 3 2.2-2.1 3.5 3.3" />
          </svg>
        </button>
        <button
          type="button"
          :disabled="uploadingMedia"
          title="在正文插入视频"
          aria-label="插入视频"
          @click="videoInput?.click()"
        >
          <svg viewBox="0 0 24 24">
            <rect x="3.5" y="5" width="17" height="14" rx="2" />
            <path d="m10 9 5 3-5 3Z" />
          </svg>
        </button>
        <span v-if="uploadingMedia" class="upload-state">上传中…</span>
        <input
          ref="imageInput"
          class="media-input"
          type="file"
          accept="image/jpeg,image/png,image/gif,image/webp,image/avif"
          @change="insertUploadedMedia('image', $event)"
        />
        <input
          ref="videoInput"
          class="media-input"
          type="file"
          accept="video/mp4,video/webm,video/quicktime"
          @change="insertUploadedMedia('video', $event)"
        />
      </div>

      <div class="toolbar-spacer"></div>
      <div class="toolbar-group history">
        <button
          type="button"
          :disabled="!editor.can().undo()"
          title="撤销"
          aria-label="撤销"
          @click="editor.chain().focus().undo().run()"
        >
          <svg viewBox="0 0 24 24"><path d="m9 8-4 4 4 4M5 12h8a6 6 0 0 1 6 6" /></svg>
        </button>
        <button
          type="button"
          :disabled="!editor.can().redo()"
          title="重做"
          aria-label="重做"
          @click="editor.chain().focus().redo().run()"
        >
          <svg viewBox="0 0 24 24"><path d="m15 8 4 4-4 4M19 12h-8a6 6 0 0 0-6 6" /></svg>
        </button>
      </div>
    </div>

    <EditorContent :editor="editor" class="editor-content" />
    <footer class="editor-footer">
      <span>支持排版、链接以及在正文中插入图片和视频</span
      ><b :class="{ over: characterCount > maxLength }">{{ characterCount }} / {{ maxLength }}</b>
    </footer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import { mergeAttributes, Node } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import Underline from '@tiptap/extension-underline'
import { uploadPostMedia } from '@/utils/Api.js'
import Message from '@/utils/Message.js'

const Video = Node.create({
  name: 'video',
  group: 'block',
  atom: true,
  selectable: true,
  addAttributes: () => ({ src: { default: null } }),
  parseHTML: () => [{ tag: 'video[src]' }],
  renderHTML: ({ HTMLAttributes }) => [
    'video',
    mergeAttributes(HTMLAttributes, { controls: '', preload: 'metadata', playsinline: '' }),
  ],
})

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '写下你的发现、体验或问题…' },
  maxLength: { type: Number, default: 8000 },
})
const emit = defineEmits(['update:modelValue'])
const focused = ref(false)
const uploadingMedia = ref(false)
const imageInput = ref(null)
const videoInput = ref(null)

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit.configure({ link: false, underline: false, heading: { levels: [2, 3] } }),
    Link.configure({
      openOnClick: false,
      autolink: true,
      defaultProtocol: 'https',
      protocols: ['http', 'https'],
      HTMLAttributes: { target: '_blank', rel: 'nofollow noopener noreferrer' },
    }),
    Underline,
    Image.configure({ inline: false, allowBase64: false }),
    Video,
    Placeholder.configure({ placeholder: props.placeholder }),
  ],
  editorProps: {
    attributes: { class: 'rich-editor-surface', 'aria-label': '帖子正文' },
  },
  onUpdate: ({ editor: instance }) => emit('update:modelValue', instance.isEmpty ? '' : instance.getHTML()),
  onFocus: () => {
    focused.value = true
  },
  onBlur: () => {
    focused.value = false
  },
})

const characterCount = computed(() => editor.value?.state.doc.textContent.length || 0)

const editLink = () => {
  if (!editor.value) return
  const previous = editor.value.getAttributes('link').href || ''
  const requested = window.prompt('输入链接地址（http:// 或 https://）', previous)
  if (requested === null) return
  const href = requested.trim()
  if (!href) return editor.value.chain().focus().extendMarkRange('link').unsetLink().run()
  const normalized = /^https?:\/\//i.test(href) ? href : `https://${href}`
  editor.value.chain().focus().extendMarkRange('link').setLink({ href: normalized }).run()
}

const insertUploadedMedia = async (mediaType, event) => {
  const input = event.target
  const file = input.files?.[0]
  if (!file || !editor.value) return
  const maxBytes = mediaType === 'video' ? 100 * 1024 * 1024 : 15 * 1024 * 1024
  if (file.size > maxBytes) {
    input.value = ''
    return Message.warning(mediaType === 'video' ? '正文视频不能超过 100MB' : '正文图片不能超过 15MB')
  }
  uploadingMedia.value = true
  try {
    const storedName = await uploadPostMedia(file, mediaType)
    if (!storedName) return
    const src = `/api/file/getResource?sourceName=${encodeURIComponent(storedName)}`
    if (mediaType === 'image') editor.value.chain().focus().setImage({ src, alt: file.name }).run()
    else editor.value.chain().focus().insertContent({ type: 'video', attrs: { src } }).run()
    Message.success(mediaType === 'image' ? '图片已插入正文' : '视频已插入正文')
  } finally {
    uploadingMedia.value = false
    input.value = ''
  }
}

watch(
  () => props.modelValue,
  (value) => {
    if (!editor.value) return
    const next = value || ''
    if (editor.value.getHTML() !== next) editor.value.commands.setContent(next, { emitUpdate: false })
  },
)

onBeforeUnmount(() => editor.value?.destroy())
</script>

<style scoped>
.rich-editor {
  overflow: hidden;
  width: 100%;
  border: 1px solid var(--zt-border);
  border-radius: var(--zt-radius-md);
  background: var(--zt-surface);
  transition:
    border-color 0.15s,
    box-shadow 0.15s;
}
.rich-editor.focused {
  border-color: var(--zt-primary);
  box-shadow: 0 0 0 3px rgba(23, 63, 52, 0.07);
}
.rich-editor.invalid {
  border-color: var(--zt-danger);
}
.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 5px;
  min-height: 45px;
  padding: 6px 8px;
  border-bottom: 1px solid var(--zt-border-soft);
  background: var(--zt-surface-hover);
}
.toolbar-group {
  display: flex;
  align-items: center;
  gap: 2px;
}
.toolbar-group button {
  display: grid;
  place-items: center;
  min-width: 30px;
  height: 30px;
  padding: 0 7px;
  border: 0;
  border-radius: 6px;
  color: var(--zt-text-2);
  background: transparent;
  font-size: 11px;
  cursor: pointer;
}
.toolbar-group button:hover:not(:disabled) {
  color: var(--zt-text);
  background: var(--zt-surface-soft);
}
.toolbar-group button.active {
  color: var(--zt-primary);
  background: var(--zt-primary-soft);
}
.toolbar-group button:disabled {
  opacity: 0.32;
  cursor: default;
}
.toolbar-group svg {
  width: 17px;
  height: 17px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.7;
}
.code-mark {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 10px;
}
.text-styles button {
  min-width: auto;
  padding: 0 9px;
}
.toolbar-separator {
  width: 1px;
  height: 20px;
  margin: 0 2px;
  background: var(--zt-border);
}
.toolbar-spacer {
  flex: 1;
}
.media-input {
  display: none;
}
.upload-state {
  padding: 0 5px;
  color: var(--zt-text-3);
  font-size: 10px;
  white-space: nowrap;
}
.editor-content {
  min-height: 220px;
}
.editor-footer {
  display: flex;
  justify-content: space-between;
  padding: 8px 11px;
  border-top: 1px solid var(--zt-border-soft);
  color: var(--zt-text-3);
  background: var(--zt-surface-hover);
  font-size: 10px;
}
.editor-footer b {
  color: var(--zt-text-2);
  font-weight: 600;
}
.editor-footer b.over {
  color: var(--zt-danger);
}
:deep(.rich-editor-surface) {
  min-height: 220px;
  padding: 16px 18px;
  color: var(--zt-text);
  font-size: 14px;
  line-height: 1.75;
  outline: 0;
  overflow-wrap: anywhere;
}
:deep(.rich-editor-surface p) {
  margin: 0 0 10px;
}
:deep(.rich-editor-surface p:last-child) {
  margin-bottom: 0;
}
:deep(.rich-editor-surface h2) {
  margin: 19px 0 8px;
  font-size: 20px;
  line-height: 1.4;
}
:deep(.rich-editor-surface h3) {
  margin: 16px 0 7px;
  font-size: 16px;
  line-height: 1.45;
}
:deep(.rich-editor-surface ul),
:deep(.rich-editor-surface ol) {
  margin: 8px 0 12px;
  padding-left: 25px;
}
:deep(.rich-editor-surface blockquote) {
  margin: 12px 0;
  padding: 4px 0 4px 13px;
  border-left: 3px solid #9eaba5;
  color: var(--zt-text-2);
}
:deep(.rich-editor-surface code) {
  padding: 2px 5px;
  border-radius: 4px;
  background: var(--zt-surface-soft);
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 0.9em;
}
:deep(.rich-editor-surface pre) {
  overflow: auto;
  margin: 12px 0;
  padding: 13px 15px;
  border-radius: 8px;
  color: #e7e9ed;
  background: #25282e;
}
:deep(.rich-editor-surface pre code) {
  padding: 0;
  background: transparent;
}
:deep(.rich-editor-surface a) {
  color: var(--zt-primary);
  text-decoration: underline;
  text-underline-offset: 2px;
}
:deep(.rich-editor-surface img),
:deep(.rich-editor-surface video) {
  display: block;
  width: auto;
  max-width: 100%;
  max-height: 520px;
  margin: 15px auto;
  border-radius: 7px;
  background: var(--zt-surface-soft);
}
:deep(.rich-editor-surface video) {
  width: 100%;
  background: #171a19;
}
:deep(.rich-editor-surface img.ProseMirror-selectednode),
:deep(.rich-editor-surface video.ProseMirror-selectednode) {
  outline: 3px solid rgba(23, 63, 52, 0.2);
}
:deep(.rich-editor-surface .is-editor-empty:first-child::before) {
  float: left;
  height: 0;
  color: var(--zt-text-3);
  content: attr(data-placeholder);
  pointer-events: none;
}
@media (max-width: 680px) {
  .editor-toolbar {
    align-items: flex-start;
    flex-wrap: wrap;
  }
  .toolbar-spacer {
    display: none;
  }
  .history {
    margin-left: auto;
  }
  .toolbar-separator {
    display: none;
  }
  .editor-content,
  :deep(.rich-editor-surface) {
    min-height: 180px;
  }
}
</style>
