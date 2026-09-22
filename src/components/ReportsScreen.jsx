import React from 'react';
import { BarChart3, TrendingUp, DollarSign, ShoppingCart, Users, Package, Printer, FileText } from 'lucide-react';
import { formatNumber } from '../storage';

export default function ReportsScreen({ data, storeInfo, showToast }) {
  const { invoices = [], purchases = [], customers = [], inventory = [] } = data;

  const totalSales = invoices.reduce((sum, inv) => sum + (inv.total || 0), 0);
  const totalPurchases = purchases.reduce((sum, p) => sum + (p.total || 0), 0);
  // Profit is based on the cost of items actually sold, not all purchases made in the period.
  const costOfGoodsSold = invoices.reduce(
    (sum, inv) => sum + (inv.items || []).reduce(
      (itemSum, item) => itemSum + ((Number(item.cost) || 0) * (Number(item.qty) || 0)),
      0
    ),
    0
  );
  const grossProfit = totalSales - costOfGoodsSold;
  const totalDebts = customers.reduce((sum, c) => sum + (c.balance > 0 ? c.balance : 0), 0);
  const totalPaidCash = invoices.reduce((sum, inv) => sum + (inv.paid || 0), 0);
  const totalCreditRemaining = invoices.reduce((sum, inv) => sum + (inv.remaining || 0), 0);

  // Top Selling Items
  const itemCounts = {};
  invoices.forEach(inv => {
    if (inv.items) {
      inv.items.forEach(it => {
        itemCounts[it.name] = (itemCounts[it.name] || 0) + (it.qty || 1);
      });
    }
  });

  const sortedItems = Object.entries(itemCounts)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 6);

  // Top Debtors
  const sortedDebtors = customers
    .filter(c => c.balance > 0)
    .sort((a, b) => b.balance - a.balance)
    .slice(0, 6);

  return (
    <div className="reports-screen">
      <div className="section-title">
        <h2>
          <BarChart3 size={22} color="#15803d" />
          <span>التقارير المالية وحركة الأرباح</span>
        </h2>
        <button className="btn btn-secondary btn-sm" onClick={() => window.print()}>
          <Printer size={16} />
          <span>طباعة التقرير الشامل</span>
        </button>
      </div>

      {/* Primary Financial KPI Metrics */}
      <div className="form-row" style={{ marginBottom: 18 }}>
        <div className="card" style={{ borderTop: '4px solid #15803d' }}>
          <div style={{ fontSize: '0.82rem', color: '#64748b', fontWeight: 600 }}>إجمالي المبيعات المحققة</div>
          <div style={{ fontSize: '1.6rem', fontWeight: 900, color: '#15803d', margin: '4px 0' }}>
            {formatNumber(totalSales)} <small style={{ fontSize: '0.85rem' }}>{storeInfo?.currency || 'ريال'}</small>
          </div>
          <div style={{ fontSize: '0.76rem', color: '#64748b' }}>من {invoices.length} فاتورة مبيعات</div>
        </div>

        <div className="card" style={{ borderTop: '4px solid #7c3aed' }}>
          <div style={{ fontSize: '0.82rem', color: '#64748b', fontWeight: 600 }}>إجمالي المشتريات والتوريد</div>
          <div style={{ fontSize: '1.6rem', fontWeight: 900, color: '#7c3aed', margin: '4px 0' }}>
            {formatNumber(totalPurchases)} <small style={{ fontSize: '0.85rem' }}>{storeInfo?.currency || 'ريال'}</small>
          </div>
          <div style={{ fontSize: '0.76rem', color: '#64748b' }}>من {purchases.length} فاتورة توريد</div>
        </div>

        <div className="card" style={{ borderTop: '4px solid #d97706' }}>
          <div style={{ fontSize: '0.82rem', color: '#64748b', fontWeight: 600 }}>الربح الإجمالي التقديري (المبيعات - تكلفة البضاعة المباعة)</div>
          <div style={{ fontSize: '1.6rem', fontWeight: 900, color: grossProfit >= 0 ? '#15803d' : '#b91c1c', margin: '4px 0' }}>
            {formatNumber(grossProfit)} <small style={{ fontSize: '0.85rem' }}>{storeInfo?.currency || 'ريال'}</small>
          </div>
          <div style={{ fontSize: '0.76rem', color: '#64748b' }}>المبيعات ناقص تكلفة الأصناف المباعة</div>
        </div>

        <div className="card" style={{ borderTop: '4px solid #b91c1c' }}>
          <div style={{ fontSize: '0.82rem', color: '#64748b', fontWeight: 600 }}>إجمالي الديون المطلوبة (الآجل)</div>
          <div style={{ fontSize: '1.6rem', fontWeight: 900, color: '#b91c1c', margin: '4px 0' }}>
            {formatNumber(totalDebts)} <small style={{ fontSize: '0.85rem' }}>{storeInfo?.currency || 'ريال'}</small>
          </div>
          <div style={{ fontSize: '0.76rem', color: '#64748b' }}>على {customers.filter(c => c.balance > 0).length} عميل في الدفتر</div>
        </div>
      </div>

      {/* Cash Flow Distribution */}
      <div className="card" style={{ marginBottom: 18 }}>
        <h3 className="card-title" style={{ color: '#0f172a', marginBottom: 12 }}>
          <DollarSign size={19} color="#15803d" />
          <span>تحليل التدفق النقدي من المبيعات</span>
        </h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 14 }}>
          <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', padding: 14, borderRadius: 10 }}>
            <span style={{ fontSize: '0.8rem', color: '#166534', fontWeight: 600 }}>السيولة النقدية المحصلة (كاش):</span>
            <div style={{ fontSize: '1.35rem', fontWeight: 900, color: '#15803d', marginTop: 4 }}>
              {formatNumber(totalPaidCash)} {storeInfo?.currency || 'ريال'}
            </div>
          </div>
          <div style={{ background: '#fef2f2', border: '1px solid #fecaca', padding: 14, borderRadius: 10 }}>
            <span style={{ fontSize: '0.8rem', color: '#991b1b', fontWeight: 600 }}>المبيعات الآجلة (المتبقية):</span>
            <div style={{ fontSize: '1.35rem', fontWeight: 900, color: '#b91c1c', marginTop: 4 }}>
              {formatNumber(totalCreditRemaining)} {storeInfo?.currency || 'ريال'}
            </div>
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 16 }}>
        {/* Top Debtors Table */}
        <div className="card">
          <h3 style={{ fontSize: '1.05rem', fontWeight: 800, color: '#b91c1c', marginBottom: 14, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Users size={19} />
            <span>أعلى العملاء مديونية</span>
          </h3>

          {sortedDebtors.length === 0 ? (
            <div style={{ color: '#15803d', textAlign: 'center', padding: 24, fontWeight: 700 }}>
              🎉 لا توجد ديون مستحقة على العملاء حالياً
            </div>
          ) : (
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>اسم العميل</th>
                    <th>الهاتف</th>
                    <th>الرصيد المستحق</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedDebtors.map((c) => (
                    <tr key={c.id}>
                      <td><strong>{c.name}</strong></td>
                      <td>{c.phone || '-'}</td>
                      <td><strong style={{ color: '#b91c1c', fontSize: '0.96rem' }}>{formatNumber(c.balance)} {storeInfo?.currency || 'ريال'}</strong></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Top Selling Products */}
        <div className="card">
          <h3 style={{ fontSize: '1.05rem', fontWeight: 800, color: '#15803d', marginBottom: 14, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Package size={19} />
            <span>الأصناف الأكثر طلباً ومبيعاً</span>
          </h3>

          {sortedItems.length === 0 ? (
            <div style={{ color: '#64748b', textAlign: 'center', padding: 24 }}>
              لا توجد بيانات مبيعات كافية
            </div>
          ) : (
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>اسم الصنف</th>
                    <th>إجمالي الكمية المباعة</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedItems.map(([name, qty], idx) => (
                    <tr key={idx}>
                      <td><strong>{name}</strong></td>
                      <td><strong style={{ color: '#15803d', fontSize: '0.96rem' }}>{qty} وحدة</strong></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
