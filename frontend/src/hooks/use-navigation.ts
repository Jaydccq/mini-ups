import { useMemo } from 'react'
import { useAuthStore } from '@/stores/auth-store'
import { navigationConfig, type NavItem } from '@/config/navigation'

export function useNavigation() {
  const { user } = useAuthStore()

  const { filteredNavigation, pathRolesMap } = useMemo(() => {
    const pathRolesMap = new Map<string, string[]>()
    
    const flattenNavigation = (items: NavItem[]) => {
      for (const item of items) {
        pathRolesMap.set(item.path, item.roles)
        if (item.children) {
          flattenNavigation(item.children)
        }
      }
    }
    
    flattenNavigation(navigationConfig)

    const filterByRole = (items: NavItem[]): NavItem[] => {
      return items.filter(item => {
        if (item.roles.length === 0) {
          return true
        }
        
        if (!user || !item.roles.includes(user.role)) {
          return false
        }
        
        return true
      }).map(item => ({
        ...item,
        children: item.children ? filterByRole(item.children) : undefined
      }))
    }

    const filtered = filterByRole(navigationConfig)
    return { filteredNavigation: filtered, pathRolesMap }
  }, [user])

  const visibleNavigation = useMemo(() => {
    return filteredNavigation.filter(item => item.inNav)
  }, [filteredNavigation])

  const hasAccess = (path: string): boolean => {
    const roles = pathRolesMap.get(path)
    if (!roles) return false
    if (roles.length === 0) return true
    return user ? roles.includes(user.role) : false
  }

  return {
    navigation: filteredNavigation,
    visibleNavigation,
    hasAccess
  }
}