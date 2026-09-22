import React, { useState, useEffect } from 'react';
import { 
  Plus, 
  Trash2, 
  Printer, 
  Share2, 
  Save, 
  RotateCcw, 
  FileText, 
  CheckCircle,
  UserCheck,
  ShoppingBag,
  CreditCard,
  DollarSign,
  MessageCircle,
  Check,
  Search,
  Minus,
  AlertCircle
} from 'lucide-react';
import { formatNumber, getInvoiceDraft, saveInvoiceDraft, clearInvoiceDraft } from '../storage';
import { generateInvoiceShareText, shareViaWhatsApp, shareContentNative } from '../utils/sharing';

export default function SalesInvoiceScreen({ data, onSaveInvoice, onOpenReceipt, showToast }) {
  const { invoices = [], customers = [], inventory = [], storeInfo = {} } = data;

  // Invoice Number
  const nextInvoiceNumber = invoices.length > 0 
    ? Math.max(...invoices.map(i => i.number || 0)) + 1 
    : 1001;

  const [invoiceNumber, setInvoiceNumber] = useState(nextInvoiceNumber);
  const [customerName, setCustomerName] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const [selectedCustomerId, setSelectedCustomerId] = useState('');
  const [items, setItems] = useState([]);
  const [paid, setPaid] = useState('');
  const [paymentType, setPaymentType] = useState('cash'); // 'cash' | 'credit'
  const [notes, setNotes] = useState('');

  // Row Entry State
  const [itemName, setItemName] = useState('');
  const [itemQty, setItemQty] = useState(1);
  const [itemPrice, setItemPrice] = useState('');
  const [itemTotal, setItemTotal] = useState('');
  const [selectedInventoryItem, setSelectedInventoryItem] = useState(null);

  // Suggestions
  const [customerSuggestions, setCustomerSuggestions] = useState([]);
  const [itemSuggestions, setItemSuggestions] = useState([]);

  // Calculate customer balance
  const existingCustomer = customers.find(c => 
    (selectedCustomerId && c.id === selectedCustomerId) || 
    (c.name.trim().toLowerCase() === customerName.trim().toLowerCase())
  );
  const currentCustomerBalance = existingCustomer ? existingCustomer.balance : 0;

  // Invoice Totals
  const totalAmount = items.reduce((sum, item) => sum + (item.total || 0), 0);
  const paidAmount = paid === '' ? 0 : Number(paid);
  const remainingAmount = Math.max(0, totalAmount - paidAmount);
  const projectedCustomerBalance = currentCustomerBalance + totalAmount - paidAmount;

  // Auto-sync item total when price/qty changes
  useEffect(() => {
    if (itemPrice !== '' && itemQty) {
      setItemTotal(Number(itemPrice) * Number(itemQty));
    }
  }, [itemPrice, itemQty]);

  const handleItemSelect = (product) => {
    setItemName(product.name);
    setItemPrice(product.sellPrice);
    setItemTotal(product.sellPrice * itemQty);
    setSelectedInventoryItem(product);
    setItemSuggestions([]);
  };

  const handleCustomerSelect = (c) => {
    setCustomerName(c.name);
    setCustomerPhone(c.phone || '');
    setSelectedCustomerId(c.id);
    setCustomerSuggestions([]);
  };

  const addItem = () => {
    if (!itemName.trim()) {
      if (showToast) showToast('يرجى إدخال أو اختيار اسم الصنف', 'danger');
      return;
    }
    const q = Number(itemQty) || 1;
    let t = Number(itemTotal);
    let p = Number(itemPrice);

    if (isNaN(t) || t <= 0) {
      if (!isNaN(p) && p > 0) {
        t = p * q;
      } else {
        if (showToast) showToast('يرجى إدخال السعر أو الإجمالي بشكل صحيح', 'danger');
        return;
      }
    } else if (isNaN(p) || p <= 0) {
      p = t / q;
    }

    setItems([...items, {
      name: itemName.trim(),
      qty: q,
      price: Math.round(p),
      total: Math.round(t)
    }]);

    if (showToast) {
      showToast(`تمت إضافة "${itemName.trim()}" للفاتورة`, 'success');
    }

    // Reset input row
    setItemName('');
    setItemQty(1);
    setItemPrice('');
    setItemTotal('');
    setSelectedInventoryItem(null);
  };

  const removeItem = (index) => {
    setItems(items.filter((_, i) => i !== index));
  };

  const handleCashMode = () => {
    setPaymentType('cash');
    setPaid(totalAmount.toString());
  };

  const handleCreditMode = () => {
    setPaymentType('credit');
    setPaid('0');
  };

  // Draft handling
  const handleSaveDraft = () => {
    saveInvoiceDraft({
      invoiceNumber,
      customerName,
      customerPhone,
      selectedCustomerId,
      items,
      paid,
      paymentType,
      notes
    });
    if (showToast) showToast('تم حفظ مسودة الفاتورة مؤقتاً 💾', 'success');
  };

  const handleRestoreDraft = () => {
    const draft = getInvoiceDraft();
    if (!draft) {
      if (showToast) showToast('لا توجد مسودة محفوظة', 'danger');
      return;
    }
    setCustomerName(draft.customerName || '');
    setCustomerPhone(draft.customerPhone || '');
    setSelectedCustomerId(draft.selectedCustomerId || '');
    setItems(draft.items || []);
    setPaid(draft.paid || '');
    setPaymentType(draft.paymentType || 'cash');
    setNotes(draft.notes || '');
    if (showToast) showToast('تم استعادة المسودة بنجاح 🔄', 'success');
  };

  const handleResetForm = () => {
    setItems([]);
    setCustomerName('');
    setCustomerPhone('');
    setSelectedCustomerId('');
    setPaid('');
    setPaymentType('cash');
    setNotes('');
    setInvoiceNumber(invoices.length > 0 ? Math.max(...invoices.map(i => i.number || 0)) + 1 : 1001);
  };

  const createInvoiceObject = () => {
    const cName = customerName.trim() || 'عميل نقدي';
    return {
      id: 'inv_' + Date.now(),
      number: invoiceNumber,
      customerId: existingCustomer ? existingCustomer.id : null,
      customerName: cName,
      customerPhone: customerPhone.trim(),
      date: new Date().toISOString(),
      items: items,
      total: totalAmount,
      paid: paidAmount,
      remaining: remainingAmount,
      paymentType: paymentType,
      notes: notes
    };
  };

  const hasInsufficientStock = () => {
    const required = {};
    items.forEach((item) => {
      const key = item.name.trim().toLowerCase();
      required[key] = (required[key] || 0) + (Number(item.qty) || 0);
    });
    return Object.entries(required).some(([key, qty]) => {
      const product = inventory.find((p) => p.name.trim().toLowerCase() === key);
      return product && Number(product.stock || 0) < qty;
    });
  };

  const handleSave = () => {
    if (items.length === 0) {
      if (showToast) showToast('يرجى إضافة صنف واحد على الأقل للفاتورة', 'danger');
      return;
    }
    if (hasInsufficientStock()) {
      if (showToast) showToast('لا يمكن حفظ الفاتورة: الكمية المطلوبة أكبر من المخزون المتوفر.', 'danger');
      return;
    }

    const invoiceData = createInvoiceObject();
    onSaveInvoice(invoiceData);
    clearInvoiceDraft();
    if (showToast) showToast(`✅ تم حفظ الفاتورة #${invoiceNumber} بنجاح`, 'success');
    handleResetForm();
  };

  const handleSaveAndShareWhatsApp = () => {
    if (items.length === 0) {
      if (showToast) showToast('يرجى إضافة صنف واحد على الأقل للفاتورة', 'danger');
      return;
    }
    if (hasInsufficientStock()) {
      if (showToast) showToast('لا يمكن حفظ الفاتورة: الكمية المطلوبة أكبر من المخزون المتوفر.', 'danger');
      return;
    }

    const invoiceData = createInvoiceObject();
    onSaveInvoice(invoiceData);
    clearInvoiceDraft();
    
    // Share WhatsApp
    const shareText = generateInvoiceShareText(invoiceData, storeInfo);
    shareViaWhatsApp(customerPhone, shareText);
    
    if (showToast) showToast(`✅ تم حفظ الفاتورة وإرسالها للواتساب!`, 'success');
    handleResetForm();
  };

  return (
    <div className="sales-invoice-screen">
      {/* Header & Meta Row */}
      <div className="card" style={{ borderRight: '5px solid #15803d' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14, flexWrap: 'wrap', gap: 8 }}>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 900, color: '#15803d', display: 'flex', alignItems: 'center', gap: 8 }}>
            <FileText size={22} />
            <span>فاتورة مبيعات جديدة</span>
          </h2>
          <div style={{ display: 'flex', gap: 6 }}>
            <button className="btn btn-sm btn-secondary" onClick={handleSaveDraft} title="حفظ مسودة مؤقتة">
              <Save size={14} />
              <span>حفظ مؤقت</span>
            </button>
            <button className="btn btn-sm btn-secondary" onClick={handleRestoreDraft} title="استعادة المسودة">
              <RotateCcw size={14} />
              <span>استعادة</span>
            </button>
            <button className="btn btn-sm btn-secondary" onClick={handleResetForm} title="مسح وبدء فاتورة جديدة">
              <span>فاتورة جديدة</span>
            </button>
          </div>
        </div>

        {/* Invoice Meta Grid */}
        <div className="form-row">
          <div className="form-group" style={{ flex: '0 0 110px' }}>
            <label>رقم الفاتورة</label>
            <input 
              className="form-control" 
              value={`#${invoiceNumber}`} 
              readOnly 
              style={{ fontWeight: 900, color: '#15803d', background: '#f8fafc', textAlign: 'center', fontSize: '1.05rem' }} 
            />
          </div>

          <div className="form-group" style={{ position: 'relative', flex: '2 1 200px' }}>
            <label>اسم العميل</label>
            <input
              className="form-control"
              placeholder="اكتب اسم العميل أو اتركه لعميل نقدي"
              value={customerName}
              onChange={(e) => {
                const val = e.target.value;
                setCustomerName(val);
                if (val.trim()) {
                  setCustomerSuggestions(
                    customers.filter(c => c.name.toLowerCase().includes(val.toLowerCase())).slice(0, 5)
                  );
                } else {
                  setCustomerSuggestions([]);
                }
              }}
            />
            {customerSuggestions.length > 0 && (
              <div style={{
                position: 'absolute',
                top: '100%',
                left: 0,
                right: 0,
                background: 'white',
                border: '1.5px solid #a7f3d0',
                borderRadius: 10,
                zIndex: 40,
                boxShadow: '0 10px 25px rgba(0,0,0,0.15)',
                maxHeight: 200,
                overflowY: 'auto'
              }}>
                {customerSuggestions.map(c => (
                  <div
                    key={c.id}
                    style={{ padding: '10px 14px', borderBottom: '1px solid #f1f5f9', cursor: 'pointer', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
                    onClick={() => handleCustomerSelect(c)}
                  >
                    <div>
                      <strong style={{ color: '#0f172a' }}>{c.name}</strong>
                      <div style={{ fontSize: '0.75rem', color: '#64748b' }}>{c.phone || 'بدون هاتف'}</div>
                    </div>
                    <span style={{ fontSize: '0.82rem', fontWeight: 700, color: c.balance > 0 ? '#b91c1c' : '#15803d' }}>
                      رصيده: {formatNumber(c.balance)} ريال
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="form-group" style={{ flex: '1.5 1 160px' }}>
            <label>رقم هاتف العميل (لإرسال الفاتورة بالواتساب)</label>
            <input
              type="tel"
              className="form-control"
              placeholder="مثال: 771234567"
              value={customerPhone}
              onChange={(e) => setCustomerPhone(e.target.value)}
            />
          </div>
        </div>

        {/* Customer Balance Status Banner */}
        {customerName.trim() && (
          <div style={{
            background: '#f0fdf4',
            border: '1px solid #bbf7d0',
            borderRadius: 10,
            padding: '10px 14px',
            marginTop: 4,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            fontSize: '0.88rem',
            flexWrap: 'wrap',
            gap: 8
          }}>
            <span>الرصيد السابق للعميل: <strong>{formatNumber(currentCustomerBalance)} {storeInfo?.currency || 'ريال'}</strong></span>
            <span>الرصيد المتوقع بعد الفاتورة: <strong style={{ color: projectedCustomerBalance > 0 ? '#b91c1c' : '#15803d', fontSize: '1rem' }}>{formatNumber(projectedCustomerBalance)} {storeInfo?.currency || 'ريال'}</strong></span>
          </div>
        )}
      </div>

      {/* Item Input Box */}
      <div className="card">
        <h3 className="card-title" style={{ color: '#15803d' }}>
          <ShoppingBag size={20} />
          <span>إدخال وبحث الأصناف</span>
        </h3>

        <div className="form-row" style={{ alignItems: 'flex-end' }}>
          <div className="form-group" style={{ flex: '2 1 220px', position: 'relative', marginBottom: 0 }}>
            <label>اسم الصنف / البيان</label>
            <input
              className="form-control"
              placeholder="اكتب اسم الصنف (أرز، سكر، زيت...)"
              value={itemName}
              onChange={(e) => {
                const val = e.target.value;
                setItemName(val);
                if (val.trim()) {
                  setItemSuggestions(
                    inventory.filter(i => i.name.toLowerCase().includes(val.toLowerCase())).slice(0, 6)
                  );
                } else {
                  setItemSuggestions([]);
                }
              }}
              onKeyDown={(e) => e.key === 'Enter' && addItem()}
            />
            {itemSuggestions.length > 0 && (
              <div style={{
                position: 'absolute',
                top: '100%',
                left: 0,
                right: 0,
                background: 'white',
                border: '1.5px solid #a7f3d0',
                borderRadius: 10,
                zIndex: 40,
                boxShadow: '0 10px 25px rgba(0,0,0,0.15)',
                maxHeight: 220,
                overflowY: 'auto'
              }}>
                {itemSuggestions.map(p => (
                  <div
                    key={p.id}
                    style={{ padding: '10px 14px', borderBottom: '1px solid #f1f5f9', cursor: 'pointer', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
                    onClick={() => handleItemSelect(p)}
                  >
                    <div>
                      <strong style={{ color: '#0f172a' }}>{p.name}</strong>
                      <div style={{ fontSize: '0.75rem', color: (p.stock || 0) <= 5 ? '#b91c1c' : '#64748b' }}>
                        المتبقي بالمخزون: <strong>{p.stock}</strong> {p.category ? `• ${p.category}` : ''}
                      </div>
                    </div>
                    <strong style={{ color: '#15803d', fontSize: '0.95rem' }}>{formatNumber(p.sellPrice)} ريال</strong>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="form-group" style={{ flex: '0 0 95px', marginBottom: 0 }}>
            <label>الكمية</label>
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <input
                type="number"
                min="1"
                className="form-control"
                style={{ textAlign: 'center', fontWeight: 700 }}
                value={itemQty}
                onChange={(e) => setItemQty(Math.max(1, Number(e.target.value)))}
              />
            </div>
          </div>

          <div className="form-group" style={{ flex: '1 1 110px', marginBottom: 0 }}>
            <label>سعر الحبة</label>
            <input
              type="number"
              className="form-control"
              placeholder="السعر"
              value={itemPrice}
              onChange={(e) => {
                setItemPrice(e.target.value);
                if (e.target.value) setItemTotal(Number(e.target.value) * itemQty);
              }}
            />
          </div>

          <div className="form-group" style={{ flex: '1 1 120px', marginBottom: 0 }}>
            <label>الإجمالي</label>
            <input
              type="number"
              className="form-control"
              placeholder="الإجمالي"
              value={itemTotal}
              onChange={(e) => {
                setItemTotal(e.target.value);
                if (e.target.value && itemQty) setItemPrice(Number(e.target.value) / itemQty);
              }}
              onKeyDown={(e) => e.key === 'Enter' && addItem()}
            />
          </div>

          <div className="form-group" style={{ flex: '0 0 auto', marginBottom: 0 }}>
            <button className="btn btn-primary" onClick={addItem} style={{ minWidth: 100 }}>
              <Plus size={18} />
              <span>إضافة</span>
            </button>
          </div>
        </div>
      </div>

      {/* Invoice Items Table Card */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <h3 className="card-title" style={{ margin: 0 }}>
            الأصناف المضافة ({items.length})
          </h3>
          {items.length > 0 && (
            <button className="btn btn-sm btn-danger" onClick={() => setItems([])}>
              مسح قائمة الأصناف
            </button>
          )}
        </div>

        {items.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '36px 20px', color: '#64748b', border: '2px dashed #cbd5e1', borderRadius: 12 }}>
            <ShoppingBag size={32} style={{ opacity: 0.4, marginBottom: 8 }} />
            <p style={{ fontWeight: 600 }}>لم يتم إدراج أصناف في الفاتورة بعد</p>
            <p style={{ fontSize: '0.82rem', color: '#94a3b8' }}>أدخل اسم الصنف بالأعلى واضغط على "إضافة"</p>
          </div>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th style={{ width: 45 }}>#</th>
                  <th>اسم الصنف</th>
                  <th>الكمية</th>
                  <th>سعر الوحدة</th>
                  <th>الإجمالي</th>
                  <th style={{ width: 60 }}>حذف</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item, idx) => (
                  <tr key={idx}>
                    <td>{idx + 1}</td>
                    <td><strong>{item.name}</strong></td>
                    <td>{item.qty}</td>
                    <td>{formatNumber(item.price)} {storeInfo?.currency || 'ريال'}</td>
                    <td><strong style={{ color: '#15803d' }}>{formatNumber(item.total)} {storeInfo?.currency || 'ريال'}</strong></td>
                    <td>
                      <button className="btn btn-sm btn-danger btn-icon" onClick={() => removeItem(idx)} title="حذف الصنف">
                        <Trash2 size={15} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Invoice Summary Box */}
        <div style={{ marginTop: 18, borderTop: '2px solid #e2e8f0', paddingTop: 18 }}>
          <div style={{
            background: 'linear-gradient(135deg, #fef3c7 0%, #fde68a 100%)',
            border: '1.5px solid #f59e0b',
            borderRadius: 14,
            padding: '16px 20px',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: 16
          }}>
            <span style={{ fontSize: '1.2rem', fontWeight: 800, color: '#92400e' }}>إجمالي الفاتورة الكلي:</span>
            <span style={{ fontSize: '1.85rem', fontWeight: 900, color: '#15803d' }}>
              {formatNumber(totalAmount)} <small style={{ fontSize: '0.95rem' }}>{storeInfo?.currency || 'ريال'}</small>
            </span>
          </div>

          {/* Payment Method and Paid Input */}
          <div className="form-row" style={{ marginBottom: 14 }}>
            <div className="form-group">
              <label>طريقة ونوع السداد</label>
              <div style={{ display: 'flex', gap: 8 }}>
                <button
                  type="button"
                  className={`btn btn-sm ${paymentType === 'cash' ? 'btn-primary' : 'btn-secondary'}`}
                  style={{ flex: 1, fontWeight: 700 }}
                  onClick={handleCashMode}
                >
                  <DollarSign size={16} />
                  <span>نقدي (سداد كامل)</span>
                </button>
                <button
                  type="button"
                  className={`btn btn-sm ${paymentType === 'credit' ? 'btn-gold' : 'btn-secondary'}`}
                  style={{ flex: 1, fontWeight: 700 }}
                  onClick={handleCreditMode}
                >
                  <CreditCard size={16} />
                  <span>آجل (على الحساب)</span>
                </button>
              </div>
            </div>

            <div className="form-group">
              <label>المبلغ المدفوع حالياً (ريال)</label>
              <input
                type="number"
                min="0"
                className="form-control"
                placeholder="0"
                value={paid}
                onChange={(e) => setPaid(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label>المبلغ المتبقي (آجل)</label>
              <input
                className="form-control"
                value={`${formatNumber(remainingAmount)} ${storeInfo?.currency || 'ريال'}`}
                readOnly
                style={{ 
                  fontWeight: 900, 
                  fontSize: '1.05rem',
                  color: remainingAmount > 0 ? '#b91c1c' : '#15803d', 
                  background: '#f8fafc' 
                }}
              />
            </div>
          </div>

          <div className="form-group">
            <label>ملاحظات إضافية على الفاتورة</label>
            <input
              className="form-control"
              placeholder="مثال: تم التوصيل مع السائق، سند قبض، دفعة أولى..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>

          {/* Action Buttons Grid */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 10, marginTop: 18 }}>
            <button 
              className="btn btn-primary" 
              onClick={handleSave} 
              style={{ fontSize: '1rem', fontWeight: 800, padding: '12px' }}
            >
              <Save size={19} />
              <span>حفظ الفاتورة</span>
            </button>

            <button 
              className="btn btn-whatsapp" 
              onClick={handleSaveAndShareWhatsApp}
              style={{ fontSize: '0.95rem', fontWeight: 800, padding: '12px' }}
            >
              <MessageCircle size={19} />
              <span>حفظ وإرسال واتساب</span>
            </button>

            <button 
              className="btn btn-secondary" 
              onClick={() => {
                if (items.length === 0) {
                  if (showToast) showToast('أضف أصنافاً لمعاينة الطباعة', 'danger');
                  return;
                }
                onOpenReceipt({
                  number: invoiceNumber,
                  customerName: customerName.trim() || 'عميل نقدي',
                  customerPhone: customerPhone,
                  date: new Date().toISOString(),
                  items,
                  total: totalAmount,
                  paid: paidAmount,
                  remaining: remainingAmount,
                  notes
                });
              }}
              style={{ padding: '12px' }}
            >
              <Printer size={18} />
              <span>معاينة وطباعة حرارية</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
