import React, { useState } from 'react';
import { ShoppingCart, Plus, Trash2, Calendar, Package, Save, CheckCircle, Search, User } from 'lucide-react';
import { formatNumber, formatDate } from '../storage';

export default function PurchasesScreen({ data, onAddPurchase, inventory = [], showToast }) {
  const { purchases = [], storeInfo = {} } = data;

  const [showAddModal, setShowAddModal] = useState(false);
  const [supplierName, setSupplierName] = useState('');
  const [items, setItems] = useState([]);
  
  // Row State
  const [itemName, setItemName] = useState('');
  const [itemQty, setItemQty] = useState(1);
  const [itemCost, setItemCost] = useState('');
  const [itemSuggestions, setItemSuggestions] = useState([]);

  const nextPurchaseNo = purchases.length > 0 
    ? Math.max(...purchases.map(p => p.number || 0)) + 1 
    : 501;

  const addItemToPurchase = () => {
    if (!itemName.trim() || Number(itemCost) <= 0) {
      if (showToast) showToast('يرجى إدخال اسم الصنف وتكلفة الشراء بشكل صحيح', 'danger');
      return;
    }

    const q = Number(itemQty) || 1;
    const c = Number(itemCost);

    setItems([...items, {
      name: itemName.trim(),
      qty: q,
      cost: c,
      total: q * c
    }]);

    if (showToast) showToast(`تمت إضافة "${itemName.trim()}" لقائمة المشتريات`, 'success');

    setItemName('');
    setItemQty(1);
    setItemCost('');
    setItemSuggestions([]);
  };

  const handleSavePurchase = () => {
    if (items.length === 0) {
      if (showToast) showToast('يرجى إضافة أصناف لفاتورة الشراء', 'danger');
      return;
    }

    const total = items.reduce((sum, i) => sum + i.total, 0);

    onAddPurchase({
      id: 'pur_' + Date.now(),
      number: nextPurchaseNo,
      supplierName: supplierName.trim() || 'مورد عام',
      date: new Date().toISOString(),
      items: items,
      total: total,
      paid: total,
      status: 'paid'
    });

    setSupplierName('');
    setItems([]);
    setShowAddModal(false);
    if (showToast) showToast(`✅ تم حفظ فاتورة الشراء #${nextPurchaseNo} وتحديث المخزون`, 'success');
  };

  const totalPurchases = purchases.reduce((sum, p) => sum + (p.total || 0), 0);

  return (
    <div className="purchases-screen">
      <div className="section-title">
        <h2>
          <ShoppingCart size={22} color="#15803d" />
          <span>فواتير الشراء والتوريد</span>
        </h2>
        <button className="btn btn-primary btn-sm" onClick={() => setShowAddModal(true)}>
          <Plus size={16} />
          <span>فاتورة شراء جديدة</span>
        </button>
      </div>

      {/* Summary Card */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
          <div>
            <div style={{ fontSize: '0.82rem', color: '#64748b' }}>إجمالي المشتريات والتوريدات</div>
            <div style={{ fontSize: '1.5rem', fontWeight: 900, color: '#15803d' }}>
              {formatNumber(totalPurchases)} {storeInfo?.currency || 'ريال'}
            </div>
          </div>
          <div style={{ fontSize: '0.88rem', color: '#64748b' }}>
            عدد فواتير الشراء: <strong>{purchases.length}</strong>
          </div>
        </div>
      </div>

      {/* Purchases List */}
      <div className="card">
        {purchases.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px 20px', color: '#64748b' }}>
            لا توجد فواتير شراء مسجلة بعد. اضغط "فاتورة شراء جديدة" لتسجيل مشتريات وتوريد البضاعة.
          </div>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>رقم الفاتورة</th>
                  <th>المورد</th>
                  <th>التاريخ</th>
                  <th>الأصناف المشتراة</th>
                  <th>الإجمالي</th>
                  <th>الحالة</th>
                </tr>
              </thead>
              <tbody>
                {purchases.map((p) => (
                  <tr key={p.id}>
                    <td><strong>#{p.number}</strong></td>
                    <td><strong>{p.supplierName}</strong></td>
                    <td style={{ fontSize: '0.82rem', color: '#64748b' }}>{formatDate(p.date)}</td>
                    <td>
                      {p.items ? p.items.map(i => `${i.name} (${i.qty})`).join(' ، ') : '-'}
                    </td>
                    <td><strong style={{ color: '#15803d', fontSize: '0.96rem' }}>{formatNumber(p.total)} {storeInfo?.currency || 'ريال'}</strong></td>
                    <td>
                      <span className="badge badge-success">تم التوريد للمخزون</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Add Purchase Modal */}
      {showAddModal && (
        <div className="modal-overlay" onClick={() => setShowAddModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>فاتورة شراء وتوريد بضاعة للمخزن</h3>
              <button className="btn btn-sm btn-secondary" onClick={() => setShowAddModal(false)}>✕</button>
            </div>

            <div className="form-group">
              <label>اسم المورد / شركة التوريد</label>
              <input
                className="form-control"
                placeholder="مثال: شركة البركة للتجارة، مورد الألبان..."
                value={supplierName}
                onChange={(e) => setSupplierName(e.target.value)}
                autoFocus
              />
            </div>

            <div style={{ background: '#f8fafc', padding: 14, borderRadius: 10, marginBottom: 14, border: '1px solid #e2e8f0' }}>
              <div style={{ fontWeight: 800, fontSize: '0.92rem', marginBottom: 10, color: '#15803d' }}>
                إضافة أصناف مشتراة للمخزون
              </div>

              <div className="form-row" style={{ alignItems: 'flex-end' }}>
                <div className="form-group" style={{ flex: '2 1 180px', position: 'relative', marginBottom: 0 }}>
                  <label>اسم الصنف</label>
                  <input
                    className="form-control"
                    placeholder="مثال: أرز، سكر، زيت..."
                    value={itemName}
                    onChange={(e) => {
                      const val = e.target.value;
                      setItemName(val);
                      if (val.trim()) {
                        setItemSuggestions(
                          inventory.filter(i => i.name.toLowerCase().includes(val.toLowerCase())).slice(0, 5)
                        );
                      } else {
                        setItemSuggestions([]);
                      }
                    }}
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
                      maxHeight: 180,
                      overflowY: 'auto'
                    }}>
                      {itemSuggestions.map(p => (
                        <div
                          key={p.id}
                          style={{ padding: '8px 12px', borderBottom: '1px solid #f1f5f9', cursor: 'pointer', display: 'flex', justifyContent: 'space-between' }}
                          onClick={() => {
                            setItemName(p.name);
                            setItemCost(p.purchasePrice || '');
                            setItemSuggestions([]);
                          }}
                        >
                          <strong>{p.name}</strong>
                          <span style={{ fontSize: '0.78rem', color: '#64748b' }}>شراء سابق: {formatNumber(p.purchasePrice)} ريال</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                <div className="form-group" style={{ flex: '0 0 75px', marginBottom: 0 }}>
                  <label>الكمية</label>
                  <input
                    type="number"
                    min="1"
                    className="form-control"
                    value={itemQty}
                    onChange={(e) => setItemQty(Math.max(1, Number(e.target.value)))}
                  />
                </div>

                <div className="form-group" style={{ flex: '1 1 110px', marginBottom: 0 }}>
                  <label>سعر التكلفة</label>
                  <input
                    type="number"
                    min="1"
                    className="form-control"
                    placeholder="التكلفة"
                    value={itemCost}
                    onChange={(e) => setItemCost(e.target.value)}
                  />
                </div>

                <div className="form-group" style={{ flex: '0 0 auto', marginBottom: 0 }}>
                  <button className="btn btn-primary" onClick={addItemToPurchase}>
                    <Plus size={16} />
                    <span>إضافة</span>
                  </button>
                </div>
              </div>
            </div>

            {/* Current Items in Purchase */}
            {items.length > 0 && (
              <div className="table-container" style={{ marginBottom: 14 }}>
                <table>
                  <thead>
                    <tr>
                      <th>الصنف</th>
                      <th>الكمية</th>
                      <th>سعر الشراء</th>
                      <th>الإجمالي</th>
                      <th>حذف</th>
                    </tr>
                  </thead>
                  <tbody>
                    {items.map((it, idx) => (
                      <tr key={idx}>
                        <td><strong>{it.name}</strong></td>
                        <td>{it.qty}</td>
                        <td>{formatNumber(it.cost)} {storeInfo?.currency || 'ريال'}</td>
                        <td><strong style={{ color: '#15803d' }}>{formatNumber(it.total)} {storeInfo?.currency || 'ريال'}</strong></td>
                        <td>
                          <button className="btn btn-sm btn-danger btn-icon" onClick={() => setItems(items.filter((_, i) => i !== idx))}>
                            <Trash2 size={13} />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div style={{ background: '#fef3c7', border: '1px solid #fde68a', padding: 14, borderRadius: 10, marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontWeight: 700 }}>إجمالي فاتورة الشراء:</span>
              <strong style={{ color: '#15803d', fontSize: '1.3rem' }}>
                {formatNumber(items.reduce((s, i) => s + i.total, 0))} {storeInfo?.currency || 'ريال'}
              </strong>
            </div>

            <div style={{ display: 'flex', gap: 10 }}>
              <button className="btn btn-primary" style={{ flex: 1 }} onClick={handleSavePurchase}>
                حفظ الفاتورة وتوريد المخزون
              </button>
              <button className="btn btn-secondary" onClick={() => setShowAddModal(false)}>
                إلغاء
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
