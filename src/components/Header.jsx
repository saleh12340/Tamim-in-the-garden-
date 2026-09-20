import React from 'react';
import { ShoppingBag, Database, Plus, Sparkles, Store } from 'lucide-react';

export default function Header({ storeInfo, onOpenBackup, onNewInvoice, onGoHome }) {
  return (
    <header className="app-header">
      <div className="header-brand" onClick={onGoHome} style={{ cursor: 'pointer' }}>
        <div className="brand-icon-wrapper">
          <span>🧺</span>
        </div>
        <div className="brand-info">
          <h1>{storeInfo?.name || 'بقالة العنزي للمواد الغذائية'}</h1>
          <p>
            <span>📞 {storeInfo?.phone || '776425052'}</span>
            <span>•</span>
            <span>{storeInfo?.subtitle || 'نظام الفواتير والحسابات والمخزون'}</span>
          </p>
        </div>
      </div>

      <div className="header-actions">
        <button 
          className="icon-btn-header" 
          onClick={onOpenBackup} 
          title="النسخ الاحتياطي وإعدادات المتجر"
        >
          <Database size={17} />
          <span style={{ fontSize: '0.82rem' }}>النسخ / الإعدادات</span>
        </button>

        <button 
          className="btn btn-gold btn-sm" 
          onClick={onNewInvoice}
          style={{ fontWeight: 800, padding: '8px 14px' }}
        >
          <Plus size={17} />
          <span>فاتورة جديدة</span>
        </button>
      </div>
    </header>
  );
}
