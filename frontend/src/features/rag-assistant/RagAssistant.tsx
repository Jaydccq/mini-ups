import { useCallback, useMemo, useState } from 'react'
import { Loader2, MessageCircle, Sparkles, Send, ThumbsUp, ThumbsDown } from 'lucide-react'
import { useAuthStore } from '@/stores/auth-store'
import { queryRag, submitRagFeedback, type RagQueryResult, type RagSourceResponse } from '@/services/rag'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Badge } from '@/components/ui/badge'
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet'
import { cn } from '@/lib/utils'
import { toast } from 'sonner'

interface RagAssistantMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  createdAt: Date
  sources?: RagSourceResponse[]
  warnings?: string[]
  logId?: string
  feedback?: 'positive' | 'negative'
  feedbackSubmitting?: boolean
}

const SUGGESTED_QUERIES = [
  '如何处理延迟配送？',
  '司机签到流程是什么？',
  '如何同步世界模拟器状态？',
]

const createId = () => {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return Math.random().toString(36).slice(2)
}

export const RagAssistant = () => {
  const { user } = useAuthStore()
  const [open, setOpen] = useState(false)
  const [input, setInput] = useState('')
  const [messages, setMessages] = useState<RagAssistantMessage[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = useCallback(async () => {
    const query = input.trim()
    if (!query) return

    const userMessage: RagAssistantMessage = {
      id: createId(),
      role: 'user',
      content: query,
      createdAt: new Date(),
    }

    setMessages((prev) => [...prev, userMessage])
    setInput('')
    setError(null)
    setLoading(true)

    try {
      const payload = {
        query,
        context: {
          role: user?.role,
        },
      }
      const result: RagQueryResult = await queryRag(payload)

      const assistantMessage: RagAssistantMessage = {
        id: createId(),
        role: 'assistant',
        content: result.answer,
        createdAt: new Date(),
        sources: result.sources,
        warnings: result.warnings,
        logId: result.logId,
      }
      setMessages((prev) => [...prev, assistantMessage])
    } catch (err) {
      console.error('RAG query failed', err)
      setError('助手暂时不可用，请稍后再试。')
      const fallbackMessage: RagAssistantMessage = {
        id: createId(),
        role: 'assistant',
        content: '抱歉，当前无法获取答案，请稍后重试。',
        createdAt: new Date(),
      }
      setMessages((prev) => [...prev, fallbackMessage])
    } finally {
      setLoading(false)
    }
  }, [input, user?.role])

  const handleQuickInsert = useCallback((text: string) => {
    setInput(text)
  }, [])

  const hasMessages = useMemo(() => messages.length > 0, [messages])

  const handleFeedback = useCallback(
    async (messageId: string, type: 'positive' | 'negative') => {
      const target = messages.find((message) => message.id === messageId)
      if (!target || !target.logId) {
        toast.error('无法提交反馈，缺少日志信息')
        return
      }
      if (target.feedback === type) {
        return
      }

      setMessages((prev) =>
        prev.map((message) =>
          message.id === messageId
            ? { ...message, feedbackSubmitting: true }
            : message
        )
      )

      try {
        await submitRagFeedback({
          logId: target.logId,
          feedback: type === 'positive' ? 'POSITIVE' : 'NEGATIVE',
        })
        setMessages((prev) =>
          prev.map((message) =>
            message.id === messageId
              ? {
                  ...message,
                  feedback: type,
                  feedbackSubmitting: false,
                }
              : message
          )
        )
        toast.success(type === 'positive' ? '感谢你的好评！' : '我们已记录你的反馈')
      } catch (err) {
        console.error('提交反馈失败', err)
        setMessages((prev) =>
          prev.map((message) =>
            message.id === messageId
              ? { ...message, feedbackSubmitting: false }
              : message
          )
        )
        toast.error('提交反馈失败，请稍后再试')
      }
    },
    [messages]
  )

  return (
    <div className="fixed bottom-6 right-6 z-50">
      <Sheet open={open} onOpenChange={(value) => setOpen(value)}>
        <SheetTrigger asChild>
          <Button className="h-12 w-12 rounded-full shadow-lg" size="icon" variant="default">
            <Sparkles className="h-5 w-5" />
            <span className="sr-only">打开智能助手</span>
          </Button>
        </SheetTrigger>
        <SheetContent
          side="right"
          className="flex h-full w-full flex-col gap-4 bg-white/95 p-0 sm:max-w-lg"
        >
          <div className="border-b px-6 py-4">
            <SheetHeader className="gap-1 text-left">
              <SheetTitle className="flex items-center gap-2 text-lg">
                <MessageCircle className="h-5 w-5 text-primary" />
                Mini-UPS 智能助手
              </SheetTitle>
              <SheetDescription>
                获取调度、司机和系统配置相关的即时答案，并附带来源引用。
              </SheetDescription>
            </SheetHeader>
          </div>

          <div className="flex flex-1 flex-col gap-4 px-6 pb-6">
            <ScrollArea className="flex-1 pr-4">
              {!hasMessages && (
                <div className="mt-8 space-y-4 text-sm text-muted-foreground">
                  <p className="font-medium text-foreground">试试以下问题：</p>
                  <div className="flex flex-wrap gap-2">
                    {SUGGESTED_QUERIES.map((suggestion) => (
                      <Button
                        key={suggestion}
                        variant="secondary"
                        size="sm"
                        className="border border-dashed border-primary/40 text-primary"
                        onClick={() => handleQuickInsert(suggestion)}
                      >
                        {suggestion}
                      </Button>
                    ))}
                  </div>
                </div>
              )}

              <div className="space-y-4">
                {messages.map((message) => (
                  <div
                    key={message.id}
                    className={cn(
                      'rounded-lg border px-4 py-3 text-sm shadow-sm transition',
                      message.role === 'user'
                        ? 'ml-8 border-primary/30 bg-primary/5'
                        : 'mr-8 border-slate-200 bg-white'
                    )}
                  >
                    <div className="mb-2 text-xs uppercase tracking-wide text-muted-foreground">
                      {message.role === 'user' ? '你' : '智能助手'}
                    </div>
                    <div className="whitespace-pre-wrap leading-relaxed text-gray-800">
                      {message.content}
                    </div>
                    {message.warnings && message.warnings.length > 0 && (
                      <div className="mt-3 space-y-1">
                        {message.warnings.map((warning) => (
                          <div
                            key={warning}
                            className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-700"
                          >
                            {warning}
                          </div>
                        ))}
                      </div>
                    )}
                    {message.sources && message.sources.length > 0 && (
                      <div className="mt-4 space-y-3 border-t pt-3">
                        <p className="text-xs font-semibold uppercase text-muted-foreground">
                          参考资料
                        </p>
                        <ul className="space-y-2">
                          {message.sources.map((source, index) => (
                            <li key={`${message.id}-source-${index}`} className="flex gap-2">
                              <Badge variant="outline" className="h-5 text-xs">
                                [{index + 1}]
                              </Badge>
                              <div className="text-xs text-muted-foreground">
                                <div className="font-medium text-foreground">{source.title}</div>
                                <div className="truncate text-[11px]">{source.source}</div>
                                <div className="text-[10px] text-emerald-600">
                                  相似度 {(source.similarity * 100).toFixed(0)}%
                                </div>
                                {(source.semanticScore !== undefined || source.keywordScore !== undefined) && (
                                  <div className="text-[10px] text-muted-foreground">
                                    语义 {(Math.min(1, Math.max(0, source.semanticScore ?? 0)) * 100).toFixed(0)}% · 关键词 {(Math.min(1, Math.max(0, source.keywordScore ?? 0)) * 100).toFixed(0)}%
                                  </div>
                                )}
                              </div>
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {message.role === 'assistant' && message.logId && (
                      <div className="mt-4 flex items-center gap-3 text-xs text-muted-foreground">
                        <span>这条回复有帮助吗？</span>
                        <div className="flex items-center gap-1">
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className={cn(
                              'h-7 w-7',
                              message.feedback === 'positive' ? 'text-emerald-600' : 'text-muted-foreground'
                            )}
                            disabled={message.feedbackSubmitting}
                            onClick={() => handleFeedback(message.id, 'positive')}
                          >
                            <ThumbsUp className="h-4 w-4" />
                          </Button>
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className={cn(
                              'h-7 w-7',
                              message.feedback === 'negative' ? 'text-rose-600' : 'text-muted-foreground'
                            )}
                            disabled={message.feedbackSubmitting}
                            onClick={() => handleFeedback(message.id, 'negative')}
                          >
                            <ThumbsDown className="h-4 w-4" />
                          </Button>
                        </div>
                        {message.feedback && (
                          <span className="text-[11px] text-emerald-600">
                            已记录
                          </span>
                        )}
                      </div>
                    )}
                  </div>
                ))}
              </div>

              {loading && (
                <div className="mt-4 flex items-center gap-2 text-sm text-muted-foreground">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  正在生成回答...
                </div>
              )}
            </ScrollArea>

            {error && (
              <div className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
                {error}
              </div>
            )}

            <form
              onSubmit={(event) => {
                event.preventDefault()
                if (!loading) {
                  handleSubmit()
                }
              }}
              className="space-y-2"
            >
              <Textarea
                placeholder="输入问题，例如：如何处理延迟配送？"
                value={input}
                onChange={(event) => setInput(event.target.value)}
                disabled={loading}
                rows={3}
              />
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted-foreground">
                  建议避免输入敏感信息。
                </span>
                <Button type="submit" disabled={loading || !input.trim()}>
                  {loading ? (
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  ) : (
                    <Send className="mr-2 h-4 w-4" />
                  )}
                  {loading ? '生成中' : '发送'}
                </Button>
              </div>
            </form>
          </div>
        </SheetContent>
      </Sheet>
    </div>
  )
}
