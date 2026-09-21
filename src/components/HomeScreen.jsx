import React from 'react';
import { 
  Receipt, 
  Users, 
  ShoppingCart, 
  Package, 
  BarChart3, 
  StickyNote, 
  TrendingUp, 
  AlertTriangle,
  Printer,
  Share2,
  ArrowRight,
  Plus,
  ArrowUpRight,
  DollarSign,
  Phone,
  Clock,
  ScanLine,
  Camera
} from 'lucide-react';
import { formatNumber, formatDate } from '../storage';
import { shareViaWhatsApp, generateInvoiceShareText } from '../utils/sharing';

export default function HomeScreen({ data, setScreen, onSelectCustomer, onOpenReceipt, showToast }) {
  const { invoices = [], customers = [], inventory = [], purchases = [], storeInfo = {} } = data;

  // Calculations
  const todayStr = new Date().toISOString().slice(0, 10);
  const todayInvoices = invoices.filter(inv => inv.date && inv.date.startsWith(todayStr));
  const todaySales = todayInvoices.reduce((sum, inv) => sum + (inv.total || 0), 0);
  const todayPaid = todayInvoices.reduce((sum, inv) => sum + (inv.paid || 0), 0);
  const todayRemaining = todayInvoices.reduce((sum, inv) => sum + (inv.remaining || 0), 0);

  const totalSales = invoices.reduce((sum, inv) => sum + (inv.total || 0), 0);
  const totalCustomerDebt = customers.reduce((sum, c) => sum + (c.balance > 0 ? c.balance : 0), 0);
  const lowStockItems = inventory.filter(i => (i.stock || 0) <= (i.minStock || 5));

  const recentInvoices = invoices.slice(0, 5);
  const debtCustomers = customers
    .filter(c => c.balance > 0)
    .sort((a, b) => b.balance - a.balance)
    .slice(0, 4);

  return (
    <div className="home-screen">
      {/* Top Welcome / Status Banner */}
      <div style={{
        background: 'linear-gradient(135deg, #064e3b 0%, #047857 100%)',
        color: 'white',
        borderRadius: 16,
        padding: '20px 22px',
        marginBottom: 20,
        boxShadow: '0 8px 20px rgba(6, 78, 59, 0.2)',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: 14
      }}>
        <div>
          <div style={{ fontSize: '0.85rem', color: '#a7f3d0', fontWeight: 600, display: 'flex', alignItems: 'center', gap: 6 }}>
            <Clock size={15} />
            <span>{new Date().toLocaleDateString('ar-EG', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</span>
          </div>
          <h2 style={{ fontSize: '1.45rem', fontWeight: 900, marginTop: 4, color: '#ffffff' }}>
            مرحباً بك في {storeInfo?.name || 'بقالة العنزي'}
          </h2>
          <p style={{ fontSize: '0.85rem', color: '#d1fae5', marginTop: 2 }}>
            نظام إدارة المبيعات، الحسابات الآجلة، والمخزون
          </p>
        </div>

        <div style={{ display: 'flex', gap: 8 }}>
          <button 
            className="btn btn-gold" 
            onClick={() => setScreen('invoice')}
            style={{ fontWeight: 800, padding: '10px 18px' }}
          >
            <Plus size={18} />
            <span>إنشاء فاتورة بيع</span>
          </button>
        </div>
      </div>

      {/* Primary KPI Summary Cards */}
      <div className="form-row" style={{ marginBottom: 20 }}>
        {/* Today Sales */}
        <div className="card" style={{ 
          background: 'linear-gradient(135deg, #15803d 0%, #166534 100%)', 
          color: 'white',
          border: 'none',
          boxShadow: '0 4px 12px rgba(21, 128, 61, 0.25)'
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.82rem', color: '#bbf7d0', fontWeight: 600 }}>مبيعات اليوم ({todayInvoices.length} فواتير)</span>
            <span style={{ background: 'rgba(255,255,255,0.2)', padding: '2px 8px', borderRadius: 20, fontSize: '0.72rem' }}>اليوم</span>
          </div>
          <div style={{ fontSize: '1.65rem', fontWeight: 900, margin: '6px 0' }}>
            {formatNumber(todaySales)} <small style={{ fontSize: '0.85rem' }}>{storeInfo?.currency || 'ريال'}</small>
          </div>
          <div style={{ fontSize: '0.76rem', color: '#dcfce7', display: 'flex', justifyContent: 'space-between' }}>
            <span>نقدي: {formatNumber(todayPaid)}</span>
            <span>آجل: {formatNumber(todayRemaining)}</span>
          </div>
        </div>

        {/* Customer Debts */}
        <div className="card" style={{ 
          background: 'linear-gradient(135deg, #b91c1c 0%, #991b1b 100%)', 
          color: 'white',
          border: 'none',
          boxShadow: '0 4px 12px rgba(185, 28, 28, 0.25)',
          cursor: 'pointer'
        }}
        onClick={() => setScreen('customers')}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.82rem', color: '#fecaca', fontWeight: 600 }}>إجمالي الديون المطلوبة (الآجل)</span>
            <Users size={16} color="#fca5a5" />
          </div>
          <div style={{ fontSize: '1.65rem', fontWeight: 900, margin: '6px 0' }}>
            {formatNumber(totalCustomerDebt)} <small style={{ fontSize: '0.85rem' }}>{storeInfo?.currency || 'ريال'}</small>
          </div>
          <div style={{ fontSize: '0.76rem', color: '#fee2e2' }}>
            على {customers.filter(c => c.balance > 0).length} عميل في الدفتر
          </div>
        </div>

        {/* Inventory Status */}
        <div className="card" style={{ 
          background: 'linear-gradient(135deg, #d97706 0%, #b45309 100%)', 
          color: 'white',
          border: 'none',
          boxShadow: '0 4px 12px rgba(217, 119, 6, 0.25)',
          cursor: 'pointer'
        }}
        onClick={() => setScreen('inventory')}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.82rem', color: '#fde68a', fontWeight: 600 }}>حالة المخزون والأصناف</span>
            <Package size={16} color="#fde68a" />
          </div>
          <div style={{ fontSize: '1.65rem', fontWeight: 900, margin: '6px 0' }}>
            {inventory.length} <small style={{ fontSize: '0.85rem' }}>صنف مسجل</small>
          </div>
          <div style={{ fontSize: '0.76rem', color: '#fef3c7' }}>
            {lowStockItems.length > 0 ? `⚠️ ${lowStockItems.length} صنف قارب على النفاد` : '✅ المخزون بوضع ممتاز'}
          </div>
        </div>
      </div>

      {/* Quick Action Tiles */}
      <div className="section-title">
        <h2>⚡ الوصول السريع والخدمات</h2>
      </div>

      <div className="tiles-grid">
        <div className="tile-btn" onClick={() => setScreen('scanner')} style={{ border: '1.5px solid #86efac', background: '#f0fdf4' }}>
          <div className="tile-icon" style={{ background: '#dcfce7', color: '#15803d' }}>
            <ScanLine size={26} />
          </div>
          <strong style={{ color: '#15803d' }}>ماسح الفواتير ⚡</strong>
          <span>مسح واقتصاص ذكي كـ CamScanner</span>
        </div>

        <div className="tile-btn" onClick={() => setScreen('invoice')}>
          <div className="tile-icon" style={{ background: '#ecfdf5', color: '#15803d' }}>
            <Receipt size={26} />
          </div>
          <strong>فاتورة مبيعات</strong>
          <span>إصدار فواتير نقدية وآجلة</span>
        </div>

        <div className="tile-btn" onClick={() => setScreen('customers')}>
          <div className="tile-icon" style={{ background: '#eff6ff', color: '#2563eb' }}>
            <Users size={26} />
          </div>
          <strong>دفتر العملاء والحسابات</strong>
          <span>متابعة الديون وكشوف الحساب</span>
        </div>

        <div className="tile-btn" onClick={() => setScreen('invoices_history')}>
          <div className="tile-icon" style={{ background: '#fef3c7', color: '#d97706' }}>
            <Receipt size={26} />
          </div>
          <strong>سجل الفواتير</strong>
          <span>عرض، طباعة ومشاركة {invoices.length} فاتورة</span>
        </div>

        <div className="tile-btn" onClick={() => setScreen('purchases')}>
          <div className="tile-icon" style={{ background: '#f5f3ff', color: '#7c3aed' }}>
            <ShoppingCart size={26} />
          </div>
          <strong>فواتير الشراء</strong>
          <span>تسجيل مشتريات وتوريد البضائع</span>
        </div>

        <div className="tile-btn" onClick={() => setScreen('inventory')}>
          <div className="tile-icon" style={{ background: '#f0fdf4', color: '#16a34a' }}>
            <Package size={26} />
          </div>
          <strong>المخزون والأسعار</strong>
          <span>إدارة الكميات وتنبيهات النواقص</span>
        </div>

        <div className="tile-btn" onClick={() => setScreen('reports')}>
          <div className="tile-icon" style={{ background: '#eff6ff', color: '#1d4ed8' }}>
            <BarChart3 size={26} />
          </div>
          <strong>التقارير والأرباح</strong>
          <span>إحصائيات المبيعات والأرباح</span>
        </div>
      </div>

      {/* Low Stock Warning Alert */}
      {lowStockItems.length > 0 && (
        <div className="card" style={{ 
          borderRight: '5px solid #d97706', 
          background: '#fffbeb', 
          borderColor: '#fde68a',
          marginBottom: 20 
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 10 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#92400e', fontWeight: 800 }}>
              <AlertTriangle size={22} color="#d97706" />
              <span>تنبيه: أصناف قاربت على النفاد بالمخزون ({lowStockItems.length})</span>
            </div>
            <button className="btn btn-sm btn-gold" onClick={() => setScreen('inventory')}>
              معاينة المخزون والشراء
            </button>
          </div>
          <div style={{ marginTop: 10, fontSize: '0.88rem', color: '#78350f', display: 'flex', flexWrap: 'wrap', gap: 6 }}>
            {lowStockItems.map(i => (
              <span key={i.id} style={{ background: '#fef3c7', padding: '3px 8px', borderRadius: 6, border: '1px solid #fde68a' }}>
                {i.name} (المتبقي: <strong>{i.stock}</strong>)
              </span>
            ))}
          </div>
        </div>
      )}

      {/* 2 Column Layout for Invoices & Debtors */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 16 }}>
        {/* Recent Invoices Card */}
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
            <h3 className="card-title" style={{ margin: 0 }}>
              <Receipt size={19} color="#15803d" />
              <span>آخر فواتير المبيعات</span>
            </h3>
            <button className="btn btn-sm btn-secondary" onClick={() => setScreen('invoices_history')}>
              <span>عرض الكل</span>
              <ArrowLeftIcon size={14} />
            </button>
          </div>

          {recentInvoices.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '30px 10px', color: '#64748b' }}>
              لا توجد فواتير مسجلة بعد. أنشئ فاتورتك الأولى الآن!
            </div>
          ) : (
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>رقم</th>
                    <th>العميل</th>
                    <th>الإجمالي</th>
                    <th>الحالة</th>
                    <th>إجراء</th>
                  </tr>
                </thead>
                <tbody>
                  {recentInvoices.map((inv) => (
                    <tr key={inv.id}>
                      <td><strong>#{inv.number}</strong></td>
                      <td>{inv.customerName || 'عميل نقدي'}</td>
                      <td><strong style={{ color: '#15803d' }}>{formatNumber(inv.total)}</strong></td>
                      <td>
                        <span className={`badge ${inv.remaining <= 0 ? 'badge-success' : 'badge-warning'}`}>
                          {inv.remaining <= 0 ? 'مسدد' : `آجل (${formatNumber(inv.remaining)})`}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: 4 }}>
                          <button 
                            className="btn btn-sm btn-secondary btn-icon" 
                            onClick={() => onOpenReceipt(inv)}
                            title="معاينة وطباعة حرارية"
                          >
                            <Printer size={14} />
                          </button>
                          <button 
                            className="btn btn-sm btn-whatsapp btn-icon" 
                            onClick={() => {
                              const txt = generateInvoiceShareText(inv, storeInfo);
                              shareViaWhatsApp(inv.customerPhone, txt);
                              if (showToast) showToast('جاري فتح واتساب...', 'success');
                            }}
                            title="مشاركة واتساب"
                          >
                            <Share2 size={13} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Top Debtors Card */}
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
            <h3 className="card-title" style={{ margin: 0 }}>
              <Users size={19} color="#dc2626" />
              <span>أعلى العملاء مديونية</span>
            </h3>
            <button className="btn btn-sm btn-secondary" onClick={() => setScreen('customers')}>
              <span>دفتر الحسابات</span>
              <ArrowLeftIcon size={14} />
            </button>
          </div>

          {debtCustomers.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '30px 10px', color: '#15803d', fontWeight: 600 }}>
              🎉 لا توجد ديون مستحقة على العملاء حالياً!
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {debtCustomers.map(c => (
                <div 
                  key={c.id} 
                  style={{ 
                    padding: '12px 14px', 
                    borderRadius: 10, 
                    border: '1.5px solid #fecaca', 
                    background: '#fef2f2',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease'
                  }}
                  onClick={() => onSelectCustomer(c)}
                >
                  <div>
                    <strong style={{ color: '#0f172a', fontSize: '0.95rem' }}>{c.name}</strong>
                    <div style={{ fontSize: '0.78rem', color: '#64748b', marginTop: 2, display: 'flex', alignItems: 'center', gap: 4 }}>
                      <Phone size={12} />
                      <span>{c.phone || 'بدون هاتف'}</span>
                    </div>
                  </div>

                  <div style={{ textAlign: 'left' }}>
                    <div style={{ color: '#b91c1c', fontWeight: 900, fontSize: '1.1rem' }}>
                      {formatNumber(c.balance)} <small style={{ fontSize: '0.75rem' }}>{storeInfo?.currency || 'ريال'}</small>
                    </div>
                    <span style={{ fontSize: '0.72rem', color: '#dc2626', fontWeight: 700 }}>عرض الكشف 👈</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function ArrowLeftIcon({ size = 16 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="m15 18-6-6 6-6"/>
    </svg>
  );
}
