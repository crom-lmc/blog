import { h } from 'vue'
import { Modal } from 'ant-design-vue'

/** 统一的二次确认对话框，视觉与交互对齐 CMS/EHR。 */
export function confirmAction(opts: {
  title: string
  content: string
  okText?: string
  okType?: 'primary' | 'danger'
  onOk: () => void | Promise<void>
}) {
  Modal.confirm({
    title: opts.title,
    content: opts.content,
    okText: opts.okText ?? '确定',
    okType: opts.okType ?? 'primary',
    cancelText: '取消',
    centered: true,
    onOk: opts.onOk
  })
}

/** 需要用户输入文本的确认框（评论回复等）。返回 null 表示取消。 */
export function promptAction(opts: {
  title: string
  placeholder?: string
  defaultValue?: string
}): Promise<string | null> {
  return new Promise((resolve) => {
    let value = opts.defaultValue || ''
    const modal = Modal.confirm({
      title: opts.title,
      centered: true,
      okText: '确定',
      cancelText: '取消',
      content: () => h('textarea', {
        rows: 4,
        placeholder: opts.placeholder || '',
        value,
        style: 'width:100%;padding:8px;border:1px solid #d9d9d9;border-radius:6px;resize:vertical',
        onInput: (e: Event) => { value = (e.target as HTMLTextAreaElement).value }
      }),
      onOk: () => resolve(value.trim() || null),
      onCancel: () => resolve(null)
    })
    void modal
  })
}
