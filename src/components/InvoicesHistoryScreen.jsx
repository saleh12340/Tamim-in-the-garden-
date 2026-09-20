import React, { useState } from 'react';
import { 
  Search, 
  Printer, 
  Trash2, 
  Calendar, 
  FileText, 
  Share2, 
  Eye, 
  MessageCircle,
  Clock,
  DollarSign
} from 'lucide-react';
import { formatNumber, formatDate } from '../storage';
import { generateInvoiceShareText, shareViaWhatsApp, shareContentNative } from '../utils/sharing';

export default function InvoicesHistoryScreen({ data, onDeleteInvoice, onOpenReceipt, showToast }) {
  const { invoices = [], storeInfo = {} } = data;
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedFilter, setSelectedFilter] = useState('all'); // 'all' | 'credit' | 'cash' | 'today'
  const [selectedInvoice, setSelectedInvoice] = useState(null);

  const todayStr = new Date().toISOString().slice(0, 10);

  const filteredInvoices = invoices.filter(inv => {
    const matchesSearch = 
      (inv.customerName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
      String(inv.number).includes(searchTerm) ||
      (inv.customerPhone || '').includes(searchTerm);
    
    if (!matchesSearch) return false;

    if (selectedFilter === 'credit') return (inv.remaining || 0) > 0;
    if (selectedFilter === 'cash') return (inv.remaining || 0) <= 0;
    if (selectedFilter === 'today') return inv.date && inv.date.startsWith(todayStr);
    return true;
  });

  const totalFilteredSales = filteredInvoices.reduce((sum, i) => sum + (i.total || 0), 0);
  const totalFilteredPaid = filteredInvoices.reduce((sum, i) => sum + (i.paid || 0), 0);
  const totalFilteredRemaining = filteredInvoices.reduce((sum, i) => sum + (i.remaining || 0), 0);

  return (
    <div className="invoices-history-screen">
      <div className="section-title">
        <h2>
          <FileText size={22} color="#15803d" />
          <span>سجل فواتير المبيعات</span>
        </h2>
      </div>

      {/* Filter and Search Bar */}
      <div className="card">
        <div className="form-row" style={{ alignItems: 'center' }}>
          <div className="form-group" style={{ flex: '2 1 220px', marginBottom: 0 }}>
            <div style={{ position: 'relative' }}>
              <input
                className="form-control"
                placeholder="بحث برقم الفاتورة، اسم العميل، أو الهاتف..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                style={{ paddingRight: 38 }}
              />
              <Search size={18} style={{ position: 'absolute', right: 12, top: '50%', transform: 'translateY(-50%)', color: '#64748b' }} />
            </div>
          </div>

          <div className="form-group" style={{ flex: '1.2 1 180px', marginBottom: 0 }}>
            <select 
              className="form-control" 
              value={selectedFilter}
              onChange={(e) => setSelectedFilter(e.target.value)}
            >
              <option value="all">جميع الفواتير ({invoices.length})</option>
              <option value="today">فواتير اليوم</option>
              <option value="credit">فواتير آجلة (بها متبقي)</option>
              <option value="cash">فواتير مسددة بالكامل</option>
            </select>
          </div>
        </div>

        <div style={{ 
          marginTop: 14, 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 8,
          fontSize: '0.88rem', 
          color: '#64748b', 
          borderTop: '1px solid #e2e8f0', 
          paddingTop: 10 
        }}>
          <span>عدد الفواتير: <strong>{filteredInvoices.length}</strong></span>
          <span>إجمالي القيمة: <strong style={{ color: '#15803d', fontSize: '1rem' }}>{formatNumber(totalFilteredSales)} {storeInfo?.currency || 'ريال'}</strong></span>
          <span>المتبقي الآجل: <strong style={{ color: '#b91c1c' }}>{formatNumber(totalFilteredRemaining)} {storeInfo?.currency || 'ريال'}</strong></span>
        </div>
      </div>

      {/* Invoices List Table */}
      <div className="card">
        {filteredInvoices.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px 20px', color: '#64748b' }}>
            لا توجد فواتير مطابقة لخيارات البحث
          </div>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>رقم الفاتورة</th>
                  <th>العميل</th>
                  <th>التاريخ والوقت</th>
                  <th>الأصناف</th>
                  <th>الإجمالي</th>
                  <th>المدفوع</th>
                  <th>المتبقي</th>
                  <th>إجراءات ومشاركة</th>
                </tr>
              </thead>
              <tbody>
                {filteredInvoices.map((inv) => (
                  <tr key={inv.id}>
                    <td><strong style={{ color: '#0f172a' }}>#{inv.number}</strong></td>
                    <td>
                      <div>
                        <strong>{inv.customerName || 'عميل نقدي'}</strong>
                        {inv.customerPhone && (
                          <div style={{ fontSize: '0.75rem', color: '#64748b' }}>{inv.customerPhone}</div>
                        )}
                      </div>
                    </td>
                    <td style={{ fontSize: '0.82rem', color: '#64748b' }}>{formatDate(inv.date)}</td>
                    <td>{inv.items ? inv.items.length : 0} صنف</td>
                    <td><strong style={{ color: '#15803d', fontSize: '0.96rem' }}>{formatNumber(inv.total)} {storeInfo?.currency || 'ريال'}</strong></td>
                    <td>{formatNumber(inv.paid)} {storeInfo?.currency || 'ريال'}</td>
                    <td>
                      <span className={`badge ${inv.remaining <= 0 ? 'badge-success' : 'badge-warning'}`}>
                        {inv.remaining <= 0 ? 'مسدد' : `${formatNumber(inv.remaining)} ريال`}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: 4 }}>
                        <button
                          className="btn btn-sm btn-secondary btn-icon"
                          onClick={() => setSelectedInvoice(inv)}
                          title="عرض تفاصيل الفاتورة"
                        >
                          <Eye size={14} />
                        </button>
                        <button
                          className="btn btn-sm btn-primary btn-icon"
                          onClick={() => onOpenReceipt(inv)}
                          title="معاينة وطباعة الإيصال الحراري"
                        >
                          <Printer size={14} />
                        </button>
                        <button
                          className="btn btn-sm btn-whatsapp btn-icon"
                          onClick={() => {
                            const text = generateInvoiceShareText(inv, storeInfo);
                            shareViaWhatsApp(inv.customerPhone, text);
                            if (showToast) showToast('جاري فتح واتساب...', 'success');
                          }}
                          title="إرسال الفاتورة عبر واتساب"
                        >
                          <MessageCircle size={14} />
                        </button>
                        <button
                          className="btn btn-sm btn-danger btn-icon"
                          onClick={() => {
                            if (window.confirm(`هل أنت متأكد من حذف الفاتورة رقم #${inv.number}؟`)) {
                              onDeleteInvoice(inv.id);
                              if (showToast) showToast(`تم حذف الفاتورة #${inv.number}`, 'success');
                            }
                          }}
                          title="حذف الفاتورة"
                        >
                          <Trash2 size={14} />
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

      {/* Invoice Details Modal */}
      {selectedInvoice && (
        <div className="modal-overlay" onClick={() => setSelectedInvoice(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>تفاصيل فاتورة مبيعات #{selectedInvoice.number}</h3>
              <button className="btn btn-sm btn-secondary" onClick={() => setSelectedInvoice(null)}>✕</button>
            </div>

            <div style={{ marginBottom: 14, background: '#f8fafc', padding: 14, borderRadius: 10, border: '1px solid #e2e8f0' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, fontSize: '0.9rem' }}>
                <div><strong>العميل:</strong> {selectedInvoice.customerName}</div>
                <div><strong>الهاتف:</strong> {selectedInvoice.customerPhone || 'غير مسجل'}</div>
                <div><strong>التاريخ:</strong> {formatDate(selectedInvoice.date)}</div>
                <div><strong>نوع السداد:</strong> {selectedInvoice.paymentType === 'cash' ? 'نقدي كامل' : 'آجل على الحساب'}</div>
              </div>
            </div>

            <div className="table-container" style={{ marginBottom: 14 }}>
              <table>
                <thead>
                  <tr>
                    <th>اسم الصنف</th>
                    <th>الكمية</th>
                    <th>سعر الوحدة</th>
                    <th>الإجمالي</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedInvoice.items && selectedInvoice.items.map((item, i) => (
                    <tr key={i}>
                      <td><strong>{item.name}</strong></td>
                      <td>{item.qty}</td>
                      <td>{formatNumber(item.price)} {storeInfo?.currency || 'ريال'}</td>
                      <td><strong style={{ color: '#15803d' }}>{formatNumber(item.total)} {storeInfo?.currency || 'ريال'}</strong></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div style={{ background: '#fef3c7', padding: 14, borderRadius: 10, border: '1px solid #fde68a', marginBottom: 14 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                <span>إجمالي الفاتورة:</span>
                <strong style={{ fontSize: '1.1rem' }}>{formatNumber(selectedInvoice.total)} {storeInfo?.currency || 'ريال'}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                <span>المدفوع:</span>
                <strong>{formatNumber(selectedInvoice.paid)} {storeInfo?.currency || 'ريال'}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid #f59e0b', paddingTop: 6 }}>
                <span>المتبقي:</span>
                <strong style={{ color: selectedInvoice.remaining > 0 ? '#b91c1c' : '#15803d', fontSize: '1.15rem' }}>
                  {formatNumber(selectedInvoice.remaining)} {storeInfo?.currency || 'ريال'}
                </strong>
              </div>
            </div>

            {selectedInvoice.notes && (
              <div style={{ fontSize: '0.85rem', color: '#64748b', marginBottom: 14, background: '#f1f5f9', padding: '8px 12px', borderRadius: 8 }}>
                <strong>ملاحظات:</strong> {selectedInvoice.notes}
              </div>
            )}

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
              <button 
                className="btn btn-primary" 
                onClick={() => {
                  onOpenReceipt(selectedInvoice);
                  setSelectedInvoice(null);
                }}
              >
                <Printer size={18} />
                <span>طباعة حرارية</span>
              </button>

              <button 
                className="btn btn-whatsapp" 
                onClick={() => {
                  const text = generateInvoiceShareText(selectedInvoice, storeInfo);
                  shareViaWhatsApp(selectedInvoice.customerPhone, text);
                  if (showToast) showToast('جاري فتح واتساب...', 'success');
                }}
              >
                <MessageCircle size={18} />
                <span>إرسال واتساب</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
