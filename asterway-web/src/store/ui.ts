import { reactive } from 'vue'
type ModalKind = 'goal' | 'task' | 'habit' | 'goalDetail' | 'focus' | 'search' | 'confirm'
export const ui = reactive({
  modal: null as null | {
    kind: ModalKind
    id?: string
    date?: string
    goalId?: string
    milestoneId?: string
  },
  confirmTitle: '',
  confirmBody: '',
  confirmAction: null as null | (() => void),
  open(
    kind: ModalKind,
    options: { id?: string; date?: string; goalId?: string; milestoneId?: string } = {},
  ) {
    this.modal = { kind, ...options }
  },
  close() {
    this.modal = null
  },
  confirm(title: string, body: string, action: () => void) {
    this.confirmTitle = title
    this.confirmBody = body
    this.confirmAction = action
    this.modal = { kind: 'confirm' }
  },
})
