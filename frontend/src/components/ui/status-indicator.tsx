import * as React from "react"
import { cn } from "@/lib/utils"
import { Badge } from "./badge"
import { 
  formatShipmentStatus, 
  getStatusBadgeVariant, 
  getStatusDotColor,
  getStatusDescription
} from "@/config/status"

interface StatusIndicatorProps extends React.HTMLAttributes<HTMLDivElement> {
  status: string
  showDot?: boolean
  animated?: boolean
  showDescription?: boolean
}

const StatusIndicator = React.forwardRef<HTMLDivElement, StatusIndicatorProps>(
  ({ status, showDot = false, animated = false, showDescription = false, className, ...props }, ref) => {
    const badgeVariant = getStatusBadgeVariant(status)
    const dotColor = getStatusDotColor(status)
    const description = getStatusDescription(status)

    return (
      <div ref={ref} className={cn("flex items-center gap-2", className)} {...props}>
        {showDot && (
          <div
            className={cn(
              "h-2 w-2 rounded-full",
              dotColor,
              animated && "animate-pulse-slow"
            )}
          />
        )}
        <div className="flex flex-col">
          <Badge variant={badgeVariant}>
            {formatShipmentStatus(status)}
          </Badge>
          {showDescription && description && (
            <span className="text-xs text-muted-foreground mt-1">
              {description}
            </span>
          )}
        </div>
      </div>
    )
  }
)
StatusIndicator.displayName = "StatusIndicator"

export { StatusIndicator }