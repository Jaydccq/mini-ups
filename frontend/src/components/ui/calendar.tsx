import * as React from "react"
import { ChevronLeft, ChevronRight } from "lucide-react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"

interface CalendarProps {
  mode?: 'single'
  selected?: Date
  onSelect?: (date: Date | undefined) => void
  disabled?: (date: Date) => boolean
  initialFocus?: boolean
  className?: string
}

function Calendar({
  className,
  selected,
  onSelect,
  disabled,
  ...props
}: CalendarProps) {
  const [currentDate, setCurrentDate] = React.useState(new Date())
  
  const today = new Date()
  const year = currentDate.getFullYear()
  const month = currentDate.getMonth()
  
  const firstDayOfMonth = new Date(year, month, 1)
  const lastDayOfMonth = new Date(year, month + 1, 0)
  const firstDayWeekday = firstDayOfMonth.getDay()
  const daysInMonth = lastDayOfMonth.getDate()
  
  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ]
  
  const weekDays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
  
  const goToPreviousMonth = () => {
    setCurrentDate(new Date(year, month - 1, 1))
  }
  
  const goToNextMonth = () => {
    setCurrentDate(new Date(year, month + 1, 1))
  }
  
  const handleDateClick = (date: Date) => {
    if (!disabled || !disabled(date)) {
      onSelect?.(date)
    }
  }
  
  const renderDays = () => {
    const days = []
    
    // Empty cells for days before month starts
    for (let i = 0; i < firstDayWeekday; i++) {
      days.push(<div key={`empty-${i}`} className="h-9 w-9" />)
    }
    
    // Days of the month
    for (let day = 1; day <= daysInMonth; day++) {
      const date = new Date(year, month, day)
      const isSelected = selected && date.toDateString() === selected.toDateString()
      const isToday = date.toDateString() === today.toDateString()
      const isDisabled = disabled && disabled(date)
      
      days.push(
        <Button
          key={day}
          variant={isSelected ? "default" : "ghost"}
          size="sm"
          className={cn(
            "h-9 w-9 p-0 font-normal",
            isToday && !isSelected && "bg-accent text-accent-foreground",
            isDisabled && "text-muted-foreground opacity-50 cursor-not-allowed"
          )}
          disabled={isDisabled}
          onClick={() => handleDateClick(date)}
        >
          {day}
        </Button>
      )
    }
    
    return days
  }
  
  return (
    <div className={cn("p-3", className)}>
      <div className="flex justify-center pt-1 relative items-center mb-4">
        <Button
          variant="outline"
          size="sm"
          className="absolute left-0 h-7 w-7 bg-transparent p-0 opacity-50 hover:opacity-100"
          onClick={goToPreviousMonth}
        >
          <ChevronLeft className="h-4 w-4" />
        </Button>
        <div className="text-sm font-medium">
          {monthNames[month]} {year}
        </div>
        <Button
          variant="outline"
          size="sm"
          className="absolute right-0 h-7 w-7 bg-transparent p-0 opacity-50 hover:opacity-100"
          onClick={goToNextMonth}
        >
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
      
      <div className="grid grid-cols-7 gap-1">
        {weekDays.map(day => (
          <div key={day} className="h-9 w-9 text-center text-sm font-normal text-muted-foreground flex items-center justify-center">
            {day}
          </div>
        ))}
        {renderDays()}
      </div>
    </div>
  )
}

Calendar.displayName = "Calendar"

export { Calendar }