import * as React from "react"
import { cn } from "@/lib/utils"

interface FlashOnUpdateProps<T = unknown> extends React.HTMLAttributes<HTMLDivElement> {
  /** 
   * The value that triggers the flash effect. Note: Change detection uses
   * shallow comparison (`!==`). For object or array triggers, you must pass a new
   * reference to trigger the effect. Using `React.useMemo` for complex triggers or
   * passing a primitive value (e.g., a timestamp or ID) is recommended.
   */
  trigger: T
  children: React.ReactNode
  /** Flash duration in milliseconds */
  duration?: number
}

/**
 * A component that flashes a background animation when the `trigger` prop changes.
 * @param trigger The value that triggers the flash effect. Note: Change detection uses
 * shallow comparison (`!==`). For object or array triggers, you must pass a new
 * reference to trigger the effect. Using `React.useMemo` for complex triggers or
 * passing a primitive value (e.g., a timestamp or ID) is recommended.
 * @param duration Flash duration in milliseconds.
 */
function FlashOnUpdateComponent<T = unknown>(
  { trigger, children, duration = 1500, className, ...props }: FlashOnUpdateProps<T>,
  ref: React.Ref<HTMLDivElement>
) {
  const [isFlashing, setIsFlashing] = React.useState(false)
  const prevTrigger = React.useRef<T>(trigger)

  React.useEffect(() => {
    if (prevTrigger.current !== trigger) {
      setIsFlashing(true)
      const timer = setTimeout(() => setIsFlashing(false), duration)
      prevTrigger.current = trigger
      return () => clearTimeout(timer)
    }
  }, [trigger, duration])

  return (
    <div
      ref={ref}
      className={cn(
        "transition-colors",
        isFlashing && "animate-flash",
        className
      )}
      style={{
        ...props.style,
        '--flash-duration': `${duration}ms`,
      } as React.CSSProperties}
      {...props}
    >
      {children}
    </div>
  )
}

// We use a type assertion here because React.forwardRef doesn't preserve generic
// type parameters on the component function. This is a standard and safe workaround.
const FlashOnUpdate = React.forwardRef(FlashOnUpdateComponent) as <T = unknown>(
  props: FlashOnUpdateProps<T> & { ref?: React.Ref<HTMLDivElement> }
) => React.ReactElement

// Set displayName on the underlying component
FlashOnUpdateComponent.displayName = "FlashOnUpdate"

export { FlashOnUpdate }