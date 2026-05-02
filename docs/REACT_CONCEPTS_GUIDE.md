# Mini-UPS 前端 React 知识点详解

本文档基于 `frontend/` 项目中实际使用的代码，系统讲解项目里用到的 React 概念、模式与生态库。每个知识点都给出代码出处（`文件:行号`），方便对照源码学习。

技术栈基线（见 `frontend/package.json`）：

- React 18.2 + TypeScript 5.2
- Vite 7（构建）
- React Router 6（路由）
- TanStack Query v5（服务端状态）
- Zustand 4（客户端状态）
- React Hook Form 7 + Zod 4（表单与校验）
- Radix UI + Tailwind + class-variance-authority（UI 与样式）
- Sonner（Toast）
- Storybook 9 + Vitest + Testing Library（开发/测试）

---

## 目录

1. [应用入口与渲染根](#1-应用入口与渲染根)
2. [函数组件与 JSX](#2-函数组件与-jsx)
3. [内置 Hooks](#3-内置-hooks)
4. [自定义 Hooks](#4-自定义-hooks)
5. [Context 与 Provider 组合](#5-context-与-provider-组合)
6. [代码分割：lazy + Suspense](#6-代码分割lazy--suspense)
7. [Error Boundary（错误边界）](#7-error-boundary错误边界)
8. [forwardRef、displayName 与泛型组件](#8-forwardrefdisplayname-与泛型组件)
9. [复合组件与 Radix 原语](#9-复合组件与-radix-原语)
10. [React Router v6](#10-react-router-v6)
11. [客户端状态：Zustand](#11-客户端状态zustand)
12. [服务端状态：TanStack Query](#12-服务端状态tanstack-query)
13. [表单：React Hook Form + Zod](#13-表单react-hook-form--zod)
14. [实时通信 Hook 模式](#14-实时通信-hook-模式)
15. [Toast 通知封装](#15-toast-通知封装)
16. [样式系统：CVA + cn() + Tailwind](#16-样式系统cva--cn--tailwind)
17. [TypeScript 与 React 的结合](#17-typescript-与-react-的结合)
18. [测试与 Storybook](#18-测试与-storybook)

---

## 1. 应用入口与渲染根

入口文件 `frontend/src/main.tsx` 演示了 React 18 的并发根 API：

```tsx
// frontend/src/main.tsx:9-16
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>,
)
```

知识点：

- **`createRoot`**（React 18+）替代了旧的 `ReactDOM.render`，开启并发特性。
- **`StrictMode`**：开发期把 effect/render 调用执行两次，用来揪出副作用不纯净的代码。
- **Provider 嵌套顺序**：路由在最内层，Query 客户端在外层；这是因为路由内的组件要能调用 `useQuery`。
- 末尾的 `!` 是 TS 的非空断言，告诉编译器 `getElementById('root')` 不会是 `null`。

---

## 2. 函数组件与 JSX

项目几乎完全使用函数组件（仅 `ErrorBoundary` 是 class，因为 React 没有提供 `useErrorBoundary` Hook）。

`frontend/src/App.tsx:46` 是最简单的根组件：

```tsx
function App() {
  return (
    <div className="min-h-screen bg-background">
      <Layout>
        <Suspense fallback={<PageLoader />}>
          <Routes>...</Routes>
        </Suspense>
      </Layout>
      <RagAssistant />
      <Toaster />
    </div>
  )
}
export default App
```

而 `ProtectedRoute`（`src/components/auth/ProtectedRoute.tsx:12`）展示了带类型 props 的写法：

```tsx
export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children, requireAdmin = false, allowedRoles, redirectTo = '/login'
}) => { /* ... */ }
```

要点：

- **`React.FC`**：自带 `children` 类型。社区现代写法更倾向显式声明 `children: React.ReactNode`，本项目两种风格都存在。
- **解构 props 时给默认值**：`requireAdmin = false`。
- **`<>...</>`** 是 Fragment 简写，用于不引入额外 DOM 节点。

---

## 3. 内置 Hooks

### 3.1 `useState`

`CreateShipmentPage.tsx:30-31` 管理多步表单的当前步骤与"草稿已保存"状态：

```tsx
const [currentStep, setCurrentStep] = useState(1);
const [isDraftSaved, setIsDraftSaved] = useState(false);
```

`useShipmentStatus` 的读取也用到 `useState`（`hooks/useSocket.ts:5`）：

```ts
const [status, setStatus] = useState<SocketStatus>(socketService.getStatus());
```

要点：

- 初始值可以是**惰性函数** `useState(() => expensiveInit())` —— 仅首次渲染调用一次。
- 函数式 `setCurrentStep((prev) => prev + 1)`（见 `CreateShipmentPage.tsx:118`）用于基于上一次状态的更新，避免 stale closure。

### 3.2 `useEffect`

最典型的三种用法都在项目里出现：

**（a）订阅外部源 + 清理**（`hooks/useSocket.ts:7-10`）：

```ts
useEffect(() => {
  const unsubscribe = socketService.onStatusChange(setStatus);
  return unsubscribe;       // 卸载时取消订阅
}, []);                     // 空依赖：仅 mount 时执行
```

**（b）随依赖变化做副作用 + 防抖清理**（`CreateShipmentPage.tsx:81-90`）：

```ts
useEffect(() => {
  const timeoutId = setTimeout(() => {
    localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(getValues()));
    setIsDraftSaved(true);
    setTimeout(() => setIsDraftSaved(false), 2000);
  }, 1000);
  return () => clearTimeout(timeoutId);
}, [formData, getValues]);
```

每次 `formData` 变化都会重置 1 秒的 `setTimeout`，实现"输入停止 1 秒后才落库"的防抖效果，**返回的清理函数**正是防抖的关键。

**（c）首屏读取本地缓存**（`CreateShipmentPage.tsx:64-78`）：

```ts
useEffect(() => {
  const savedDraft = localStorage.getItem(DRAFT_STORAGE_KEY);
  if (savedDraft) { /* setValue 回填表单 */ }
}, [setValue]);
```

要点：

- **依赖数组**决定 effect 何时重跑；漏依赖会导致 stale closure。
- React 18 在 `StrictMode` 下 effect 会执行→清理→再执行一次，因此清理函数必须真正幂等。

### 3.3 `useRef`

`flash-on-update.tsx:30` 用 `useRef` 记录"上一次的 trigger 值"，避免引起重渲染：

```tsx
const prevTrigger = React.useRef<T>(trigger)

React.useEffect(() => {
  if (prevTrigger.current !== trigger) {
    setIsFlashing(true)
    const timer = setTimeout(() => setIsFlashing(false), duration)
    prevTrigger.current = trigger
    return () => clearTimeout(timer)
  }
}, [trigger, duration])
```

要点：

- `ref.current` 写入**不会**触发渲染，适合保存"前一帧"或"DOM 元素"等可变值。
- `forwardRef` 把 ref 转交给宿主元素（见 §8）。

### 3.4 `useMemo` / `useCallback`

UI 库里用得较多（如 `data-table.tsx`）。它们是性能优化工具，**不是默认手段**。一个好的判断标准：

- **`useMemo`**：当某个值用作子组件 prop 或别的 effect 的依赖、且重计算成本高，才包。
- **`useCallback`**：把函数固化成稳定引用，常配合 `React.memo` 子组件或 `useEffect` 依赖使用。

切忌"无脑包一切"，否则反而增加内存占用与可读性负担。

### 3.5 `useContext`

项目里几乎不用直接 `useContext`——客户端状态由 Zustand 接管，TanStack Query 也维护了自己的 Context。`SystemProvider`（`components/providers/SystemProvider.tsx:112`）只是把多个 Provider 组合，没有自定义 Context。

> **设计取舍**：Context 的"全量重渲染"问题（任意值变化都会刷新所有消费者）让它不适合大颗粒可变状态，本项目改用 Zustand 的"选择器订阅"（见 §11）。

---

## 4. 自定义 Hooks

`frontend/src/hooks/` 下汇集了项目所有的逻辑复用单元，这是 Hook 时代最重要的抽象方式。命名一律以 `use` 开头（这是 React 识别 Hook 调用规则的硬性约定）。

### 4.1 包装第三方资源：`useSocketStatus`

`hooks/useSocket.ts:4-19`：

```ts
export const useSocketStatus = () => {
  const [status, setStatus] = useState<SocketStatus>(socketService.getStatus());
  useEffect(() => {
    const unsubscribe = socketService.onStatusChange(setStatus);
    return unsubscribe;
  }, []);
  return {
    status,
    isConnected: status === 'connected',
    isConnecting: status === 'connecting',
    isDisconnected: status === 'disconnected',
    hasError: status === 'error',
  };
};
```

把"订阅 socket 状态 → 暴露语义化布尔值"封装成一行 `useSocketStatus()`，调用方再也不用自己写 `useEffect`。

### 4.2 封装 Query：`useDashboardData`

`hooks/useDashboardData.ts:37-82` 是 TanStack Query 的语义封装：

```ts
export const useDashboardData = ({ userId, enabled = true, refetchInterval }) => {
  const query = useQuery({
    queryKey: queryKeys.dashboard.stats(),
    queryFn: () => shipmentApi.getDashboardStats(userId),
    enabled: enabled && !!userId,
    staleTime: 1000 * 60 * 2,
    gcTime: 1000 * 60 * 10,
    refetchInterval: refetchInterval || 1000 * 60 * 5,
    refetchOnWindowFocus: false,
    refetchOnReconnect: true,
    retry: 3,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
    meta: { errorMessage: 'Failed to load dashboard statistics', critical: false },
  });
  return { data: query.data, isLoading: query.isLoading, /* ... */ };
};
```

把 `useQuery` 配置（`staleTime`、`gcTime`、重试策略等）藏在 Hook 内部，组件层只见 `data / isLoading / refetch`。这是 Hook 复用业务规则、让组件保持薄壳的核心范式。

### 4.3 Hook 组合：`useNotificationSync`

被 `SystemProvider` 调用（`components/providers/SystemProvider.tsx:54`），里面会再调用别的 Hook、维护订阅。**Hook 之间能直接互相调用**，这正是它比 HOC、render props 更优雅的地方。

**Hook 调用规则**（必须严格遵守）：

1. 只在函数顶层调用（不在 if / for / 嵌套函数中）。
2. 只在 React 函数组件或其他 Hook 中调用。

ESLint 的 `eslint-plugin-react-hooks`（`package.json:devDependencies`）会自动检查。

---

## 5. Context 与 Provider 组合

`components/providers/SystemProvider.tsx:112` 把所有"全局副作用"模块以子组件形式拼装在 `QueryClientProvider` 下：

```tsx
export const SystemProvider: React.FC<SystemProviderProps> = ({ children }) => (
  <GlobalErrorBoundary>
    <QueryClientProvider client={queryClient}>
      <WebSocketManager />
      <NotificationSyncManager />
      <ConflictResolutionManager />
      <ConnectionStatusDisplay />
      <Toaster position="top-right" expand richColors closeButton />
      {children}
      {process.env.NODE_ENV === 'development' && <ReactQueryDevtools />}
    </QueryClientProvider>
  </GlobalErrorBoundary>
);
```

模式要点：

- **"无 UI 管理者"组件**：`WebSocketManager`、`NotificationSyncManager` 等组件 `return null`，只跑 effect，把"启停外部资源"封装成可声明式挂载/卸载的子节点（`SystemProvider.tsx:26-47`）。
- **开发期工具按 NODE_ENV 条件渲染**：`ReactQueryDevtools` 仅在开发模式挂载。
- 这种"Provider 桶"式组织让 `App.tsx` 能保持极简。

---

## 6. 代码分割：lazy + Suspense

`App.tsx:11-29` 把每个 page 都用 `React.lazy` 包成异步 chunk，配合顶层 `<Suspense fallback={<PageLoader />}>` 给加载中状态：

```tsx
const HomePage = lazy(() =>
  import('@/pages/HomePage').then(module => ({ default: module.HomePage }))
)
// ...
<Suspense fallback={<PageLoader />}>
  <Routes>
    <Route path="/" element={<HomePage />} />
    {/* ... */}
  </Routes>
</Suspense>
```

要点：

- `React.lazy` 只接受**默认导出**的动态 import。这里页面用的是命名导出，所以用 `.then()` 手动转成 `{ default: ... }`。
- `Suspense` 在子树有"挂起"组件时展示 fallback。结合 lazy = 路由级代码分割，首屏 bundle 体积大幅缩小。
- `PageLoader`（`App.tsx:32-44`）使用 `<Skeleton>` 组件做骨架屏，体验比转圈圈更好。

---

## 7. Error Boundary（错误边界）

错误边界是 React 中**唯一仍必须用 class 写**的组件（因为依赖 `getDerivedStateFromError` / `componentDidCatch` 生命周期）。

`components/error/ErrorBoundary.tsx:47-79`：

```tsx
class BaseErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    return { hasError: true, error, errorId: `error_${Date.now()}_...` };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    this.setState({ error, errorInfo });
    this.logError(error, errorInfo);
    if (this.props.onError) this.props.onError(error, errorInfo);
  }
  // ...
}
```

亮点：

- **三层错误边界**：`GlobalErrorBoundary` / `RouteErrorBoundary` / `WidgetErrorBoundary`（`ErrorBoundary.tsx:344-354`），分别给整页崩溃、路由级失败、单个小组件失败用不同 UI。
- 错误边界**捕获不到**：事件处理函数里的同步错误、`setTimeout` 异步错误、SSR 错误、自身 render 错误。这些场景需要 try/catch 或全局 `window.onerror`。
- 配套的 `useErrorReporting` Hook（`ErrorBoundary.tsx:357-380`）补上"主动上报"能力。

---

## 8. forwardRef、displayName 与泛型组件

### 8.1 标准 forwardRef

`components/ui/button.tsx:43-55`：

```tsx
const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : "button"
    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      />
    )
  }
)
Button.displayName = "Button"
```

为什么需要：父组件想直接拿 `<Button>` 渲染出的 DOM 节点（聚焦、滚动、测量等），就得用 `forwardRef` 把 `ref` 转交。`displayName` 让 DevTools 显示组件名而不是 `ForwardRef`。

`asChild` 配合 Radix 的 `Slot` 让 `<Button>` 能"借用"子元素的 tag（这是 Radix 的"slot 模式"）。

### 8.2 泛型 forwardRef（高级）

`components/ui/flash-on-update.tsx:62-67`：

```tsx
const FlashOnUpdate = React.forwardRef(FlashOnUpdateComponent) as <T = unknown>(
  props: FlashOnUpdateProps<T> & { ref?: React.Ref<HTMLDivElement> }
) => React.ReactElement
```

`React.forwardRef` 的类型签名不会保留泛型参数 `<T>`。这里通过 `as` 类型断言**显式还原泛型**，是 React + TypeScript 的标准 workaround。

---

## 9. 复合组件与 Radix 原语

`components/ui/dialog.tsx`、`dropdown-menu.tsx`、`sheet.tsx` 等都是 Radix 的"复合组件（compound components）"模式：

```tsx
<Dialog>
  <DialogTrigger asChild>...</DialogTrigger>
  <DialogContent>
    <DialogHeader>
      <DialogTitle>...</DialogTitle>
      <DialogDescription>...</DialogDescription>
    </DialogHeader>
    {/* body */}
  </DialogContent>
</Dialog>
```

要点：

- 各个子组件之间通过 **Context** 共享开/关状态、ID 关联，无需用户手动传 props。
- Radix 内部用 **`createPortal`** 把对话框/下拉菜单挂到 `document.body`，避免 z-index 与 overflow 裁切问题（这是 Portal 的典型用例）。
- 项目自己只做了"加默认 className + 重新导出"的薄封装，业务侧拼装组件。

---

## 10. React Router v6

`App.tsx:52-77` 展示了 v6 的所有核心特性：

```tsx
<Routes>
  <Route path="/" element={<HomePage />} />
  <Route path="/login" element={<LoginPage />} />

  <Route element={<ProtectedLayout />}>          {/* 嵌套路由 + Outlet 守卫 */}
    <Route path="/dashboard" element={<DashboardPage />} />
    <Route path="/shipments/tracking/:trackingNumber" element={<ShipmentDetailPage />} />
  </Route>

  <Route element={<AdminLayout />}>
    <Route path="/admin" element={<AdminDashboardPage />} />
  </Route>
</Routes>
```

涉及的 v6 概念：

- **`element` prop** 取代了 v5 的 `component`/`render`，必须传 React 元素（带 JSX）。
- **嵌套 `<Route>` 不带 path**：父路由作为布局壳，内部用 `<Outlet />` 决定渲染哪个子路由（在 `ProtectedLayout` 内部）。
- **路由参数** `:trackingNumber` 通过 `useParams<{ trackingNumber: string }>()` 取得。
- **编程式导航**：`CreateShipmentPage.tsx:28` 中 `const navigate = useNavigate()`，提交成功后 `navigate('/shipments/tracking/...')`。
- **路由守卫**：`ProtectedRoute.tsx:21` 通过 `<Navigate to={redirectTo} replace />` 实现重定向。`replace` 表示不在历史栈里压入当前 URL。

---

## 11. 客户端状态：Zustand

`stores/auth-store.ts:25-89` 是 Zustand 的标准用法：

```ts
export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      status: 'idle',

      login: (user, token) => {
        set({ user, token, isAuthenticated: true, status: 'authenticated' })
        try { getWebSocketService().connect().catch(console.error) } catch (e) { /* ... */ }
      },

      logout: () => { set({ user: null, /* ... */ }); /* disconnect ws */ },

      setLoading: (loading) =>
        set({ status: loading ? 'loading' : get().isAuthenticated ? 'authenticated' : 'unauthenticated' }),

      updateUser: (userData) => {
        const currentUser = get().user
        if (currentUser) set({ user: { ...currentUser, ...userData } })
      }
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({ user: state.user, token: state.token, isAuthenticated: state.isAuthenticated })
    }
  )
)
```

知识点：

- **`create<AuthState>()(...)`** 的双层括号：第一层是泛型，第二层是真正接收 store factory 的调用。
- **`persist` 中间件**自动把状态写入 localStorage；`partialize` 决定**只持久化哪些字段**（不持久化 `status`，避免跨标签状态污染）。
- 在组件中：`const { user, isAuthenticated } = useAuthStore()`（如 `ProtectedRoute.tsx:18`）。
- 进阶用法可传**选择器**只订阅需要的字段：`useAuthStore(s => s.user)` —— 只有 `user` 变化时才会重渲染该组件，这是 Zustand 比 Context 更高性能的关键。
- Store 里直接发起副作用（连/断 WebSocket）是有意为之：把"登录态 ↔ 长连接"绑定在唯一真相源。

---

## 12. 服务端状态：TanStack Query

### 12.1 Query

`hooks/useDashboardData.ts:37` 用 `useQuery` 抓取"看板统计"，关键配置：

| 配置 | 含义 |
| --- | --- |
| `queryKey` | 缓存键，**数组结构**支持嵌套作用域 |
| `queryFn` | 实际取数函数，返回 Promise |
| `enabled` | false 时跳过请求（依赖未就绪场景） |
| `staleTime` | 多久后视为"陈旧"，期间复用缓存不打 API |
| `gcTime` | 没人订阅后保留多久（v5 改名，原 `cacheTime`） |
| `refetchInterval` | 轮询间隔 |
| `retry` / `retryDelay` | 失败重试策略，这里指数退避 |

### 12.2 Mutation

`hooks/mutations/useShipmentMutations.ts` 演示了三种风险等级的写操作模式：

**乐观更新**（`useAddShipmentComment`，`mutations/useShipmentMutations.ts:13-58`）：

```ts
onMutate: async (variables) => {
  await queryClient.cancelQueries({ queryKey: ['shipmentComments', variables.shipmentId] });
  const previousComments = queryClient.getQueryData(['shipmentComments', variables.shipmentId]);

  const optimisticComment = { id: `temp_${Date.now()}`, /* ... */ };
  queryClient.setQueryData(
    ['shipmentComments', variables.shipmentId],
    (old: any[]) => old ? [...old, optimisticComment] : [optimisticComment]
  );
  return { previousComments };           // 回传给 rollback
},
rollback: (variables, context) => {
  if (context?.previousComments) {
    queryClient.setQueryData(['shipmentComments', variables.shipmentId], context.previousComments);
  }
},
```

模式："先动 UI（`setQueryData`） → 后端返回 → 失败回滚（用 `onMutate` 返回的快照）"。这是 TanStack Query 处理评论、点赞这类低风险操作的经典姿势。

**悲观更新**（`useUpdateShipmentStatus`，同文件第 103 行）：高风险操作要求确认弹窗、显示 loading、操作完成后才更新缓存（通过 `invalidateQueries`）。

### 12.3 缓存失效

```ts
invalidateQueries: [['shipment'], ['shipments'], ['dashboardStats'], ['trackingHistory']],
```

把所有相关 query 标记为陈旧，触发后台重新拉取。**层级数组键**让你可以一次性失效一个前缀下的所有缓存。

### 12.4 直接用法

`pages/CreateShipmentPage.tsx:93-103` 是 inline `useMutation`：

```ts
const createShipmentMutation = useMutation({
  mutationFn: shipmentApi.createShipment,
  onSuccess: (response) => {
    localStorage.removeItem(DRAFT_STORAGE_KEY);
    toast.success('Shipment created successfully!');
    navigate(`/shipments/tracking/${response.tracking_number}`);
  },
  onError: (error: Error) => toast.error(`Failed to create shipment: ${error.message}`),
});
```

UI 侧只关心 `createShipmentMutation.isPending`、`.error`、`.mutate(data)`。

---

## 13. 表单：React Hook Form + Zod

`CreateShipmentPage.tsx:33-58`：

```tsx
const form = useForm<CreateShipmentFormData>({
  resolver: zodResolver(createShipmentSchema),
  mode: 'onChange',
  defaultValues: { senderName: user?.name || '', /* ... */ },
});

const { watch, trigger, getValues, setValue } = form;
const formData = watch();
```

知识点：

- **RHF 用 ref 而非受控**：表单字段直接绑到 input 的 `ref` 上，渲染次数远低于"每个 input 都 useState"。
- **`zodResolver`** 把 Zod schema 接到 RHF 的校验流程。Schema 既是运行时校验，也是 TS 类型来源（`type FormData = z.infer<typeof schema>`）。
- **`watch()`** 拿到全部值（**会触发该组件重渲染**）；性能敏感时改用 `getValues()`（只读快照、不订阅）。
- **`trigger(fields)`**：手动触发分步校验，`CreateShipmentPage.tsx:115` 在"下一步"按钮里只校验当前步的字段。
- **`mode: 'onChange'`** 让校验实时跑（默认 onSubmit）。

---

## 14. 实时通信 Hook 模式

`SystemProvider.tsx:26-47` 的 `WebSocketManager` 是把"长连接生命周期挂到登录态"上的标准写法：

```tsx
const WebSocketManager: React.FC = () => {
  const { token, isAuthenticated } = useAuthStore();

  useEffect(() => {
    if (isAuthenticated && token) {
      const wsService = getWebSocketService();
      wsService.connect().catch(console.error);
      return () => wsService.disconnect();          // 卸载或登出 → 断开
    }
  }, [isAuthenticated, token]);

  return null;
};
```

要点：

- effect 的**清理函数**对应资源的释放（断开 WS）。
- 依赖 `[isAuthenticated, token]`：登录/登出/换 token 都会触发"断旧连新"。
- 该组件不渲染任何 UI，只是把生命周期托管到 React 树。

`useSocketStatus`（§4.1）是订阅侧；`useShipmentSubscription`、`useNotificationSync` 是同模式的具体业务封装。

---

## 15. Toast 通知封装

`hooks/use-toast.ts:9-23` 在 Sonner 之上做了一层语义化适配：

```ts
export function useToast() {
  return {
    toast: ({ title, description, variant }: ToastProps) => {
      if (variant === 'destructive') toast.error(title || 'Error', { description });
      else toast.success(title || 'Success', { description });
    },
  };
}
```

启示：第三方库的 API 风格变化时，**靠一层 Hook 把"调用形态"标准化**，业务代码就不必在升级时大面积改写。

---

## 16. 样式系统：CVA + cn() + Tailwind

### 16.1 CVA（class-variance-authority）

`components/ui/button.tsx:6-35`：

```ts
const buttonVariants = cva(
  "inline-flex items-center justify-center ...",   // 基础类
  {
    variants: {
      variant: {
        default:     "bg-primary text-primary-foreground hover:bg-primary/90",
        destructive: "bg-destructive ...",
        outline:     "border border-input ...",
        ghost:       "hover:bg-accent ...",
        link:        "text-primary underline-offset-4 ...",
        success:     "bg-success ...",
        warning:     "bg-warning ...",
      },
      size: {
        default: "h-10 px-4 py-2",
        sm:      "h-9 rounded-md px-3",
        lg:      "h-11 rounded-md px-8",
        icon:    "h-10 w-10",
      },
    },
    defaultVariants: { variant: "default", size: "default" },
  }
)

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
          VariantProps<typeof buttonVariants> {
  asChild?: boolean
}
```

CVA 把 "Tailwind 类名 + 变体" 的组合声明化，**`VariantProps<typeof buttonVariants>` 自动派生 TS 类型**，避免维护两份 source of truth。

### 16.2 `cn()`

`@/lib/utils` 里的 `cn()` 通常是 `twMerge(clsx(...))` 的组合：

- `clsx` 处理"条件类名"（数组、对象、布尔短路）。
- `tailwind-merge` 解决 Tailwind 冲突类（如 `p-2 p-4` 自动保留后者）。

### 16.3 与 Radix 的 `asChild`

```tsx
const Comp = asChild ? Slot : "button"
return <Comp className={cn(buttonVariants({ variant, size, className }))} ref={ref} {...props} />
```

`asChild={true}` 时，`<Button asChild><Link to="/x">…</Link></Button>` 会把 Button 的样式与行为合并到 `<Link>` 上，避免嵌套 `<button><a></a></button>`（不合法的 HTML）。

---

## 17. TypeScript 与 React 的结合

**Props 接口**：

```tsx
// ProtectedRoute.tsx:5-10
interface ProtectedRouteProps {
  children: React.ReactNode
  requireAdmin?: boolean
  allowedRoles?: string[]
  redirectTo?: string
}
```

**HTML 属性继承**（`button.tsx:37-41`）：

```ts
export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
          VariantProps<typeof buttonVariants> {
  asChild?: boolean
}
```

继承 `React.ButtonHTMLAttributes<HTMLButtonElement>` 让 `<Button onClick disabled type="submit" />` 等原生属性全部自动可用且类型正确。

**Ref 的类型**：

```tsx
React.forwardRef<HTMLButtonElement, ButtonProps>(...)
```

第一个泛型是 ref 指向的元素类型，第二个是 props。

**泛型组件**：见 §8.2 `FlashOnUpdate<T>`。

**类型推导自 Schema**：Zod 的 `z.infer<typeof createShipmentSchema>` 让"运行时校验"和"编译期类型"共享同一份定义。

---

## 18. 测试与 Storybook

### 18.1 单元测试

`frontend/src/test/components/auth/ProtectedRoute.test.tsx` 用 Vitest + Testing Library：

- `render(<ProtectedRoute>...</ProtectedRoute>)` 挂载组件。
- `screen.getByRole(...)` / `screen.getByText(...)` 按可访问性查询节点。
- `userEvent` 模拟交互。

### 18.2 Storybook

`src/components/ui/button.stories.tsx` 等文件用 CSF（Component Story Format）：每个 story 是组件的一种状态，可视化预览 + 自动文档。

---

## 学习路径建议

如果你要从这份代码里学 React，建议按下面顺序读源码：

1. `main.tsx` → `App.tsx`：理解 Provider 嵌套与路由结构。
2. `components/ui/button.tsx`：吃透 forwardRef + CVA + asChild 三件套。
3. `stores/auth-store.ts`：Zustand 全貌。
4. `hooks/useDashboardData.ts` + `hooks/mutations/useShipmentMutations.ts`：TanStack Query 的 Query 与 Mutation 完整范式。
5. `pages/CreateShipmentPage.tsx`：把 useState、useEffect、useForm、useMutation、useNavigate、Zustand 全部串起来的综合案例。
6. `components/error/ErrorBoundary.tsx`：唯一保留 class 写法的场景，理解为什么。
7. `components/providers/SystemProvider.tsx`：理解"无 UI 管理者组件 + Provider 桶"的工程化套路。

读完上述七个文件，本项目用到的 React 知识点基本就清晰了。
