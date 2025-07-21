import { c as createLucideIcon, r as reactExports, j as jsxRuntimeExports, S as Search, B as Button, a3 as Root2, a4 as Trigger, a5 as Portal2, a6 as Content2, e as cn, a7 as Item2, a8 as Separator2, a9 as SubTrigger2, aa as SubContent2, ab as CheckboxItem2, ac as ItemIndicator2, ad as RadioItem2, ae as Label2 } from "./index-D-bA2lkg.js";
import { I as Input } from "./input-BHuFzm78.js";
import { C as Card, d as CardContent, a as CardHeader } from "./card-CsLEcWf3.js";
import { D as Download } from "./admin-NK0n_rcZ.js";
import { C as ChevronUp, a as ChevronDown } from "./chevron-up-5dIABbZ-.js";
import { C as ChevronRight } from "./chevron-right-B1ZewIMz.js";
import { C as Check } from "./check-DBMoG0qQ.js";
/**
 * @license lucide-react v0.309.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const Circle = createLucideIcon("Circle", [
  ["circle", { cx: "12", cy: "12", r: "10", key: "1mglay" }]
]);
/**
 * @license lucide-react v0.309.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const Filter = createLucideIcon("Filter", [
  ["polygon", { points: "22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3", key: "1yg77f" }]
]);
/**
 * @license lucide-react v0.309.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const MoreHorizontal = createLucideIcon("MoreHorizontal", [
  ["circle", { cx: "12", cy: "12", r: "1", key: "41hilf" }],
  ["circle", { cx: "19", cy: "12", r: "1", key: "1wjl8i" }],
  ["circle", { cx: "5", cy: "12", r: "1", key: "1pcz8c" }]
]);
/**
 * @license lucide-react v0.309.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const Trash2 = createLucideIcon("Trash2", [
  ["path", { d: "M3 6h18", key: "d0wm0j" }],
  ["path", { d: "M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6", key: "4alrt4" }],
  ["path", { d: "M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2", key: "v07s0e" }],
  ["line", { x1: "10", x2: "10", y1: "11", y2: "17", key: "1uufr5" }],
  ["line", { x1: "14", x2: "14", y1: "11", y2: "17", key: "xtxkd" }]
]);
function DataTable({
  data,
  columns,
  loading = false,
  searchable = true,
  searchPlaceholder = "Search...",
  searchFields,
  filterable = false,
  sortable = true,
  selectable = false,
  onSelectionChange,
  onRowClick,
  pagination,
  actions = [],
  exportable = false,
  onExport,
  emptyMessage = "No data available",
  className = ""
}) {
  const [searchTerm, setSearchTerm] = reactExports.useState("");
  const [sortConfig, setSortConfig] = reactExports.useState(null);
  const [selectedRows, setSelectedRows] = reactExports.useState(/* @__PURE__ */ new Set());
  const [filters, setFilters] = reactExports.useState({});
  const handleSort = (columnKey) => {
    if (!sortable) return;
    let direction = "asc";
    if (sortConfig && sortConfig.key === columnKey && sortConfig.direction === "asc") {
      direction = "desc";
    }
    setSortConfig({ key: columnKey, direction });
  };
  const processedData = reactExports.useMemo(() => {
    let filtered = [...data];
    if (searchable && searchTerm) {
      const searchLower = searchTerm.toLowerCase();
      filtered = filtered.filter((row) => {
        if (searchFields && searchFields.length > 0) {
          return searchFields.some(
            (field) => String(row[field]).toLowerCase().includes(searchLower)
          );
        } else {
          return Object.values(row).some(
            (value) => String(value).toLowerCase().includes(searchLower)
          );
        }
      });
    }
    Object.entries(filters).forEach(([key, value]) => {
      if (value) {
        filtered = filtered.filter(
          (row) => String(row[key]).toLowerCase().includes(value.toLowerCase())
        );
      }
    });
    if (sortConfig) {
      filtered.sort((a, b) => {
        const aValue = a[sortConfig.key];
        const bValue = b[sortConfig.key];
        if (aValue < bValue) {
          return sortConfig.direction === "asc" ? -1 : 1;
        }
        if (aValue > bValue) {
          return sortConfig.direction === "asc" ? 1 : -1;
        }
        return 0;
      });
    }
    return filtered;
  }, [data, searchTerm, searchFields, filters, sortConfig, searchable]);
  const handleRowSelect = (index, checked) => {
    const newSelection = new Set(selectedRows);
    if (checked) {
      newSelection.add(index);
    } else {
      newSelection.delete(index);
    }
    setSelectedRows(newSelection);
    if (onSelectionChange) {
      const selectedData = Array.from(newSelection).map((i) => processedData[i]);
      onSelectionChange(selectedData);
    }
  };
  const handleSelectAll = (checked) => {
    if (checked) {
      const allIndices = new Set(processedData.map((_, index) => index));
      setSelectedRows(allIndices);
      onSelectionChange?.(processedData);
    } else {
      setSelectedRows(/* @__PURE__ */ new Set());
      onSelectionChange?.([]);
    }
  };
  const isAllSelected = processedData.length > 0 && selectedRows.size === processedData.length;
  const isIndeterminate = selectedRows.size > 0 && selectedRows.size < processedData.length;
  const getNestedValue = (obj, path) => {
    return path.split(".").reduce((current, key) => current?.[key], obj);
  };
  if (loading) {
    return /* @__PURE__ */ jsxRuntimeExports.jsx(Card, { className, children: /* @__PURE__ */ jsxRuntimeExports.jsx(CardContent, { className: "p-6", children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "space-y-4", children: Array.from({ length: 5 }).map((_, i) => /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "h-12 bg-gray-200 rounded animate-pulse" }, i)) }) }) });
  }
  return /* @__PURE__ */ jsxRuntimeExports.jsxs(Card, { className, children: [
    /* @__PURE__ */ jsxRuntimeExports.jsx(CardHeader, { children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-4", children: [
        searchable && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "relative", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(Search, { className: "absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(
            Input,
            {
              placeholder: searchPlaceholder,
              value: searchTerm,
              onChange: (e) => setSearchTerm(e.target.value),
              className: "pl-10 w-64"
            }
          )
        ] }),
        selectedRows.size > 0 && actions.length > 0 && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { className: "text-sm text-gray-600", children: [
            selectedRows.size,
            " selected"
          ] }),
          actions.map((action, index) => /* @__PURE__ */ jsxRuntimeExports.jsxs(
            Button,
            {
              variant: action.variant || "secondary",
              size: "sm",
              onClick: () => {
                const selectedData = Array.from(selectedRows).map((i) => processedData[i]);
                action.onClick(selectedData);
              },
              disabled: action.disabled?.(Array.from(selectedRows).map((i) => processedData[i])),
              children: [
                action.icon,
                action.label
              ]
            },
            index
          ))
        ] })
      ] }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
        filterable && /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { variant: "outline", size: "sm", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(Filter, { className: "h-4 w-4 mr-2" }),
          "Filters"
        ] }),
        exportable && /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { variant: "outline", size: "sm", onClick: onExport, children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(Download, { className: "h-4 w-4 mr-2" }),
          "Export"
        ] })
      ] })
    ] }) }),
    /* @__PURE__ */ jsxRuntimeExports.jsxs(CardContent, { className: "p-0", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "overflow-x-auto", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("table", { className: "w-full", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("thead", { className: "bg-gray-50 border-b", children: /* @__PURE__ */ jsxRuntimeExports.jsxs("tr", { children: [
          selectable && /* @__PURE__ */ jsxRuntimeExports.jsx("th", { className: "w-12 px-4 py-3 text-left", children: /* @__PURE__ */ jsxRuntimeExports.jsx(
            "input",
            {
              type: "checkbox",
              checked: isAllSelected,
              ref: (el) => {
                if (el) el.indeterminate = isIndeterminate;
              },
              onChange: (e) => handleSelectAll(e.target.checked),
              className: "rounded border-gray-300 text-blue-600 focus:ring-blue-500"
            }
          ) }),
          columns.map((column, index) => /* @__PURE__ */ jsxRuntimeExports.jsx(
            "th",
            {
              className: `px-4 py-3 text-${column.align || "left"} text-sm font-medium text-gray-700 ${column.sortable !== false && sortable ? "cursor-pointer hover:bg-gray-100" : ""}`,
              style: { width: column.width },
              onClick: () => {
                if (column.sortable !== false && sortable) {
                  handleSort(String(column.key));
                }
              },
              children: /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
                column.title,
                column.sortable !== false && sortable && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex flex-col", children: [
                  /* @__PURE__ */ jsxRuntimeExports.jsx(
                    ChevronUp,
                    {
                      className: `h-3 w-3 ${sortConfig?.key === column.key && sortConfig.direction === "asc" ? "text-blue-600" : "text-gray-400"}`
                    }
                  ),
                  /* @__PURE__ */ jsxRuntimeExports.jsx(
                    ChevronDown,
                    {
                      className: `h-3 w-3 -mt-1 ${sortConfig?.key === column.key && sortConfig.direction === "desc" ? "text-blue-600" : "text-gray-400"}`
                    }
                  )
                ] })
              ] })
            },
            index
          ))
        ] }) }),
        /* @__PURE__ */ jsxRuntimeExports.jsx("tbody", { className: "divide-y divide-gray-200", children: processedData.length === 0 ? /* @__PURE__ */ jsxRuntimeExports.jsx("tr", { children: /* @__PURE__ */ jsxRuntimeExports.jsx(
          "td",
          {
            colSpan: columns.length + (selectable ? 1 : 0),
            className: "px-4 py-12 text-center text-gray-500",
            children: emptyMessage
          }
        ) }) : processedData.map((row, rowIndex) => /* @__PURE__ */ jsxRuntimeExports.jsxs(
          "tr",
          {
            className: `hover:bg-gray-50 ${onRowClick ? "cursor-pointer" : ""} ${selectedRows.has(rowIndex) ? "bg-blue-50" : ""}`,
            onClick: () => onRowClick?.(row, rowIndex),
            children: [
              selectable && /* @__PURE__ */ jsxRuntimeExports.jsx("td", { className: "px-4 py-3", children: /* @__PURE__ */ jsxRuntimeExports.jsx(
                "input",
                {
                  type: "checkbox",
                  checked: selectedRows.has(rowIndex),
                  onChange: (e) => handleRowSelect(rowIndex, e.target.checked),
                  onClick: (e) => e.stopPropagation(),
                  className: "rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                }
              ) }),
              columns.map((column, colIndex) => {
                const value = getNestedValue(row, String(column.key));
                return /* @__PURE__ */ jsxRuntimeExports.jsx(
                  "td",
                  {
                    className: `px-4 py-3 text-sm text-${column.align || "left"}`,
                    children: column.render ? column.render(value, row, rowIndex) : String(value || "")
                  },
                  colIndex
                );
              })
            ]
          },
          rowIndex
        )) })
      ] }) }),
      pagination && /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between px-4 py-3 border-t", children: [
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-sm text-gray-600", children: [
          "Showing ",
          (pagination.current - 1) * pagination.pageSize + 1,
          " to",
          " ",
          Math.min(pagination.current * pagination.pageSize, pagination.total),
          " of",
          " ",
          pagination.total,
          " results"
        ] }),
        /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(
            Button,
            {
              variant: "outline",
              size: "sm",
              disabled: pagination.current === 1,
              onClick: () => pagination.onChange(pagination.current - 1, pagination.pageSize),
              children: "Previous"
            }
          ),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { className: "text-sm text-gray-600", children: [
            "Page ",
            pagination.current,
            " of ",
            Math.ceil(pagination.total / pagination.pageSize)
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(
            Button,
            {
              variant: "outline",
              size: "sm",
              disabled: pagination.current >= Math.ceil(pagination.total / pagination.pageSize),
              onClick: () => pagination.onChange(pagination.current + 1, pagination.pageSize),
              children: "Next"
            }
          )
        ] })
      ] })
    ] })
  ] });
}
const DropdownMenu = Root2;
const DropdownMenuTrigger = Trigger;
const DropdownMenuSubTrigger = reactExports.forwardRef(({ className, inset, children, ...props }, ref) => /* @__PURE__ */ jsxRuntimeExports.jsxs(
  SubTrigger2,
  {
    ref,
    className: cn(
      "flex cursor-default select-none items-center rounded-sm px-2 py-1.5 text-sm outline-none focus:bg-accent data-[state=open]:bg-accent",
      inset && "pl-8",
      className
    ),
    ...props,
    children: [
      children,
      /* @__PURE__ */ jsxRuntimeExports.jsx(ChevronRight, { className: "ml-auto h-4 w-4" })
    ]
  }
));
DropdownMenuSubTrigger.displayName = SubTrigger2.displayName;
const DropdownMenuSubContent = reactExports.forwardRef(({ className, ...props }, ref) => /* @__PURE__ */ jsxRuntimeExports.jsx(
  SubContent2,
  {
    ref,
    className: cn(
      "z-50 min-w-[8rem] overflow-hidden rounded-md border bg-popover p-1 text-popover-foreground shadow-lg data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2",
      className
    ),
    ...props
  }
));
DropdownMenuSubContent.displayName = SubContent2.displayName;
const DropdownMenuContent = reactExports.forwardRef(({ className, sideOffset = 4, ...props }, ref) => /* @__PURE__ */ jsxRuntimeExports.jsx(Portal2, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(
  Content2,
  {
    ref,
    sideOffset,
    className: cn(
      "z-50 min-w-[8rem] overflow-hidden rounded-md border bg-white p-1 text-gray-950 shadow-md data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2",
      className
    ),
    ...props
  }
) }));
DropdownMenuContent.displayName = Content2.displayName;
const DropdownMenuItem = reactExports.forwardRef(({ className, inset, ...props }, ref) => /* @__PURE__ */ jsxRuntimeExports.jsx(
  Item2,
  {
    ref,
    className: cn(
      "relative flex cursor-default select-none items-center rounded-sm px-2 py-1.5 text-sm outline-none transition-colors focus:bg-gray-100 focus:text-gray-900 data-[disabled]:pointer-events-none data-[disabled]:opacity-50",
      inset && "pl-8",
      className
    ),
    ...props
  }
));
DropdownMenuItem.displayName = Item2.displayName;
const DropdownMenuCheckboxItem = reactExports.forwardRef(({ className, children, checked, ...props }, ref) => /* @__PURE__ */ jsxRuntimeExports.jsxs(
  CheckboxItem2,
  {
    ref,
    className: cn(
      "relative flex cursor-default select-none items-center rounded-sm py-1.5 pl-8 pr-2 text-sm outline-none transition-colors focus:bg-accent focus:text-accent-foreground data-[disabled]:pointer-events-none data-[disabled]:opacity-50",
      className
    ),
    checked,
    ...props,
    children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "absolute left-2 flex h-3.5 w-3.5 items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(ItemIndicator2, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(Check, { className: "h-4 w-4" }) }) }),
      children
    ]
  }
));
DropdownMenuCheckboxItem.displayName = CheckboxItem2.displayName;
const DropdownMenuRadioItem = reactExports.forwardRef(({ className, children, ...props }, ref) => /* @__PURE__ */ jsxRuntimeExports.jsxs(
  RadioItem2,
  {
    ref,
    className: cn(
      "relative flex cursor-default select-none items-center rounded-sm py-1.5 pl-8 pr-2 text-sm outline-none transition-colors focus:bg-accent focus:text-accent-foreground data-[disabled]:pointer-events-none data-[disabled]:opacity-50",
      className
    ),
    ...props,
    children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "absolute left-2 flex h-3.5 w-3.5 items-center justify-center", children: /* @__PURE__ */ jsxRuntimeExports.jsx(ItemIndicator2, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(Circle, { className: "h-2 w-2 fill-current" }) }) }),
      children
    ]
  }
));
DropdownMenuRadioItem.displayName = RadioItem2.displayName;
const DropdownMenuLabel = reactExports.forwardRef(({ className, inset, ...props }, ref) => /* @__PURE__ */ jsxRuntimeExports.jsx(
  Label2,
  {
    ref,
    className: cn(
      "px-2 py-1.5 text-sm font-semibold",
      inset && "pl-8",
      className
    ),
    ...props
  }
));
DropdownMenuLabel.displayName = Label2.displayName;
const DropdownMenuSeparator = reactExports.forwardRef(({ className, ...props }, ref) => /* @__PURE__ */ jsxRuntimeExports.jsx(
  Separator2,
  {
    ref,
    className: cn("-mx-1 my-1 h-px bg-gray-200", className),
    ...props
  }
));
DropdownMenuSeparator.displayName = Separator2.displayName;
export {
  DataTable as D,
  MoreHorizontal as M,
  Trash2 as T,
  DropdownMenu as a,
  DropdownMenuTrigger as b,
  DropdownMenuContent as c,
  DropdownMenuItem as d,
  DropdownMenuSeparator as e
};
