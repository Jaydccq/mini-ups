import React from 'react'
import { Link } from 'react-router-dom'
import { Copy, ExternalLink } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { toast } from 'sonner'

interface ActionsCardProps {
  trackingNumber: string
}

export const ActionsCard: React.FC<ActionsCardProps> = ({ trackingNumber }) => {
  const copyTrackingNumber = () => {
    navigator.clipboard.writeText(trackingNumber)
    toast.success('Tracking number copied to clipboard')
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Actions</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          <Button
            variant="outline"
            className="w-full justify-start"
            onClick={copyTrackingNumber}
          >
            <Copy className="h-4 w-4 mr-2" />
            Copy Tracking Number
          </Button>
          <Link to={`/tracking?q=${trackingNumber}`} className="block">
            <Button variant="outline" className="w-full justify-start">
              <ExternalLink className="h-4 w-4 mr-2" />
              Public Tracking Page
            </Button>
          </Link>
        </div>
      </CardContent>
    </Card>
  )
}