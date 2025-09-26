import api from './api'

export interface RagQueryPayload {
  query: string
  context?: Record<string, unknown>
}

export interface RagSourceResponse {
  title: string
  source: string
  similarity: number
  confidence: number
  semanticScore?: number
  keywordScore?: number
}

export interface RagQueryResult {
  logId?: string
  answer: string
  confidence: number
  sources: RagSourceResponse[]
  warnings?: string[]
}

export interface RagFeedbackPayload {
  logId: string
  feedback: 'POSITIVE' | 'NEGATIVE'
  comment?: string
}

export async function queryRag(payload: RagQueryPayload): Promise<RagQueryResult> {
  const { data } = await api.post<RagQueryResult>('/rag/query', payload)
  return data
}

export async function submitRagFeedback(payload: RagFeedbackPayload): Promise<void> {
  await api.post('/rag/feedback', payload)
}
