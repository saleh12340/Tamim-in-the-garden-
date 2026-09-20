import React, { useState } from 'react';
import { Package, Search, Plus, Trash2, Edit2, AlertTriangle, CheckCircle, ArrowUpDown } from 'lucide-react';
import { formatNumber } from '../storage';

export default function InventoryScreen({ data, onAddProduct, onUpdateProduct, onDeleteProduct, showToast }) {
  const { inventory = [], storeInfo = {} } = data;

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);

  // Form State
  const [name, setName] = useState('');
  const [barcode, setBarcode] = useState('');
  const [purchasePrice, setPurchasePrice] = useState('');
  const [sellPrice, setSellPrice] = useState('');
  const [stock, setStock] = useState('');
  const [minStock, setMinStock] = useState('5');
  const [category, setCategory] = useState('مواد أساسية');

  const categories = ['all', 'مواد أساسية', 'حبوب وأرز', 'زيوت وسمن', 'ألبان وأجبان', 'معلبات', 'مشروبات ومياه', 'منظفات وعناية', 'حلويات وبسكويت', 'أخرى'];

  const filteredInventory = inventory.filter(item => {
    const matchesSearch = 
      (item.name || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
      (item.barcode || '').includes(searchQuery);
    
    if (!matchesSearch) return false;
    if (selectedCategory !== 'all' && item.category !== selectedCategory) return false;
    return true;
  });

  const lowStockCount = inventory.filter(i => (i.stock || 0) <= (i.minStock || 5)).length;

  const handleOpenAdd = () => {
    setName('');
    setBarcode('');
    setPurchasePrice('');
    setSellPrice('');
    setStock('');
    setMinStock('5');
    setCategory('مواد أساسية');
    setEditingProduct(null);
    setShowAddModal(true);
  };

  const handleOpenEdit = (p) => {
    setEditingProduct(p);
    setName(p.name);
    setBarcode(p.barcode || '');
    setPurchasePrice(p.purchasePrice || '');
    setSellPrice(p.sellPrice || '');
    setStock(p.stock || '');
    setMinStock(p.minStock || '5');
    setCategory(p.category || 'مواد أساسية');
    setShowAddModal(true);
  };

  const handleSave = () => {
    if (!name.trim() || Number(sellPrice) <= 0) {
      if (showToast) showToast('يرجى إدخال اسم الصنف وسعر البيع بشكل صحيح', 'danger');
      return;
    }

    const itemData = {
      id: editingProduct ? editingProduct.id : 'item_' + Date.now(),
      name: name.trim(),
      barcode: barcode.trim(),
      purchasePrice: Number(purchasePrice) || 0,
      sellPrice: Number(sellPrice),
      stock: Number(stock) || 0,
      minStock: Number(minStock) || 5,
      category: category
    };

    if (editingProduct) {
      onUpdateProduct(itemData);
      if (showToast) showToast(`تم تعديل الصنف "${name.trim()}" بنجاح`, 'success');
    } else {
      onAddProduct(itemData);
      if (showToast) showToast(`تمت إضافة الصنف "${name.trim()}" للمخزون`, 'success');
    }

    setShowAddModal(false);
  };

  const handleQuickAdjustStock = (item, delta) => {
    const newStock = Math.max(0, (item.stock || 0) + delta);
    onUpdateProduct({ ...item, stock: newStock });
    if (showToast) showToast(`تم تعديل مخزون "${item.name}": ${newStock}`, 'success');
  };

  return (
    <div className="inventory-screen">
      <div className="section-title">
        <h2>
          <Package size={22} color="#15803d" />
          <span>إدارة المخزون والأصناف والأسعار</span>
        </h2>
        <button className="btn btn-primary btn-sm" onClick={handleOpenAdd}>
          <Plus size={16} />
          <span>إضافة صنف جديد</span>
        </button>
      </div>

      {/* Stats & Search Card */}
      <div className="card">
        <div className="form-row" style={{ alignItems: 'center' }}>
          <div className="form-group" style={{ flex: '2 1 200px', marginBottom: 0 }}>
            <div style={{ position: 'relative' }}>
              <input
                className="form-control"
                placeholder="بحث باسم الصنف أو الباركود..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                style={{ paddingRight: 38 }}
              />
              <Search size={18} style={{ position: 'absolute', right: 12, top: '50%', transform: 'translateY(-50%)', color: '#64748b' }} />
            </div>
          </div>

          <div className="form-group" style={{ flex: '1 1 160px', marginBottom: 0 }}>
            <select
              className="form-control"
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
            >
              {categories.map(cat => (
                <option key={cat} value={cat}>
                  {cat === 'all' ? 'جميع التصنيفات' : cat}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div style={{ marginTop: 12, display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', color: '#64748b', borderTop: '1px solid #e2e8f0', paddingTop: 10, flexWrap: 'wrap', gap: 6 }}>
          <span>إجمالي الأصناف: <strong>{inventory.length}</strong></span>
          <span>أصناف قاربت على النفاد: <strong style={{ color: lowStockCount > 0 ? '#b91c1c' : '#15803d' }}>{lowStockCount}</strong></span>
        </div>
      </div>

      {/* Inventory Items Table */}
      <div className="card">
        {filteredInventory.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px 20px', color: '#64748b' }}>
            لا توجد أصناف مطابقة لخيارات البحث.
          </div>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>اسم الصنف</th>
                  <th>التصنيف</th>
                  <th>سعر الشراء</th>
                  <th>سعر البيع</th>
                  <th>الكمية بالمخزن</th>
                  <th>الحالة</th>
                  <th>تعديل سريع للكمية</th>
                  <th>إجراءات</th>
                </tr>
              </thead>
              <tbody>
                {filteredInventory.map((item) => {
                  const isLow = (item.stock || 0) <= (item.minStock || 5);
                  return (
                    <tr key={item.id}>
                      <td><strong>{item.name}</strong></td>
                      <td><span className="badge badge-info">{item.category || 'عام'}</span></td>
                      <td>{formatNumber(item.purchasePrice)} {storeInfo?.currency || 'ريال'}</td>
                      <td><strong style={{ color: '#15803d', fontSize: '0.96rem' }}>{formatNumber(item.sellPrice)} {storeInfo?.currency || 'ريال'}</strong></td>
                      <td>
                        <strong style={{ color: isLow ? '#b91c1c' : '#15803d', fontSize: '1.05rem' }}>
                          {item.stock}
                        </strong>
                      </td>
                      <td>
                        <span className={`badge ${isLow ? 'badge-danger' : 'badge-success'}`}>
                          {isLow ? '⚠️ نقص مخزون' : 'متوفر'}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
                          <button 
                            className="btn btn-sm btn-secondary" 
                            style={{ padding: '2px 8px', minHeight: 28 }}
                            onClick={() => handleQuickAdjustStock(item, -1)}
                          >
                            -1
                          </button>
                          <button 
                            className="btn btn-sm btn-secondary" 
                            style={{ padding: '2px 8px', minHeight: 28 }}
                            onClick={() => handleQuickAdjustStock(item, 1)}
                          >
                            +1
                          </button>
                          <button 
                            className="btn btn-sm btn-secondary" 
                            style={{ padding: '2px 8px', minHeight: 28 }}
                            onClick={() => handleQuickAdjustStock(item, 5)}
                          >
                            +5
                          </button>
                        </div>
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: 4 }}>
                          <button 
                            className="btn btn-sm btn-secondary btn-icon"
                            onClick={() => handleOpenEdit(item)}
                            title="تعديل بيانات الصنف"
                          >
                            <Edit2 size={14} />
                          </button>
                          <button 
                            className="btn btn-sm btn-danger btn-icon"
                            onClick={() => {
                              if (window.confirm(`هل أنت متأكد من حذف الصنف "${item.name}"؟`)) {
                                onDeleteProduct(item.id);
                                if (showToast) showToast(`تم حذف الصنف "${item.name}"`, 'success');
                              }
                            }}
                            title="حذف"
                          >
                            <Trash2 size={14} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Add / Edit Product Modal */}
      {showAddModal && (
        <div className="modal-overlay" onClick={() => setShowAddModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editingProduct ? 'تعديل بيانات الصنف' : 'إضافة صنف جديد للمخزون'}</h3>
              <button className="btn btn-sm btn-secondary" onClick={() => setShowAddModal(false)}>✕</button>
            </div>

            <div className="form-group">
              <label>اسم الصنف</label>
              <input
                className="form-control"
                placeholder="مثال: أرز الشعلان 10 كجم"
                value={name}
                onChange={(e) => setName(e.target.value)}
                autoFocus
              />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label>التصنيف</label>
                <select className="form-control" value={category} onChange={(e) => setCategory(e.target.value)}>
                  {categories.filter(c => c !== 'all').map(c => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>الباركود (اختياري)</label>
                <input
                  className="form-control"
                  placeholder="628..."
                  value={barcode}
                  onChange={(e) => setBarcode(e.target.value)}
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label>سعر التكلفة / الشراء ({storeInfo?.currency || 'ريال'})</label>
                <input
                  type="number"
                  className="form-control"
                  placeholder="0"
                  value={purchasePrice}
                  onChange={(e) => setPurchasePrice(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label>سعر البيع ({storeInfo?.currency || 'ريال'})</label>
                <input
                  type="number"
                  className="form-control"
                  placeholder="0"
                  value={sellPrice}
                  onChange={(e) => setSellPrice(e.target.value)}
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label>الكمية المتاحة حالياً</label>
                <input
                  type="number"
                  className="form-control"
                  placeholder="0"
                  value={stock}
                  onChange={(e) => setStock(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label>حد التنبيه عند نقص المخزون</label>
                <input
                  type="number"
                  className="form-control"
                  placeholder="5"
                  value={minStock}
                  onChange={(e) => setMinStock(e.target.value)}
                />
              </div>
            </div>

            <div style={{ display: 'flex', gap: 10, marginTop: 18 }}>
              <button className="btn btn-primary" style={{ flex: 1 }} onClick={handleSave}>
                {editingProduct ? 'حفظ التعديلات' : 'إضافة الصنف'}
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
