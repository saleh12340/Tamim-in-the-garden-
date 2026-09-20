import React, { useState } from 'react';
import { 
  Users, 
  Search, 
  Plus, 
  ArrowLeft, 
  Phone, 
  Share2, 
  Printer, 
  DollarSign, 
  FileText, 
  Trash2,
  Edit2,
  TrendingDown,
  TrendingUp,
  MessageCircle,
  Copy,
  Check,
  CreditCard,
  History
} from 'lucide-react';
import { formatNumber, formatDate } from '../storage';
import { 
  generateCustomerStatementText, 
  shareViaWhatsApp, 
  shareContentNative, 
  copyToClipboard 
} from '../utils/sharing';

export default function CustomersScreen({ 
  data, 
  selectedCustomer, 
  onSelectCustomer, 
  onAddCustomer, 
  onUpdateCustomer,
  onDeleteCustomer,
  onAddTransaction,
  onDeleteTransaction,
  storeInfo,
  showToast
}) {
  const { customers = [], transactions = [] } = data;

  const [searchQuery, setSearchQuery] = useState('');
  const [showAddCustomerModal, setShowAddCustomerModal] = useState(false);
  const [newCustomerName, setNewCustomerName] = useState('');
  const [newCustomerPhone, setNewCustomerPhone] = useState('');
  const [newCustomerBalance, setNewCustomerBalance] = useState('');
  const [newCustomerNotes, setNewCustomerNotes] = useState('');

  // Transaction Modal State
  const [showTxModal, setShowTxModal] = useState(false);
  const [txType, setTxType] = useState('credit'); // 'credit' = دفع وسداد (ينقص الدين), 'debit' = قيد دين جديد (يزيد الدين)
  const [txAmount, setTxAmount] = useState('');
  const [txDetails, setTxDetails] = useState('');

  // Filtered customer list
  const filteredCustomers = customers.filter(c => 
    (c.name || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
    (c.phone || '').includes(searchQuery)
  );

  const totalDebts = customers.reduce((sum, c) => sum + (c.balance > 0 ? c.balance : 0), 0);
  const totalCreditors = customers.reduce((sum, c) => sum + (c.balance < 0 ? Math.abs(c.balance) : 0), 0);

  // If a customer is selected, show their Account Statement (كشف الحساب)
  if (selectedCustomer) {
    const customerTx = transactions
      .filter(tx => tx.customerId === selectedCustomer.id)
      .sort((a, b) => new Date(b.date) - new Date(a.date));

    const totalDebit = customerTx
      .filter(tx => tx.type === 'debit')
      .reduce((sum, tx) => sum + (tx.amount || 0), 0);

    const totalCredit = customerTx
      .filter(tx => tx.type === 'credit')
      .reduce((sum, tx) => sum + (tx.amount || 0), 0);

    const handleSaveTransaction = () => {
      const amount = Number(txAmount);
      if (isNaN(amount) || amount <= 0) {
        if (showToast) showToast('يرجى إدخال مبلغ صحيح', 'danger');
        return;
      }

      onAddTransaction({
        customerId: selectedCustomer.id,
        customerName: selectedCustomer.name,
        type: txType,
        amount: amount,
        details: txDetails.trim() || (txType === 'credit' ? 'سداد دفعة نقدية' : 'قيد حساب / بضاعة'),
        date: new Date().toISOString()
      });

      if (showToast) {
        showToast(txType === 'credit' ? 'تم تسجيل سند القبض وتخفيض الدين ✅' : 'تم قيد الدين على العميل ✅', 'success');
      }

      setShowTxModal(false);
      setTxAmount('');
      setTxDetails('');
    };

    const statementText = generateCustomerStatementText(selectedCustomer, transactions, storeInfo);

    const handleWhatsAppShare = () => {
      shareViaWhatsApp(selectedCustomer.phone, statementText);
      if (showToast) showToast('جاري فتح واتساب لإرسال كشف الحساب 💬', 'success');
    };

    const handleNativeShare = async () => {
      const res = await shareContentNative({
        title: `كشف حساب - ${selectedCustomer.name}`,
        text: statementText,
        phone: selectedCustomer.phone
      });
      if (res.success && showToast) {
        showToast('تمت مشاركة كشف الحساب بنجاح', 'success');
      }
    };

    const handleCopyStatement = async () => {
      const success = await copyToClipboard(statementText);
      if (success && showToast) {
        showToast('تم نسخ كشف الحساب للحافظة بنجاح 📋', 'success');
      }
    };

    return (
      <div className="customer-statement-screen">
        {/* Header Bar */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16, flexWrap: 'wrap', gap: 10 }}>
          <button className="btn btn-secondary" onClick={() => onSelectCustomer(null)}>
            <ArrowLeft size={18} />
            <span>العودة لدليل العملاء</span>
          </button>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 900, color: '#15803d' }}>
            دفتر وكشف حساب: {selectedCustomer.name}
          </h2>
        </div>

        {/* Customer Overview Card */}
        <div className="card" style={{ borderRight: '5px solid #15803d' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 14 }}>
            <div>
              <h3 style={{ fontSize: '1.35rem', fontWeight: 900, color: '#0f172a' }}>{selectedCustomer.name}</h3>
              <div style={{ fontSize: '0.9rem', color: '#64748b', marginTop: 4, display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                  <Phone size={14} color="#15803d" />
                  <strong>{selectedCustomer.phone || 'بدون رقم هاتف'}</strong>
                </span>
                {selectedCustomer.notes && (
                  <span style={{ background: '#f1f5f9', padding: '2px 8px', borderRadius: 6 }}>
                    {selectedCustomer.notes}
                  </span>
                )}
              </div>
            </div>

            <div style={{ textAlign: 'left', background: selectedCustomer.balance > 0 ? '#fef2f2' : '#f0fdf4', padding: '12px 18px', borderRadius: 12, border: `1.5px solid ${selectedCustomer.balance > 0 ? '#fecaca' : '#bbf7d0'}` }}>
              <div style={{ fontSize: '0.8rem', color: '#64748b', fontWeight: 600 }}>الرصيد المستحق حالياً</div>
              <div style={{ fontSize: '1.8rem', fontWeight: 900, color: selectedCustomer.balance > 0 ? '#b91c1c' : '#15803d' }}>
                {formatNumber(selectedCustomer.balance)} <small style={{ fontSize: '0.9rem' }}>{storeInfo?.currency || 'ريال'}</small>
              </div>
              <div style={{ fontSize: '0.78rem', fontWeight: 700, color: selectedCustomer.balance > 0 ? '#dc2626' : '#16a34a' }}>
                {selectedCustomer.balance > 0 ? '🔴 عليه دين مطلوب سداده' : selectedCustomer.balance < 0 ? '🟢 له رصيد دائن' : '✅ خالص لا يوجد دين'}
              </div>
            </div>
          </div>

          {/* Action Row */}
          <div style={{ display: 'flex', gap: 8, marginTop: 18, borderTop: '1px solid #e2e8f0', paddingTop: 16, flexWrap: 'wrap' }}>
            <button 
              className="btn btn-primary" 
              onClick={() => {
                setTxType('credit');
                setShowTxModal(true);
              }}
            >
              <Plus size={16} />
              <span>تسجيل سداد دفعة نقدية (قبض)</span>
            </button>

            <button 
              className="btn btn-gold" 
              onClick={() => {
                setTxType('debit');
                setShowTxModal(true);
              }}
            >
              <Plus size={16} />
              <span>قيد دين جديد (عليه)</span>
            </button>

            <button className="btn btn-whatsapp" onClick={handleWhatsAppShare}>
              <MessageCircle size={16} />
              <span>مشاركة واتساب</span>
            </button>

            <button className="btn btn-secondary" onClick={handleNativeShare}>
              <Share2 size={16} />
              <span>مشاركة</span>
            </button>

            <button className="btn btn-secondary" onClick={handleCopyStatement}>
              <Copy size={16} />
              <span>نسخ الكشف</span>
            </button>

            <button className="btn btn-secondary" onClick={() => window.print()}>
              <Printer size={16} />
              <span>طباعة كشف الحساب</span>
            </button>
          </div>
        </div>

        {/* Transactions Ledger Table */}
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14, flexWrap: 'wrap', gap: 8 }}>
            <h3 className="card-title" style={{ margin: 0 }}>
              <History size={19} color="#15803d" />
              <span>سجل الحركات والمعاملات ({customerTx.length})</span>
            </h3>
            <div style={{ fontSize: '0.85rem', color: '#64748b' }}>
              إجمالي عليه: <strong style={{ color: '#b91c1c' }}>{formatNumber(totalDebit)}</strong> | إجمالي سدد: <strong style={{ color: '#15803d' }}>{formatNumber(totalCredit)}</strong>
            </div>
          </div>

          {customerTx.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '36px 20px', color: '#64748b' }}>
              لا توجد حركات مسجلة لهذا العميل بعد.
            </div>
          ) : (
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>التاريخ والوقت</th>
                    <th>البيان والتفاصيل</th>
                    <th>مدين (عليه +)</th>
                    <th>دائن (سدد -)</th>
                    <th>الرصيد بعد الحركة</th>
                    <th>حذف</th>
                  </tr>
                </thead>
                <tbody>
                  {customerTx.map((tx) => (
                    <tr key={tx.id}>
                      <td style={{ fontSize: '0.82rem', color: '#64748b' }}>{formatDate(tx.date)}</td>
                      <td><strong>{tx.details}</strong></td>
                      <td style={{ color: '#b91c1c', fontWeight: tx.type === 'debit' ? 800 : 400 }}>
                        {tx.type === 'debit' ? `${formatNumber(tx.amount)} ${storeInfo?.currency || 'ريال'}` : '-'}
                      </td>
                      <td style={{ color: '#15803d', fontWeight: tx.type === 'credit' ? 800 : 400 }}>
                        {tx.type === 'credit' ? `${formatNumber(tx.amount)} ${storeInfo?.currency || 'ريال'}` : '-'}
                      </td>
                      <td><strong>{formatNumber(tx.balanceAfter)} {storeInfo?.currency || 'ريال'}</strong></td>
                      <td>
                        <button 
                          className="btn btn-sm btn-danger btn-icon"
                          onClick={() => {
                            if (window.confirm('هل تريد حذف هذه الحركة وتعديل رصيد العميل؟')) {
                              onDeleteTransaction(tx.id);
                              if (showToast) showToast('تم حذف الحركة وتحديث الرصيد', 'success');
                            }
                          }}
                          title="حذف الحركة"
                        >
                          <Trash2 size={14} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Add Transaction Modal */}
        {showTxModal && (
          <div className="modal-overlay" onClick={() => setShowTxModal(false)}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
              <div className="modal-header">
                <h3>{txType === 'credit' ? 'تسجيل سداد دفعة نقدية (قبض)' : 'قيد مبلغ دين جديد (عليه)'}</h3>
                <button className="btn btn-sm btn-secondary" onClick={() => setShowTxModal(false)}>✕</button>
              </div>

              <div className="form-group">
                <label>نوع الحركة</label>
                <div style={{ display: 'flex', gap: 8 }}>
                  <button
                    type="button"
                    className={`btn btn-sm ${txType === 'credit' ? 'btn-primary' : 'btn-secondary'}`}
                    style={{ flex: 1, fontWeight: 700 }}
                    onClick={() => setTxType('credit')}
                  >
                    سداد نقد (له/سدد - ينقص الدين)
                  </button>
                  <button
                    type="button"
                    className={`btn btn-sm ${txType === 'debit' ? 'btn-gold' : 'btn-secondary'}`}
                    style={{ flex: 1, fontWeight: 700 }}
                    onClick={() => setTxType('debit')}
                  >
                    قيد دين (عليه + يزيد الدين)
                  </button>
                </div>
              </div>

              <div className="form-group">
                <label>المبلغ ({storeInfo?.currency || 'ريال'})</label>
                <input
                  type="number"
                  min="1"
                  className="form-control"
                  placeholder="أدخل المبلغ هنا"
                  value={txAmount}
                  onChange={(e) => setTxAmount(e.target.value)}
                  autoFocus
                />
              </div>

              <div className="form-group">
                <label>تفاصيل / بيان الحركة</label>
                <input
                  className="form-control"
                  placeholder={txType === 'credit' ? 'سداد دفعة نقدية' : 'بضاعة، فاتورة يدوية، كشف...'}
                  value={txDetails}
                  onChange={(e) => setTxDetails(e.target.value)}
                />
              </div>

              <div style={{ display: 'flex', gap: 10, marginTop: 18 }}>
                <button className="btn btn-primary" style={{ flex: 1 }} onClick={handleSaveTransaction}>
                  حفظ الحركة وتحديث الرصيد
                </button>
                <button className="btn btn-secondary" onClick={() => setShowTxModal(false)}>
                  إلغاء
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }

  // Customer List Directory View
  const handleAddNewCustomer = () => {
    if (!newCustomerName.trim()) {
      if (showToast) showToast('يرجى كتابة اسم العميل', 'danger');
      return;
    }

    onAddCustomer({
      name: newCustomerName.trim(),
      phone: newCustomerPhone.trim(),
      balance: Number(newCustomerBalance) || 0,
      notes: newCustomerNotes.trim()
    });

    if (showToast) showToast(`تمت إضافة العميل "${newCustomerName.trim()}" بنجاح`, 'success');

    setNewCustomerName('');
    setNewCustomerPhone('');
    setNewCustomerBalance('');
    setNewCustomerNotes('');
    setShowAddCustomerModal(false);
  };

  return (
    <div className="customers-screen">
      <div className="section-title">
        <h2>
          <Users size={22} color="#15803d" />
          <span>دفتر الحسابات والعملاء</span>
        </h2>
        <button className="btn btn-primary btn-sm" onClick={() => setShowAddCustomerModal(true)}>
          <Plus size={16} />
          <span>إضافة عميل جديد</span>
        </button>
      </div>

      {/* Overview stats & Search */}
      <div className="card">
        <div className="form-row" style={{ alignItems: 'center' }}>
          <div className="form-group" style={{ flex: '2 1 220px', marginBottom: 0 }}>
            <div style={{ position: 'relative' }}>
              <input
                className="form-control"
                placeholder="بحث باسم العميل أو رقم هاتفه..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                style={{ paddingRight: 38 }}
              />
              <Search size={18} style={{ position: 'absolute', right: 12, top: '50%', transform: 'translateY(-50%)', color: '#64748b' }} />
            </div>
          </div>

          <div style={{ flex: '1 1 200px', textAlign: 'left', background: '#fef2f2', padding: '10px 14px', borderRadius: 10, border: '1.5px solid #fecaca' }}>
            <div style={{ fontSize: '0.78rem', color: '#991b1b', fontWeight: 600 }}>إجمالي ديون العملاء (المطلوبة):</div>
            <div style={{ fontSize: '1.25rem', fontWeight: 900, color: '#b91c1c' }}>
              {formatNumber(totalDebts)} {storeInfo?.currency || 'ريال'}
            </div>
          </div>
        </div>
      </div>

      {/* Customer List */}
      <div className="card">
        {filteredCustomers.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px 20px', color: '#64748b' }}>
            لا يوجد عملاء مسجلين بهذا الاسم. اضغط "إضافة عميل جديد" لإضافة عميل.
          </div>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>اسم العميل</th>
                  <th>رقم الهاتف</th>
                  <th>الرصيد المتبقي</th>
                  <th>الحالة</th>
                  <th>إجراءات ومشاركة</th>
                </tr>
              </thead>
              <tbody>
                {filteredCustomers.map((c) => (
                  <tr key={c.id}>
                    <td>
                      <strong 
                        style={{ cursor: 'pointer', color: '#15803d', fontSize: '0.96rem' }} 
                        onClick={() => onSelectCustomer(c)}
                      >
                        {c.name}
                      </strong>
                    </td>
                    <td>{c.phone || 'غير مسجل'}</td>
                    <td>
                      <strong style={{ color: c.balance > 0 ? '#b91c1c' : '#15803d', fontSize: '1rem' }}>
                        {formatNumber(c.balance)} {storeInfo?.currency || 'ريال'}
                      </strong>
                    </td>
                    <td>
                      <span className={`badge ${c.balance > 0 ? 'badge-danger' : c.balance < 0 ? 'badge-info' : 'badge-success'}`}>
                        {c.balance > 0 ? 'عليه دين' : c.balance < 0 ? 'له رصيد' : 'خالص'}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: 6 }}>
                        <button 
                          className="btn btn-sm btn-primary"
                          onClick={() => onSelectCustomer(c)}
                          title="عرض كشف الحساب والعمليات"
                        >
                          <FileText size={14} />
                          <span>كشف الحساب</span>
                        </button>
                        <button 
                          className="btn btn-sm btn-whatsapp btn-icon"
                          onClick={() => {
                            const text = generateCustomerStatementText(c, transactions, storeInfo);
                            shareViaWhatsApp(c.phone, text);
                            if (showToast) showToast('جاري فتح واتساب...', 'success');
                          }}
                          title="مشاركة كشف الحساب عبر واتساب"
                        >
                          <MessageCircle size={14} />
                        </button>
                        <button 
                          className="btn btn-sm btn-danger btn-icon"
                          onClick={() => {
                            if (window.confirm(`هل أنت متأكد من حذف حساب العميل "${c.name}"؟`)) {
                              onDeleteCustomer(c.id);
                              if (showToast) showToast('تم حذف العميل بنجاح', 'success');
                            }
                          }}
                          title="حذف العميل"
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

      {/* Add Customer Modal */}
      {showAddCustomerModal && (
        <div className="modal-overlay" onClick={() => setShowAddCustomerModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>إضافة عميل جديد لدفتر الحسابات</h3>
              <button className="btn btn-sm btn-secondary" onClick={() => setShowAddCustomerModal(false)}>✕</button>
            </div>

            <div className="form-group">
              <label>اسم العميل الكامل</label>
              <input
                className="form-control"
                placeholder="مثال: صالح أحمد العنزي"
                value={newCustomerName}
                onChange={(e) => setNewCustomerName(e.target.value)}
                autoFocus
              />
            </div>

            <div className="form-group">
              <label>رقم الهاتف (لإرسال كشف الحساب والفواتير عبر واتساب)</label>
              <input
                type="tel"
                className="form-control"
                placeholder="مثال: 771234567"
                value={newCustomerPhone}
                onChange={(e) => setNewCustomerPhone(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label>الرصيد الافتتاحي السابق (إن وجد - {storeInfo?.currency || 'ريال'})</label>
              <input
                type="number"
                className="form-control"
                placeholder="0"
                value={newCustomerBalance}
                onChange={(e) => setNewCustomerBalance(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label>ملاحظات إضافية / العنوان</label>
              <input
                className="form-control"
                placeholder="حي الروضة، محل الخضار..."
                value={newCustomerNotes}
                onChange={(e) => setNewCustomerNotes(e.target.value)}
              />
            </div>

            <div style={{ display: 'flex', gap: 10, marginTop: 18 }}>
              <button className="btn btn-primary" style={{ flex: 1 }} onClick={handleAddNewCustomer}>
                حفظ العميل
              </button>
              <button className="btn btn-secondary" onClick={() => setShowAddCustomerModal(false)}>
                إلغاء
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
