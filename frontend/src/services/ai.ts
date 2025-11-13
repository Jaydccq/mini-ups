/**
 * AI Service for Guest Users
 *
 * Provides intelligent responses using OpenRouter API
 * for users who are not logged in to the system
 */

interface OpenRouterMessage {
  role: 'system' | 'user' | 'assistant'
  content: string
}

interface OpenRouterResponse {
  choices: Array<{
    message: {
      content: string
    }
    finish_reason: string
  }>
}

interface AIQueryResult {
  answer: string
  isPreview: boolean
  suggestions?: string[]
}

interface EnhancedRAGResult {
  answer: string
  sources?: Array<{
    title: string
    source: string
    similarity: number
    confidence: number
    semanticScore?: number
    keywordScore?: number
  }>
  warnings?: string[]
  logId?: string
  isEnhanced: boolean
}

// OpenRouter API configuration
const OPENROUTER_API_KEY = import.meta.env.VITE_OPENROUTER_API_KEY || 'sk-or-v1-YOUR_KEY_HERE'
const OPENROUTER_BASE_URL = 'https://openrouter.ai/api/v1'

// System prompt for Mini-UPS AI assistant
const SYSTEM_PROMPT = `You are a helpful AI assistant for Mini-UPS, a shipping and delivery management system.

You help users understand:
- Shipping and delivery processes
- Package tracking and status updates
- Driver check-in procedures
- Fleet and truck management
- World simulator synchronization
- System configuration and troubleshooting
- Delivery times and scheduling

Guidelines:
- Be concise and helpful
- Use specific examples when possible
- Mention Mini-UPS features and capabilities
- Focus on practical, actionable advice
- If asked about specific tracking numbers or personal data, explain that they need to sign in for personalized information
- Use emojis sparingly and appropriately
- Keep responses under 300 words for better readability

For questions about personal shipments, tracking numbers, or account-specific information, politely redirect users to sign in for access to their personalized data and enhanced features.`

// Enhanced system prompt for logged-in users with RAG data
const ENHANCED_RAG_PROMPT = `You are an intelligent assistant for Mini-UPS logistics system. You have access to live data from the Mini-UPS database and knowledge base.

Your role:
- Analyze user questions and provide comprehensive, accurate answers
- Use the provided RAG data sources to enhance your responses
- Combine your general knowledge with specific Mini-UPS system data
- Provide actionable insights and recommendations
- Reference specific sources when relevant

Guidelines:
- Start with a direct answer to the user's question
- Incorporate specific data from the provided sources
- Add context and recommendations based on system knowledge
- Use clear formatting with sections, bullet points, and emojis
- Keep responses concise but comprehensive (under 400 words)
- Always maintain a helpful, professional tone

When RAG sources are provided:
- Synthesize information from multiple sources
- Highlight the most relevant and recent data
- Explain technical concepts in user-friendly terms
- Provide specific actions users can take

Response structure:
1. Direct answer with key information
2. Supporting details from sources
3. Actionable recommendations
4. Related suggestions if applicable`

export class AIService {
  private static instance: AIService

  private constructor() {}

  static getInstance(): AIService {
    if (!AIService.instance) {
      AIService.instance = new AIService()
    }
    return AIService.instance
  }

  async queryAI(query: string): Promise<AIQueryResult> {
    try {
      // For development/demo purposes, if no API key is configured,
      // fall back to intelligent static responses
      if (!OPENROUTER_API_KEY || OPENROUTER_API_KEY.includes('YOUR_KEY_HERE')) {
        return this.getFallbackResponse(query)
      }

      const messages: OpenRouterMessage[] = [
        {
          role: 'system',
          content: SYSTEM_PROMPT
        },
        {
          role: 'user',
          content: query
        }
      ]

      const response = await fetch(`${OPENROUTER_BASE_URL}/chat/completions`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${OPENROUTER_API_KEY}`,
          'Content-Type': 'application/json',
          'HTTP-Referer': window.location.origin,
          'X-Title': 'Mini-UPS AI Assistant'
        },
        body: JSON.stringify({
          model: 'anthropic/claude-3.5-haiku', // Fast and cost-effective
          messages,
          max_tokens: 500,
          temperature: 0.7,
          top_p: 1,
          frequency_penalty: 0,
          presence_penalty: 0
        })
      })

      if (!response.ok) {
        throw new Error(`OpenRouter API error: ${response.status}`)
      }

      const data: OpenRouterResponse = await response.json()
      const answer = data.choices[0]?.message?.content || 'Sorry, I couldn\'t generate a response.'

      return {
        answer: answer + this.getPreviewSuffix(),
        isPreview: true,
        suggestions: this.getSuggestions(query)
      }

    } catch (error) {
      console.error('AI query failed:', error)
      return this.getFallbackResponse(query)
    }
  }

  async enhanceRAGResponse(query: string, ragData: any): Promise<EnhancedRAGResult> {
    try {
      // For development/demo purposes, if no API key is configured,
      // return enhanced static response
      if (!OPENROUTER_API_KEY || OPENROUTER_API_KEY.includes('YOUR_KEY_HERE')) {
        return this.getEnhancedFallbackResponse(query, ragData)
      }

      // Create enhanced prompt with RAG data
      const ragContext = this.formatRAGContext(ragData)
      const enhancedPrompt = `${ENHANCED_RAG_PROMPT}

RAG Data Sources:
${ragContext}

User Question: ${query}

Please provide a comprehensive response using both your knowledge and the provided RAG data.`

      const messages: OpenRouterMessage[] = [
        {
          role: 'system',
          content: ENHANCED_RAG_PROMPT
        },
        {
          role: 'user',
          content: enhancedPrompt
        }
      ]

      const response = await fetch(`${OPENROUTER_BASE_URL}/chat/completions`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${OPENROUTER_API_KEY}`,
          'Content-Type': 'application/json',
          'HTTP-Referer': window.location.origin,
          'X-Title': 'Mini-UPS Enhanced RAG Assistant'
        },
        body: JSON.stringify({
          model: 'anthropic/claude-3.5-sonnet', // More capable model for complex RAG processing
          messages,
          max_tokens: 800,
          temperature: 0.3,
          top_p: 1,
          frequency_penalty: 0,
          presence_penalty: 0
        })
      })

      if (!response.ok) {
        throw new Error(`OpenRouter API error: ${response.status}`)
      }

      const data: OpenRouterResponse = await response.json()
      const answer = data.choices[0]?.message?.content || 'Sorry, I couldn\'t generate an enhanced response.'

      return {
        answer,
        sources: ragData.sources,
        warnings: ragData.warnings,
        logId: ragData.logId,
        isEnhanced: true
      }

    } catch (error) {
      console.error('Enhanced RAG query failed:', error)
      return this.getEnhancedFallbackResponse(query, ragData)
    }
  }

  private formatRAGContext(ragData: any): string {
    if (!ragData.sources || ragData.sources.length === 0) {
      return 'No specific RAG data available for this query.'
    }

    return ragData.sources.map((source: any, index: number) =>
      `Source ${index + 1}: ${source.title}
Content: ${source.source}
Relevance: ${(source.similarity * 100).toFixed(1)}%
Confidence: ${(source.confidence * 100).toFixed(1)}%`
    ).join('\n\n')
  }

  private getEnhancedFallbackResponse(query: string, ragData: any): EnhancedRAGResult {
    // Use the original RAG answer but enhance it with structure
    const baseAnswer = ragData.answer || 'I can help you with Mini-UPS questions.'

    const enhancedAnswer = `📋 **Mini-UPS System Response**

${baseAnswer}

💡 **Quick Tips:**
• Use the search functionality for specific information
• Check the documentation for detailed procedures
• Contact support for urgent matters

🔍 **Related Actions:**
• View system dashboard for live updates
• Check notification center for alerts
• Access help documentation`

    return {
      answer: enhancedAnswer,
      sources: ragData.sources,
      warnings: ragData.warnings,
      logId: ragData.logId,
      isEnhanced: true
    }
  }

  private getFallbackResponse(query: string): AIQueryResult {
    const lowerQuery = query.toLowerCase()

    // Enhanced intelligent keyword matching
    const responses = {
      sync: `To sync world simulator status:

🔄 **Automatic Sync**: The system syncs every 30 seconds automatically
📡 **Manual Sync**: Use the admin dashboard "Sync Now" button
🗂️ **Check Status**: Monitor trucks table for updated positions
📊 **Verify Data**: Confirm warehouse inventory synchronization

**Quick Steps:**
1. Navigate to Admin Dashboard → World Simulator
2. Click "Sync Now" for immediate refresh
3. Monitor status indicators for confirmation`,

      delay: `Handling delayed deliveries:

📍 **Real-time Tracking**: Check shipment status for live updates
🚛 **Driver Reassignment**: Use driver management to assign available drivers
🌐 **Traffic Monitoring**: Check world simulator for traffic conditions
📧 **Auto Notifications**: System sends automatic customer updates

**Action Items:**
• View delayed shipments in Admin → Shipments
• Use bulk actions for driver reassignment
• Monitor traffic patterns in World Simulator`,

      driver: `Driver check-in process:

📱 **Mobile Check-in**: Drivers use mobile app at delivery locations
📍 **GPS Validation**: System validates against destination coordinates
📸 **Photo Proof**: Upload delivery confirmation photos
✅ **Status Update**: Automatic update to 'DELIVERED' status
📧 **Customer Alert**: Automatic notification sent to customer

**Troubleshooting Tips:**
• Ensure GPS permissions are enabled
• Check network connectivity for uploads
• Verify driver credentials are active`,

      truck: `Truck management overview:

🗺️ **Fleet Overview**: Real-time view of all truck locations
🔗 **Assignment System**: Automatic driver-truck pairing
🎯 **Route Optimization**: AI-powered route planning for efficiency
📡 **Live Tracking**: GPS integration for real-time monitoring

**Key Features:**
• Interactive fleet map with live positions
• Fuel and maintenance tracking
• Performance analytics and reporting`,

      delivery: `Delivery time information:

⚡ **Express**: 1-2 business days
📦 **Standard**: 2-3 business days
🚀 **Same-day**: Available in select metropolitan areas
📍 **Real-time**: Live tracking with estimated arrival

**Factors Affecting Delivery:**
• Package size and weight
• Distance to destination
• Current traffic conditions
• Weather conditions

**Track Your Package:**
• Enter tracking number on main page
• Get SMS/email updates
• View driver location on live map`,

      tracking: `Shipment tracking guide:

🔍 **Track Package**: Enter tracking number on tracking page
📍 **Live Updates**: Real-time status and location information
📱 **Notifications**: SMS/email alerts for status changes
📞 **Support**: Contact support for delivery issues

**Tracking Features:**
• Real-time GPS location
• Delivery photo confirmation
• Estimated arrival time
• Direct driver communication

**Common Status Codes:**
• PENDING: Package prepared for pickup
• IN_TRANSIT: En route to destination
• OUT_FOR_DELIVERY: Driver heading to you
• DELIVERED: Successfully delivered`,

      default: `🎯 **Mini-UPS AI Assistant**

I can help you with:
• **Delivery scheduling** and tracking information
• **Driver coordination** and check-in processes
• **Fleet management** and vehicle tracking
• **System operations** and troubleshooting
• **Shipping guidance** and best practices
• **World simulator** status and synchronization

💡 **Pro tip**: Ask specific questions about shipping, tracking, or system operations for detailed guidance!`
    }

    // Smart keyword matching
    let selectedResponse = responses.default

    for (const [key, response] of Object.entries(responses)) {
      if (key === 'default') continue

      const keywords = {
        sync: ['sync', 'simulator', 'world', 'synchronize'],
        delay: ['delay', 'delayed', 'late', 'behind'],
        driver: ['driver', 'check-in', 'checkin', 'mobile'],
        truck: ['truck', 'vehicle', 'fleet', 'management'],
        delivery: ['delivery', 'deliver', 'time', 'schedule', 'speed'],
        tracking: ['track', 'tracking', 'status', 'shipment', 'package']
      }

      if (keywords[key as keyof typeof keywords]?.some(keyword => lowerQuery.includes(keyword))) {
        selectedResponse = response
        break
      }
    }

    return {
      answer: selectedResponse + this.getPreviewSuffix(),
      isPreview: true,
      suggestions: this.getSuggestions(query)
    }
  }

  private getPreviewSuffix(): string {
    return `

---
🔐 **Sign in to Mini-UPS** for enhanced features:
• Live data access from your personal account
• Real-time shipment tracking with your tracking numbers
• Personalized delivery recommendations
• Source citations and detailed references
• Advanced search capabilities with full system access`
  }

  private getSuggestions(query: string): string[] {
    const allSuggestions = [
      'How to sync world simulator status?',
      'What are the delivery time options?',
      'How does driver check-in work?',
      'How to track my shipment?',
      'Fleet management best practices',
      'How to handle delayed deliveries?',
      'System configuration guide',
      'Truck assignment process'
    ]

    // Return 3 random suggestions that don't match the current query
    const filtered = allSuggestions.filter(s =>
      !query.toLowerCase().includes(s.toLowerCase().substring(0, 10))
    )

    return filtered
      .sort(() => Math.random() - 0.5)
      .slice(0, 3)
  }
}

export const aiService = AIService.getInstance()