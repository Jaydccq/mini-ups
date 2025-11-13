import { useCallback, useMemo, useState, useRef, useEffect } from 'react'
import { Loader2, MessageCircle, Sparkles, Send, ThumbsUp, ThumbsDown, User, LogIn, Zap, Bot, Copy, RotateCcw } from 'lucide-react'
import { useAuthStore } from '@/stores/auth-store'
import { queryRag, submitRagFeedback, type RagQueryResult, type RagSourceResponse } from '@/services/rag'
import { aiService } from '@/services/ai'
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
  isPreview?: boolean
  suggestions?: string[]
  isEnhanced?: boolean
}

const SUGGESTED_QUERIES = [
  'How to sync world simulator status?',
  'How to handle delayed deliveries?',
  'Driver check-in process?',
  'Truck management guide',
  'What is my delivery time?',
  'Shipment tracking help'
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
  const scrollRef = useRef<HTMLDivElement>(null)
  const [isNearBottom, setIsNearBottom] = useState(true)

  // Auto-scroll to bottom when new messages arrive
  const scrollToBottom = useCallback((behavior: 'auto' | 'smooth' = 'smooth') => {
    if (scrollRef.current) {
      scrollRef.current.scrollTo({
        top: scrollRef.current.scrollHeight,
        behavior
      })
    }
  }, [])

  // Check if user is near bottom for smart auto-scroll
  const handleScroll = useCallback(() => {
    if (scrollRef.current) {
      const { scrollTop, scrollHeight, clientHeight } = scrollRef.current
      const isNear = scrollHeight - scrollTop - clientHeight < 100
      setIsNearBottom(isNear)
    }
  }, [])

  // Auto-scroll when messages change (only if user is near bottom)
  useEffect(() => {
    if (isNearBottom && messages.length > 0) {
      scrollToBottom()
    }
  }, [messages, isNearBottom, scrollToBottom])

  // Copy message content to clipboard
  const copyToClipboard = useCallback(async (content: string) => {
    try {
      await navigator.clipboard.writeText(content)
      toast.success('Message copied to clipboard')
    } catch (err) {
      toast.error('Failed to copy message')
    }
  }, [])

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
      if (!user) {
        // Guest user - use AI service for intelligent responses
        const aiResult = await aiService.queryAI(query)

        const assistantMessage: RagAssistantMessage = {
          id: createId(),
          role: 'assistant',
          content: aiResult.answer,
          createdAt: new Date(),
          isPreview: aiResult.isPreview,
          suggestions: aiResult.suggestions,
        }
        setMessages((prev) => [...prev, assistantMessage])
      } else {
        // Authenticated user - use AI-enhanced RAG processing
        // Step 1: Get RAG data from backend
        const payload = {
          query,
          context: {
            role: user.role,
          },
        }
        const ragResult: RagQueryResult = await queryRag(payload)

        // Step 2: Enhance RAG response with AI processing
        const enhancedResult = await aiService.enhanceRAGResponse(query, ragResult)

        const assistantMessage: RagAssistantMessage = {
          id: createId(),
          role: 'assistant',
          content: enhancedResult.answer,
          createdAt: new Date(),
          sources: enhancedResult.sources,
          warnings: enhancedResult.warnings,
          logId: enhancedResult.logId,
          isPreview: false,
          isEnhanced: enhancedResult.isEnhanced,
        }
        setMessages((prev) => [...prev, assistantMessage])
      }
    } catch (err) {
      console.error('Query failed', err)
      setError('Assistant is temporarily unavailable, please try again later.')
      const fallbackMessage: RagAssistantMessage = {
        id: createId(),
        role: 'assistant',
        content: 'Sorry, unable to get an answer at the moment, please try again later.',
        createdAt: new Date(),
        isPreview: !user,
      }
      setMessages((prev) => [...prev, fallbackMessage])
    } finally {
      setLoading(false)
    }
  }, [input, user])

  const handleQuickInsert = useCallback((text: string) => {
    setInput(text)
  }, [])

  const hasMessages = useMemo(() => messages.length > 0, [messages])

  const handleFeedback = useCallback(
    async (messageId: string, type: 'positive' | 'negative') => {
      const target = messages.find((message) => message.id === messageId)
      if (!target || !target.logId) {
        toast.error('Unable to submit feedback, missing log information')
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
        toast.success(type === 'positive' ? 'Thanks for your positive feedback!' : 'We have recorded your feedback')
      } catch (err) {
        console.error('提交反馈失败', err)
        setMessages((prev) =>
          prev.map((message) =>
            message.id === messageId
              ? { ...message, feedbackSubmitting: false }
              : message
          )
        )
        toast.error('Failed to submit feedback, please try again later')
      }
    },
    [messages]
  )

  return (
    <div className="fixed bottom-6 right-6 z-[9999]">
        <Sheet open={open} onOpenChange={(value) => setOpen(value)}>
        <SheetTrigger asChild>
          <Button
            className={cn(
              "h-14 w-14 rounded-full shadow-2xl relative overflow-hidden",
              "bg-gradient-to-br from-primary via-blue-600 to-purple-600",
              "hover:shadow-3xl hover:scale-105 transition-all duration-300",
              "border-2 border-white/20"
            )}
            size="icon"
          >
            <div className="absolute inset-0 bg-gradient-to-br from-white/20 to-transparent" />
            <Sparkles className="h-6 w-6 text-white relative z-10" />
            {!user && (
              <>
                <div className="absolute -top-1 -right-1 h-4 w-4 bg-orange-500 rounded-full animate-pulse border-2 border-white shadow-lg" />
                <div className="absolute -top-1 -right-1 h-4 w-4 bg-orange-400 rounded-full animate-ping" />
              </>
            )}
            {user && (
              <div className="absolute -top-1 -right-1 h-4 w-4 bg-green-500 rounded-full border-2 border-white shadow-lg">
                <div className="absolute inset-0.5 bg-green-400 rounded-full animate-pulse" />
              </div>
            )}
            <span className="sr-only">Open AI Assistant</span>
          </Button>
        </SheetTrigger>
        <SheetContent
          side="right"
          className="flex h-full w-full flex-col gap-0 bg-gradient-to-br from-white/95 to-gray-50/95 backdrop-blur-xl border-l border-white/20 p-0 sm:max-w-lg z-[9999] shadow-2xl"
        >
          <div className="border-b border-white/20 px-6 py-4 bg-gradient-to-r from-primary/5 to-blue-500/5">
            <SheetHeader className="gap-1 text-left">
              <SheetTitle className="flex items-center gap-2 text-xl font-semibold">
                <div className="p-2 rounded-full bg-gradient-to-br from-primary to-blue-600">
                  <MessageCircle className="h-5 w-5 text-white" />
                </div>
                Mini-UPS AI Assistant
                {!user && (
                  <Badge variant="secondary" className="ml-2 text-xs bg-orange-100 text-orange-700 border-orange-200">
                    Preview Mode
                  </Badge>
                )}
                {user && (
                  <Badge variant="default" className="ml-2 text-xs bg-gradient-to-r from-green-500 to-emerald-600">
                    AI Enhanced
                  </Badge>
                )}
              </SheetTitle>
              <SheetDescription className="text-gray-600 leading-relaxed">
                {user
                  ? "Get AI-enhanced answers with live data from your account, source citations, and intelligent insights."
                  : "Get helpful AI-powered answers about Mini-UPS services. Sign in for personalized assistance and live data access."
                }
              </SheetDescription>
            </SheetHeader>
          </div>

          <div className="flex flex-1 flex-col overflow-hidden">
            <div
              ref={scrollRef}
              onScroll={handleScroll}
              className="flex-1 overflow-y-auto px-6 py-4 space-y-4 scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-transparent"
            >
              {!hasMessages && (
                <div className="mt-12 space-y-6 text-sm">
                  {!user && (
                    <div className="text-center space-y-6 mb-8">
                      <div className="flex items-center justify-center">
                        <div className="relative">
                          <div className="w-16 h-16 bg-gradient-to-br from-primary to-blue-600 rounded-full flex items-center justify-center shadow-lg">
                            <Bot className="h-8 w-8 text-white" />
                          </div>
                          <div className="absolute -top-1 -right-1 w-5 h-5 bg-green-500 rounded-full border-2 border-white animate-pulse"></div>
                        </div>
                      </div>
                      <div className="space-y-2">
                        <div className="text-2xl font-bold bg-gradient-to-r from-gray-900 to-gray-600 bg-clip-text text-transparent">
                          Welcome to Mini-UPS AI Assistant!
                        </div>
                        <p className="text-gray-600">I can help you with shipping questions and system guidance.</p>
                      </div>
                      <div className="bg-gradient-to-br from-blue-50 to-indigo-50 p-4 rounded-xl border border-blue-100 shadow-sm">
                        <div className="flex items-center gap-2 text-blue-700 mb-3">
                          <LogIn className="h-4 w-4" />
                          <span className="font-semibold">Sign in for premium features:</span>
                        </div>
                        <div className="grid grid-cols-2 gap-2 text-blue-600 text-xs">
                          <div className="flex items-center gap-2">
                            <Sparkles className="h-3 w-3" />
                            AI-enhanced responses
                          </div>
                          <div className="flex items-center gap-2">
                            <Zap className="h-3 w-3" />
                            Live account data
                          </div>
                          <div className="flex items-center gap-2">
                            <MessageCircle className="h-3 w-3" />
                            Source citations
                          </div>
                          <div className="flex items-center gap-2">
                            <User className="h-3 w-3" />
                            Personalized insights
                          </div>
                        </div>
                      </div>
                    </div>
                  )}
                  <div className="space-y-3">
                    <p className="font-semibold text-gray-700 flex items-center gap-2">
                      <Zap className="h-4 w-4 text-amber-500" />
                      Try these quick questions:
                    </p>
                    <div className="grid grid-cols-1 gap-3">
                      {SUGGESTED_QUERIES.map((suggestion) => (
                        <Button
                          key={suggestion}
                          variant="ghost"
                          size="sm"
                          className="justify-start h-auto p-3 border border-gray-200 hover:border-primary/40 hover:bg-gradient-to-r hover:from-primary/5 hover:to-blue-500/5 transition-all duration-200 group"
                          onClick={() => handleQuickInsert(suggestion)}
                        >
                          <div className="flex items-center gap-3 w-full">
                            <div className="p-1.5 rounded-md bg-primary/10 group-hover:bg-primary/20 transition-colors">
                              <MessageCircle className="h-3 w-3 text-primary" />
                            </div>
                            <span className="text-gray-700 group-hover:text-gray-900 text-left text-sm">
                              {suggestion}
                            </span>
                          </div>
                        </Button>
                      ))}
                    </div>
                  </div>
                </div>
              )}

              <div className="space-y-6 pb-6">
                {messages.map((message, index) => (
                  <div
                    key={message.id}
                    className={cn(
                      'group relative animate-in slide-in-from-bottom-4 duration-300',
                      message.role === 'user' ? 'flex justify-end' : 'flex justify-start'
                    )}
                    style={{ animationDelay: `${index * 100}ms` }}
                  >
                    <div
                      className={cn(
                        'max-w-[85%] rounded-2xl px-4 py-3 text-sm shadow-lg transition-all duration-200 hover:shadow-xl',
                        message.role === 'user'
                          ? 'bg-gradient-to-br from-primary to-blue-600 text-white rounded-br-md'
                          : 'bg-white border border-gray-200 text-gray-800 rounded-bl-md'
                      )}
                    >
                      <div className="mb-3 flex items-center justify-between">
                        <div className={cn(
                          "flex items-center gap-2 text-xs font-medium",
                          message.role === 'user' ? 'text-white/80' : 'text-gray-500'
                        )}>
                          {message.role === 'user' ? (
                            <>
                              <User className="h-3 w-3" />
                              You
                            </>
                          ) : (
                            <>
                              <Bot className="h-3 w-3" />
                              AI Assistant
                            </>
                          )}
                          <span className="text-[10px] opacity-60">
                            {new Date(message.createdAt).toLocaleTimeString([], {
                              hour: '2-digit',
                              minute: '2-digit'
                            })}
                          </span>
                        </div>

                        {/* Action buttons for assistant messages */}
                        {message.role === 'assistant' && (
                          <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-6 w-6 hover:bg-gray-100"
                              onClick={() => copyToClipboard(message.content)}
                            >
                              <Copy className="h-3 w-3" />
                            </Button>
                          </div>
                        )}

                        {/* Status badges */}
                        <div className="flex items-center gap-1">
                          {message.role === 'assistant' && message.isPreview && (
                            <Badge variant="secondary" className="text-xs bg-orange-100 text-orange-700 border-orange-200">
                              <Bot className="h-3 w-3 mr-1" />
                              AI Preview
                            </Badge>
                          )}
                          {message.role === 'assistant' && message.isEnhanced && !message.isPreview && (
                            <Badge variant="default" className="text-xs bg-gradient-to-r from-blue-500 to-purple-600">
                              <Sparkles className="h-3 w-3 mr-1" />
                              AI Enhanced
                            </Badge>
                          )}
                        </div>
                      </div>
                      <div className={cn(
                        "whitespace-pre-wrap leading-relaxed",
                        message.role === 'user' ? 'text-white' : 'text-gray-800'
                      )}>
                        {message.content}
                      </div>
                      {message.warnings && message.warnings.length > 0 && (
                        <div className="mt-4 space-y-2">
                          {message.warnings.map((warning) => (
                            <div
                              key={warning}
                              className="rounded-lg border border-amber-200 bg-gradient-to-r from-amber-50 to-yellow-50 px-3 py-2 text-xs text-amber-800 flex items-center gap-2"
                            >
                              <div className="w-1.5 h-1.5 rounded-full bg-amber-500 flex-shrink-0"></div>
                              {warning}
                            </div>
                          ))}
                        </div>
                      )}
                      {message.sources && message.sources.length > 0 && (
                        <div className="mt-4 space-y-3 border-t border-gray-200 pt-4">
                          <p className="text-xs font-semibold text-gray-600 flex items-center gap-2">
                            <MessageCircle className="h-3 w-3" />
                            REFERENCES
                          </p>
                          <div className="space-y-3">
                            {message.sources.map((source, index) => (
                              <div key={`${message.id}-source-${index}`} className="flex gap-3 p-2 rounded-lg bg-gray-50 border border-gray-100">
                                <Badge variant="outline" className="h-5 w-6 text-xs flex-shrink-0 justify-center">
                                  {index + 1}
                                </Badge>
                                <div className="text-xs text-gray-600 flex-1 min-w-0">
                                  <div className="font-semibold text-gray-800 mb-1">{source.title}</div>
                                  <div className="text-[11px] text-gray-500 truncate mb-2">{source.source}</div>
                                  <div className="flex items-center gap-3 text-[10px]">
                                    <div className="flex items-center gap-1">
                                      <div className="w-2 h-2 rounded-full bg-emerald-500"></div>
                                      <span className="text-emerald-600 font-medium">
                                        Similarity {(source.similarity * 100).toFixed(0)}%
                                      </span>
                                    </div>
                                    {(source.semanticScore !== undefined || source.keywordScore !== undefined) && (
                                      <div className="text-gray-500">
                                        Semantic {(Math.min(1, Math.max(0, source.semanticScore ?? 0)) * 100).toFixed(0)}% · Keyword {(Math.min(1, Math.max(0, source.keywordScore ?? 0)) * 100).toFixed(0)}%
                                      </div>
                                    )}
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}

                      {/* AI Suggestions for preview mode */}
                      {message.isPreview && message.suggestions && message.suggestions.length > 0 && (
                        <div className="mt-4 space-y-3 border-t border-gray-200 pt-4">
                          <p className="text-xs font-semibold text-gray-600 flex items-center gap-2">
                            <Bot className="h-3 w-3" />
                            TRY ASKING ABOUT
                          </p>
                          <div className="flex flex-wrap gap-2">
                            {message.suggestions.map((suggestion, index) => (
                              <Button
                                key={`${message.id}-suggestion-${index}`}
                                variant="outline"
                                size="sm"
                                className="h-7 text-xs border-dashed border-primary/40 text-primary hover:bg-primary/10 hover:border-primary/60 transition-all duration-200"
                                onClick={() => handleQuickInsert(suggestion)}
                              >
                                {suggestion}
                              </Button>
                            ))}
                          </div>
                        </div>
                      )}

                      {message.role === 'assistant' && message.logId && (
                        <div className="mt-4 pt-3 border-t border-gray-200">
                          <div className="flex items-center justify-between">
                            <span className="text-xs text-gray-500">Was this reply helpful?</span>
                            <div className="flex items-center gap-2">
                              <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className={cn(
                                  'h-8 w-8 rounded-full transition-all duration-200',
                                  message.feedback === 'positive'
                                    ? 'text-emerald-600 bg-emerald-50 hover:bg-emerald-100'
                                    : 'text-gray-400 hover:text-emerald-600 hover:bg-emerald-50'
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
                                  'h-8 w-8 rounded-full transition-all duration-200',
                                  message.feedback === 'negative'
                                    ? 'text-rose-600 bg-rose-50 hover:bg-rose-100'
                                    : 'text-gray-400 hover:text-rose-600 hover:bg-rose-50'
                                )}
                                disabled={message.feedbackSubmitting}
                                onClick={() => handleFeedback(message.id, 'negative')}
                              >
                                <ThumbsDown className="h-4 w-4" />
                              </Button>
                              {message.feedback && (
                                <span className="text-[11px] text-emerald-600 ml-2 flex items-center gap-1">
                                  <div className="w-1.5 h-1.5 rounded-full bg-emerald-500"></div>
                                  Recorded
                                </span>
                              )}
                            </div>
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                ))}

                {loading && (
                  <div className="flex justify-start">
                    <div className="max-w-[85%] rounded-2xl rounded-bl-md px-4 py-3 bg-white border border-gray-200 shadow-lg">
                      <div className="flex items-center gap-3">
                        <div className="flex items-center gap-2 text-xs font-medium text-gray-500">
                          <Bot className="h-3 w-3" />
                          AI Assistant
                        </div>
                      </div>
                      <div className="mt-3 flex items-center gap-2 text-sm text-gray-600">
                        <div className="flex gap-1">
                          {[0, 1, 2].map((i) => (
                            <div
                              key={i}
                              className="w-2 h-2 bg-primary rounded-full animate-bounce"
                              style={{ animationDelay: `${i * 0.1}s` }}
                            />
                          ))}
                        </div>
                        <span>Thinking...</span>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {error && (
              <div className="mx-6 mb-4 rounded-xl border border-red-200 bg-gradient-to-r from-red-50 to-rose-50 px-4 py-3 text-sm text-red-700 flex items-center gap-3">
                <div className="w-2 h-2 rounded-full bg-red-500 flex-shrink-0"></div>
                <span>{error}</span>
              </div>
            )}

            <div className="border-t border-white/20 bg-white/50 backdrop-blur-sm px-6 py-4">
              <form
                onSubmit={(event) => {
                  event.preventDefault()
                  if (!loading) {
                    handleSubmit()
                  }
                }}
                className="space-y-4"
              >
                <div className="relative">
                  <Textarea
                    placeholder={user
                      ? "Ask about your shipments, deliveries, or system operations..."
                      : "Ask me about Mini-UPS services, delivery times, or tracking..."
                    }
                    value={input}
                    onChange={(event) => setInput(event.target.value)}
                    disabled={loading}
                    rows={3}
                    className="resize-none border-gray-200 focus:border-primary/50 focus:ring-2 focus:ring-primary/20 rounded-xl shadow-sm transition-all duration-200"
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' && !e.shiftKey) {
                        e.preventDefault()
                        if (!loading && input.trim()) {
                          handleSubmit()
                        }
                      }
                    }}
                  />
                  <div className="absolute bottom-3 right-3">
                    <Button
                      type="submit"
                      disabled={loading || !input.trim()}
                      size="sm"
                      className="h-8 w-8 p-0 rounded-full shadow-lg hover:shadow-xl transition-all duration-200"
                    >
                      {loading ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : (
                        <Send className="h-4 w-4" />
                      )}
                    </Button>
                  </div>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-xs text-gray-500">
                    <div className="flex items-center gap-1">
                      <div className={cn(
                        "w-2 h-2 rounded-full",
                        user ? "bg-green-500" : "bg-orange-500"
                      )}></div>
                      <span>
                        {user ? "Private and secure chat" : "Preview mode - limited features"}
                      </span>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    {!user && (
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => window.location.href = '/login'}
                        className="text-xs h-8 hover:bg-primary/10 hover:border-primary/40 transition-all duration-200"
                      >
                        <User className="h-3 w-3 mr-1" />
                        Sign In
                      </Button>
                    )}
                    <span className="text-[10px] text-gray-400">Press Enter to send</span>
                  </div>
                </div>
              </form>
            </div>
          </div>
        </SheetContent>
      </Sheet>
    </div>
  )
}

