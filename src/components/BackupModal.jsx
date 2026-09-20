import React, { useState } from 'react';
import { Database, Download, Upload, RefreshCw, X, Store, Check } from 'lucide-react';

export default function BackupModal({ data, onRestoreData, onUpdateStoreInfo, onClose, showToast }) {
  const [storeName, setStoreName] = useState(data.storeInfo?.name || 'بقالة العنزي للمواد الغذائية');
  const [storePhone, setStorePhone] = useState(data.storeInfo?.phone || '776425052');
  const [storeSubtitle, setStoreSubtitle] = useState(data.storeInfo?.subtitle || 'مبيعات جملة وتجزئة - مواد غذائية واستهلاكية');
  const [storeCurrency, setStoreCurrency] = useState(data.storeInfo?.currency || 'ريال');

  const handleExportBackup = () => {
    const jsonStr = JSON.stringify(data, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `نسخة_احتياطية_بقالة_العنزي_${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
    if (showToast) showToast('تم تنزيل النسخة الاحتياطية بنجاح 💾', 'success');
  };

  const handleImportBackup = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const parsed = JSON.parse(event.target?.result);
        if (!parsed.invoices || !parsed.customers) {
          if (showToast) showToast('ملف النسخة الاحتياطية غير صالح', 'danger');
          return;
        }
        if (window.confirm('هل تريد بالتأكيد استبدال البيانات الحالية بالنسخة الاحتياطية المحددة؟')) {
          onRestoreData(parsed);
          if (showToast) showToast('✅ تم استرجاع النسخة الاحتياطية بنجاح!', 'success');
          onClose();
        }
      } catch (err) {
        if (showToast) showToast('حدث خطأ أثناء قراءة الملف: ' + err.message, 'danger');
      }
    };
    reader.readAsText(file);
  };

  const handleSaveStoreInfo = () => {
    onUpdateStoreInfo({
      name: storeName.trim() || 'بقالة العنزي',
      phone: storePhone.trim() || '776425052',
      subtitle: storeSubtitle.trim(),
      currency: storeCurrency.trim() || 'ريال'
    });
    if (showToast) showToast('تم حفظ بيانات وإعدادات البقالة بنجاح ✅', 'success');
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <Database size={20} color="#15803d" />
            <span>النسخ الاحتياطي وإعدادات المتجر</span>
          </h3>
          <button className="btn btn-sm btn-secondary" onClick={onClose}>
            <X size={16} />
          </button>
        </div>

        {/* Store Settings */}
        <div style={{ background: '#f8fafc', padding: 14, borderRadius: 10, marginBottom: 16, border: '1px solid #e2e8f0' }}>
          <h4 style={{ fontSize: '0.95rem', fontWeight: 800, marginBottom: 10, color: '#15803d', display: 'flex', alignItems: 'center', gap: 6 }}>
            <Store size={16} />
            <span>بيانات المحل (تظهر في الفواتير والتقارير المطبوعة)</span>
          </h4>

          <div className="form-group">
            <label>اسم المحل / البقالة</label>
            <input
              className="form-control"
              value={storeName}
              onChange={(e) => setStoreName(e.target.value)}
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>رقم هاتف المحل</label>
              <input
                className="form-control"
                value={storePhone}
                onChange={(e) => setStorePhone(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label>رمز العملة</label>
              <input
                className="form-control"
                value={storeCurrency}
                onChange={(e) => setStoreCurrency(e.target.value)}
                placeholder="ريال"
              />
            </div>
          </div>

          <div className="form-group">
            <label>الوصف / النشاط التجاري</label>
            <input
              className="form-control"
              value={storeSubtitle}
              onChange={(e) => setStoreSubtitle(e.target.value)}
            />
          </div>

          <button className="btn btn-sm btn-primary" onClick={handleSaveStoreInfo} style={{ width: '100%', marginTop: 4 }}>
            <Check size={16} />
            <span>حفظ بيانات المتجر</span>
          </button>
        </div>

        {/* Backup Operations */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <button className="btn btn-primary" onClick={handleExportBackup} style={{ padding: '12px 16px' }}>
            <Download size={18} />
            <span>تنزيل نسخة احتياطية كاملة (JSON)</span>
          </button>

          <label className="btn btn-secondary" style={{ padding: '12px 16px', cursor: 'pointer', textAlign: 'center' }}>
            <Upload size={18} />
            <span>استرجاع البيانات من ملف نسخة احتياطية</span>
            <input type="file" accept=".json" onChange={handleImportBackup} style={{ display: 'none' }} />
          </label>
        </div>

        <div style={{ marginTop: 16, fontSize: '0.8rem', color: '#64748b', textAlign: 'center', lineHeight: 1.5 }}>
          جميع البيانات (الفواتير، العملاء، المخزون، الحسابات) محفوظة بأمان محلياً على جهازك. يمكنك أخذ نسخة احتياطية في أي وقت لنقلها لجهاز آخر.
        </div>
      </div>
    </div>
  );
}
