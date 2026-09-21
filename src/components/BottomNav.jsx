import React from 'react';
import { 
  Home, 
  Receipt, 
  Users, 
  ShoppingCart, 
  Package, 
  BarChart3, 
  StickyNote,
  History,
  ScanLine
} from 'lucide-react';

export default function BottomNav({ currentScreen, setScreen, data }) {
  const lowStockCount = (data?.inventory || []).filter(i => (i.stock || 0) <= (i.minStock || 5)).length;
  const debtCustomersCount = (data?.customers || []).filter(c => (c.balance || 0) > 0).length;
  const scannedInvoicesCount = (data?.scannedInvoices || []).length;

  const navItems = [
    { id: 'home', label: 'الرئيسية', icon: Home },
    { id: 'scanner', label: 'ماسح الفواتير', icon: ScanLine, badge: scannedInvoicesCount > 0 ? scannedInvoicesCount : null },
    { id: 'invoice', label: 'فاتورة بيع', icon: Receipt },
    { id: 'invoices_history', label: 'سجل الفواتير', icon: History },
    { id: 'customers', label: 'العملاء والديون', icon: Users, badge: debtCustomersCount > 0 ? debtCustomersCount : null },
    { id: 'purchases', label: 'فواتير الشراء', icon: ShoppingCart },
    { id: 'inventory', label: 'المخزون', icon: Package, badge: lowStockCount > 0 ? lowStockCount : null },
    { id: 'reports', label: 'التقارير', icon: BarChart3 },
    { id: 'notes', label: 'الملاحظات', icon: StickyNote },
  ];

  return (
    <nav className="nav-bar">
      {navItems.map((item) => {
        const Icon = item.icon;
        const isActive = currentScreen === item.id;
        return (
          <button
            key={item.id}
            className={`nav-item ${isActive ? 'active' : ''}`}
            onClick={() => setScreen(item.id)}
          >
            <Icon size={18} />
            <span>{item.label}</span>
            {item.badge && (
              <span className="nav-badge" title={`${item.badge}`}>
                {item.badge}
              </span>
            )}
          </button>
        );
      })}
    </nav>
  );
}
